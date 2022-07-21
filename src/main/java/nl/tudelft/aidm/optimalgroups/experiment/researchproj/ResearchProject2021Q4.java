package nl.tudelft.aidm.optimalgroups.experiment.researchproj;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.FixMatchingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.UndominatedByProfileConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.HardGroupingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ChiarandiniAgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.experiment.dataset.ResearchProject2021Q4Dataset;
import nl.tudelft.aidm.optimalgroups.export.ProjectStudentMatchingCSV;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.dataset.ManualDatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matchfix.MatchFixes;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.FormedGroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.io.File;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("DuplicatedCode")
public class ResearchProject2021Q4
{
	public static void main(String[] args)
	{
		var datasetContext = ResearchProject2021Q4Dataset.getInstance();
		
		var allAgents = datasetContext.allAgents();
//
//			var algo = new Chiarandini_Utilitarian_MinSum_IdentityScheme();
		
		var maxsizeCliques = CliqueGroups.from(allAgents).ofSize(datasetContext.groupSizeConstraint().maxSize());
		
		// Indifferent agents don't care, don't include them in the profile as they consider any project to be equal.
		var groupingAgents = maxsizeCliques.asAgents();
		var indifferentAgents = allAgents.asCollection().stream().filter(agent -> agent.projectPreference().isCompletelyIndifferent()).collect(collectingAndThen(toList(), Agents::from));
		var individualAgents = allAgents.without(groupingAgents).without(indifferentAgents);
		
		// dgb info
		var values = maxsizeCliques.asCollection().stream().map(group -> {
			return group.members().asCollection().stream()
				       .map(agent -> agent.sequenceNumber().toString())
				       .collect(Collectors.joining(", ", "[", "]"));
		}).collect(Collectors.joining("\n"));
		
		
		try {
			var env = new GRBEnv();
			env.start();
			var model = new GRBModel(env);
			
			/*         */
			/* ROUND 1 */
			/*         */
			AssignmentConstraints assignmentConstraints = AssignmentConstraints.createInModel(model, datasetContext);
			
			var objFn = new OWAObjective();
			objFn.apply(model, assignmentConstraints);
			
			// process match-fixes
			var matchFixesSeq = datasetContext.matchesToFix();
			applyManualMatchFixes(matchFixesSeq, model, assignmentConstraints);
			
			model.optimize();
			
			// results round 1
			var matching = new ChiarandiniAgentToProjectMatching(assignmentConstraints.xVars, datasetContext);
			var profileIndividual = profileOfIndividualAgentsInMatching(datasetContext, individualAgents, matching);
			
			/*         */
			/* ROUND 2 */
			/*         */
			var grpConstr = new HardGroupingConstraint(maxsizeCliques);
			grpConstr.apply(model, assignmentConstraints);
			
			var domConstr = new UndominatedByProfileConstraint(profileIndividual, individualAgents);
			domConstr.apply(model, assignmentConstraints);
			
			model.update();
			model.optimize();
			
//			model.write();
			
			// results round 2
			var matching2 = new ChiarandiniAgentToProjectMatching(assignmentConstraints.xVars, datasetContext);
			
			// EXPORT RESULTS
			Assert.that(datasetContext.numMaxSlots() == 1).orThrowMessage("TODO: get mapping slot to agent (projects in dataset have more than 1 slot)");
			var csv = new ProjectStudentMatchingCSV(FormedGroupToProjectMatching.byTriviallyPartitioning(matching2));
			csv.writeToFile("research_project/research_proj " + objFn.name() + " 23_03_21 - w optional");
			
			
			
			var report = new TwoRoundExperimentReport(matching, matching2,
				datasetContext.allAgents(), individualAgents, groupingAgents, indifferentAgents);
			
			report.asHtmlReport()
				.writeHtmlSourceToFile(new File("reports/research_project/research_proj " + objFn.name() + " 23_03_21 - w optional" + ".html"));
			
		}
		catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void applyManualMatchFixes(MatchFixes matchFixes, GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
	{
		for (var matchFix : matchFixes.asList())
		{
			if (matchFix.group().members().count() > 1) {
				new HardGroupingConstraint(Groups.of(matchFix.group()))
					.apply(model, assignmentConstraints);
			}
			
			for (var agent : matchFix.group().members())
			{
				var matchFixConstraint = new FixMatchingConstraint(agent, matchFix.project());
				matchFixConstraint.apply(model, assignmentConstraints);
			}
		}
	}
	
	
	private static void fixMatching(GRBModel model, AssignmentConstraints assignmentConstraints, Agent agent, Project project) throws GRBException
	{
		var constraint = new FixMatchingConstraint(agent, project);
		constraint.apply(model, assignmentConstraints);
	}
	
	private static DatasetContext filteredOfAgents(DatasetContext datasetContext, Agents toExclude)
	{
		// Remove certain students from the matching pool
		var filteredStudents = datasetContext.allAgents().without(toExclude);
		
		var datasetContextFiltered = new ManualDatasetContext(
			datasetContext.identifier() + "_modified", datasetContext.allProjects(), filteredStudents, datasetContext.groupSizeConstraint()
		);
		
		return datasetContextFiltered;
	}
	
	
	private static Profile profileOfIndividualAgentsInMatching(DatasetContext datasetContext, Agents individualAgents, AgentToProjectMatching matching)
	{
		return matching.asList().stream()
			       // Only agents that are 'individual'
			       .filter(match -> individualAgents.contains(match.from()))
			       // A profile is a sorted list of ranks
			       .map(match -> {
				       var rank = match.from().projectPreference().rankOf(match.to());
				       Assert.that(rank.isPresent()).orThrowMessage("Rank not present, handle this case");
				       return rank.asInt();
			       })
			       .sorted()
			       .collect(collectingAndThen(toList(), Profile::fromRanks));
	}

	private static DatasetContext datasetCE10()
	{
		DatasetContext dataContext = CourseEditionFromDb.fromLocalBepSysDbSnapshot(10);
		return dataContext;
	}
	
	private static DatasetContext datasetResearchProj21()
	{
		var dataContext = CourseEditionFromDb.fromLocalBepSysDbSnapshot(39);
		
		return dataContext;
	}

}

