package nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.dataset.DatasetContextTiesBrokenCommonly;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import plouchtch.assertion.Assert;

import java.util.ArrayList;
import java.util.function.Predicate;

public class SDPC
{
	private final Agents agents;
	private final Projects projects;
	private final GroupSizeConstraint groupSizeConstraint;

	public SDPC(Agents agents, Projects projects, GroupSizeConstraint groupSizeConstraint)
	{
		this.agents = agents;
		this.projects = projects;
		this.groupSizeConstraint = groupSizeConstraint;
	}


	public static void main(String[] args)
	{
		var ce = DatasetContextTiesBrokenCommonly.from(CourseEditionFromDb.fromLocalBepSysDbSnapshot(10));
//		var ce = CourseEdition.fromLocalBepSysDbSnapshot(10);

		System.out.println(ce.identifier());

		var thing = new SDPC(ce.allAgents(), ce.allProjects(), ce.groupSizeConstraint());
//		thing.determineK();

		var matching = thing.doIt();

		var metrics = new MatchingMetrics.StudentProject(matching);
		metrics.rankDistribution().displayChart("SDPC - CE10");

		return;
	}

	public AgentToProjectMatching doIt()
	{
		var indexableAgents = new ArrayList<>(agents.asCollection());
		var partialMatching = new SDPCPartialMatching(agents.datasetContext);

		var remainingAgents = agents;

		int n = agents.count();
		for (int t = 1; t <= n; t++)
		{
			var dictatorInThisStep = indexableAgents.get(t-1);
			remainingAgents = remainingAgents.without(dictatorInThisStep);

			var activeProjects = new ActiveProjects(partialMatching, projects, remainingAgents, groupSizeConstraint);
			System.out.printf("Dictator@[t:\t%s]: %s\n", t, dictatorInThisStep);

			var chosenProject = dictatorInThisStep.projectPreference().asList().stream()
				.dropWhile(Predicate.not(activeProjects::contains))
				.findFirst().orElseThrow();

			partialMatching = partialMatching.withNewMatch(dictatorInThisStep, chosenProject);
		}

		// Check all students matched
		Assert.that(partialMatching.asList().size() == n)
			.orThrowMessage("Not all agents were matched");

		// Check if all size constraints met as well
		var groupFact = GroupFactorization.cachedInstanceFor(groupSizeConstraint);
		var matchingGroupedByProject = partialMatching.groupedByProject();
		for (var projectWithMatches : matchingGroupedByProject.entrySet()) {
			var project = projectWithMatches.getKey();
			var matches = projectWithMatches.getValue();
			Assert.that(groupFact.isFactorableIntoValidGroups(matches.size()))
				.orThrowMessage("Students matched to a project cannot be partitioned into groups");
		}

		// Not so 'partial' now though
		return partialMatching;
	}
}
