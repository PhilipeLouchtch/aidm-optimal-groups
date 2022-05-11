package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.EpsilonConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.FixMatchingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.UndominatedByProfileConstraint;
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
import plouchtch.util.Try;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class MILP_Mechanism_FairPregroupingEpsilon
{
	private final DatasetContext datasetContext;
	private final ObjectiveFunction objectiveFunction;
	
	private final Pregrouping pregrouping;
	
	private final FixMatchingConstraint[] matchFixes;
	
	public MILP_Mechanism_FairPregroupingEpsilon(DatasetContext datasetContext, ObjectiveFunction objectiveFunction, PregroupingType pregroupingType, FixMatchingConstraint... matchFixes)
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
		var paretoConstraint = new UndominatedByProfileConstraint(profile, singleStudents);
		
		var x = pregrouping.constraint();
		Assert.that(x instanceof EpsilonConstraint).orThrowMessage("FIXME: the model only supports Epsilon pregrouping constraints");
		var groupingConstraint = (EpsilonConstraint) x;
		
		/* EPSILON SEARCH */
		enum EpsilonOutcome { FEASIBLE, INFEASIBLE }
		record EpsilonResult(EpsilonOutcome outcome, GroupToProjectMatching<Group.FormedGroup> result) {}
		
		var resultPerEpsilon = new EpsilonResult[preFormedGroups.count()+1];
//		var epsilonSearch = new EpsilonSearchBinary(preFormedGroups.count());
		// Often, there are only a few pregroupings not possible, thus linear search has better best-case performance
		var epsilonSearch = new EpsilonSearchLinear(preFormedGroups.count());
		
		Function<Integer, EpsilonResult> epsilonMatching = (Integer epsilon) -> {
			var allConstraints = new ArrayList<>(Arrays.asList(paretoConstraint, groupingConstraint));
			allConstraints.addAll(Arrays.asList(matchFixes));
			
			try {
				var result = new ChiarandiniBaseModel(datasetContext, objectiveFunction, allConstraints.toArray(Constraint[]::new)).doIt();
				return new EpsilonResult(EpsilonOutcome.FEASIBLE, result);
			}
			catch (ChiarandiniBaseModel.InfeasbileMatchingException ex) {
				Assert.that(epsilon > 0).orThrowMessage("Epsilon 0 is infeasible??");
				return new EpsilonResult(EpsilonOutcome.INFEASIBLE, null);
			}
		};
		
		Function<Integer, EpsilonResult> computeIfAbsent = epsilon -> {
			var result = resultPerEpsilon[epsilon];
			
			if (result == null) {
				result = epsilonMatching.apply(epsilon);
				resultPerEpsilon[epsilon] = result;
			}
			
			return result;
		};
		
		var epsilon = epsilonSearch.initial();
		while (true) {
			groupingConstraint.setEpsilon(epsilon);
			var result = computeIfAbsent.apply(epsilon);
			
			var nextEpsilon = epsilonSearch.next(result.outcome == EpsilonOutcome.FEASIBLE);
			
			if (nextEpsilon == epsilon)
				break;
			else
				epsilon = nextEpsilon;
		}
		
		var epsilonResult = computeIfAbsent.apply(epsilon);
		Assert.that(epsilonResult.outcome == EpsilonOutcome.FEASIBLE).orThrowMessage("BUGCHECK: Infeasible matching?");
		
		var matchingResults = new Matching(epsilonResult.result(), baselineMatching);
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
	
	interface EpsilonSearch
	{
		int initial();
		int next(boolean currentIsFeasible);
	}
	
	public static class EpsilonSearchBinary implements EpsilonSearch
	{
		private int lb;
		private int ub;
		
		private int current;
		
		public EpsilonSearchBinary(int maxValue)
		{
			this.lb = 0;
			this.ub = maxValue;
			
			this.current = calc(lb, ub);
		}
		
		public int initial()
		{
			return current;
		}
		
		public int next(boolean currentIsFeasible)
		{
			if (currentIsFeasible) {
				// Epsilon is feasible, can we go higher?
				lb = current+1;
			} else {
				// Epsilon not feasible, have to try smaller
				ub = current-1;
			}
			
			current = calc(lb, ub);
			return current;
		}
		
		private static int calc(int lb, int ub)
		{
			return (ub + lb) >>> 1;
		}
	}
	
	public static class EpsilonSearchLinear implements EpsilonSearch
	{
		private int current;
		
		public EpsilonSearchLinear(int maxValue)
		{
			this.current = maxValue;
		}
		
		@Override
		public int initial()
		{
			return current;
		}
		
		@Override
		public int next(boolean currentIsFeasible)
		{
			if (currentIsFeasible) {
				// We're trying epsilons from max to 0, the very first value
				// that is feasible is the winner
				return current;
			} else {
				return Math.max(--current, 0);
			}
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
