package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.FixMatchingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.UndominatedByProfileConstraintImproved;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.metric.profile.StudentRankProfile;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Uses the "improved" {@link UndominatedByProfileConstraintImproved} fairness constraint,
 * for the rest exactly the same as the initial Fair mechanism implementation
 */
public class MILP_Mechanism_FairPregroupingImpr
{
	private final DatasetContext datasetContext;
	private final ObjectiveFunction objectiveFunction;
	
	private final Pregrouping pregrouping;
	
	private final FixMatchingConstraint[] matchFixes;
	
	public MILP_Mechanism_FairPregroupingImpr(DatasetContext datasetContext, ObjectiveFunction objectiveFunction, PregroupingType pregroupingType, FixMatchingConstraint... matchFixes)
	{
		this.datasetContext = datasetContext;
		this.objectiveFunction = objectiveFunction;
	
		this.pregrouping = pregroupingType.instantiateFor(datasetContext);
	
		this.matchFixes = matchFixes;
	}
	
	public Matching doIt()
	{
		var allAgents = datasetContext.allAgents();
		
		var preFormedGroups = pregrouping.groups();
		
		var pregroupingStudents = preFormedGroups.asAgents();
		var indifferentStudents = allAgents.asCollection().stream().filter(agent -> agent.projectPreference().isCompletelyIndifferent()).collect(Agents.collector);
		var singleStudents = allAgents.without(pregroupingStudents).without(indifferentStudents);

		// Initial matching to determine baseline outcome quality for the "single" students
		var baselineMatching = AgentToProjectMatching.from(
				new ChiarandiniBaseModel(datasetContext, objectiveFunction, matchFixes).doIt()
		);
		
		var profile = Profile.of(baselineMatching, singleStudents);
		var paretoConstraint = new UndominatedByProfileConstraintImproved(profile, singleStudents);
		
		var groupingConstraint = pregrouping.constraint();
		
		var allConstraints = new ArrayList<>(Arrays.asList(paretoConstraint, groupingConstraint));
		allConstraints.addAll(Arrays.asList(matchFixes));
		
		var finalMatching = new ChiarandiniBaseModel(datasetContext, objectiveFunction, allConstraints.toArray(Constraint[]::new))
				.doIt();
		
		var matchingResults = new Matching(finalMatching, baselineMatching);
		
		return matchingResults;
	}
	
	public record Matching(GroupToProjectMatching<Group.FormedGroup> finalMatching, AgentToProjectMatching baselineMatching) implements GroupToProjectMatching<Group.FormedGroup>
	{
		public Matching
		{
			Assert.that(finalMatching.datasetContext() == baselineMatching.datasetContext()).orThrowMessage("dataset mismatch between baseline and final matching");
		}
		
		@Override
		public List<Match<Group.FormedGroup, Project>> asList()
		{
			return finalMatching.asList();
		}
		
		@Override
		public DatasetContext datasetContext()
		{
			return finalMatching.datasetContext();
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		CourseEdition ce = CourseEditionFromDb.fromLocalBepSysDbSnapshot(10);
		
		var owaMinimaxChiarandini = new Chiarandini_MinimaxOWA(ce, PregroupingType.anyCliqueHardGrouped());
		var resultOwa = owaMinimaxChiarandini.doIt();
		var agentToProjectMatching = AgentToProjectMatching.from(resultOwa);
		
		var metricsOwa = new MatchingMetrics.StudentProject(agentToProjectMatching);
		new StudentRankProfile(agentToProjectMatching).displayChart("Chiarandini minimax-owa");
		
		return;
	}
}
