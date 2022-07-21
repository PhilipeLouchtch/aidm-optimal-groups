package nl.tudelft.aidm.optimalgroups.experiment.researchproj;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.UndominatedByProfileConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.HardGroupingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ChiarandiniAgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.export.ProjectStudentMatchingCSV;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.FormedGroupToProjectMatching;
import plouchtch.assertion.Assert;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class Experiment_two_round_groups_undom_individuals
{

	public static void main(String[] args)
	{
		var datasetContext = datasetResearchProj21();
//		var datasetContext = datasetCE10();
		
		var allAgents = datasetContext.allAgents();
//
//			var algo = new Chiarandini_Utilitarian_MinSum_IdentityScheme();
		
		var cliques = CliqueGroups.from(allAgents);
		
		var maxsizeCliques = cliques.asCollection().stream()
			                     .filter(tentativeGroup -> tentativeGroup.members().count() == datasetContext.groupSizeConstraint().maxSize())
			                     .collect(collectingAndThen(toList(), Groups.ListBackedImpl<Group.TentativeGroup>::new));
		
		// Indifferent agents don't care, don't include them in the profile as they consider any project to be equal.
		var groupingAgents = maxsizeCliques.asAgents();
		var indifferentAgents = allAgents.asCollection().stream().filter(agent -> agent.projectPreference().isCompletelyIndifferent()).collect(collectingAndThen(toList(), Agents::from));
		var individualAgents = allAgents.without(groupingAgents).without(indifferentAgents);
		
		
		try {
			var env = new GRBEnv();
			env.start();
			var model = new GRBModel(env);
			
			AssignmentConstraints assignmentConstraints = AssignmentConstraints.createInModel(model, datasetContext);
			
			var objFn = new OWAObjective();
			objFn.apply(model, assignmentConstraints);
			
			model.optimize();
			
			var matching = new ChiarandiniAgentToProjectMatching(assignmentConstraints.xVars, datasetContext);
			var profileIndividual = profileOfIndividualAgentsInMatching(individualAgents, matching);
			
			var grpConstr = new HardGroupingConstraint(maxsizeCliques);
			grpConstr.apply(model, assignmentConstraints);
			
			var domConstr = new UndominatedByProfileConstraint(profileIndividual, individualAgents);
			domConstr.apply(model, assignmentConstraints);
			
			model.update();
			model.optimize();
			
			var matching2 = new ChiarandiniAgentToProjectMatching(assignmentConstraints.xVars, datasetContext);
			
			Assert.that(datasetContext.numMaxSlots() == 1).orThrowMessage("TODO: get mapping slot to agent (projects in dataset have more than 1 slot)");
			var csv = new ProjectStudentMatchingCSV(FormedGroupToProjectMatching.byTriviallyPartitioning(matching2));
			csv.writeToFile("research_project " + objFn.name());
			
			
			
			var report = new TwoRoundExperimentReport(matching, matching2,
				datasetContext.allAgents(), individualAgents, groupingAgents, indifferentAgents);
			
//			report.asHtmlReport()
//				.writeHtmlSourceToFile(new File("reports/research_project/research_proj " + objFn.name() + ".html"));
			
		}
		catch (GRBException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private static Profile profileOfIndividualAgentsInMatching(Agents individualAgents, AgentToProjectMatching matching)
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

