package nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.MinQuorumRequirement;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.NumAgentsTillQuorum;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.WorstAmongBestProjectPairings;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedCommonly;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import plouchtch.assertion.Assert;

import java.util.Comparator;
import java.util.Set;

public class SDPCPessimism
{
	private final Agents agents;
	private final Projects projects;
	private final GroupSizeConstraint groupSizeConstraint;

	public SDPCPessimism(Agents agents, Projects projects, GroupSizeConstraint groupSizeConstraint)
	{
		this.agents = agents;
		this.projects = projects;
		this.groupSizeConstraint = groupSizeConstraint;
	}

	public static void main(String[] args)
	{
		var ce = DatasetContext_AugmentedPreferences_AppendedCommonly.from(CourseEditionFromDb.fromLocalBepSysDbSnapshot(10));
//		var ce = CourseEdition.fromLocalBepSysDbSnapshot(10);

		System.out.println(ce.identifier());

		var thing = new SDPCPessimism(ce.allAgents(), ce.allProjects(), ce.groupSizeConstraint());
//		thing.determineK();

		var matching = thing.matching();

		var metrics = new MatchingMetrics.StudentProject(matching);
		metrics.rankDistribution().displayChart("SDPCPessimism");

		return;
	}

	public AgentToProjectMatching matching()
	{
		var remaningAgents = agents;
		var partialMatching = new SDPCPartialMatching(agents.datasetContext);

		int n = agents.count();
		for (int t = 1; t <= n;)
		{
			var activeProjects = new ActiveProjects(partialMatching, projects, agents, groupSizeConstraint);
//			DatasetContext datasetContext = null;

			// TODO: find 'eccentric' student
			var worstBestOffAgents = worstBestOffAgents(remaningAgents, activeProjects, partialMatching, groupSizeConstraint);
			var chosenProject = worstBestOffAgents.chosenProject();

			var agentsAdded = 0;
			for (var agent : worstBestOffAgents.agents())
			{
				partialMatching = partialMatching.withNewMatch(agent, chosenProject);

				System.out.printf("t: %s, worst: %s\n", t+agentsAdded, new MatchingMetrics.StudentProject(partialMatching).worstRank().asInt());

				remaningAgents.without(agent);
				agentsAdded++;

				if (new ActiveProjects(partialMatching, projects, remaningAgents, groupSizeConstraint).contains(chosenProject)) continue;
				else break;
			}

			t += agentsAdded;
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

	record WorstBestOffAgents(Project chosenProject, Set<Agent>agents)
	{
	}

	private WorstBestOffAgents worstBestOffAgents(Agents agents, Projects projects, SDPCPartialMatching partialMatching, GroupSizeConstraint groupSizeConstraint)
	{
		if (projects.count() == 0) {
			// No active projects
		}

		MinQuorumRequirement minQuorumRequirement = project -> {
			var partialGroupedByProject = partialMatching.groupedByProject();
			var currentlyMatchedToProject = partialGroupedByProject.get(project);

			int numCurrentlyMatchedToProject = currentlyMatchedToProject == null ? 0 : currentlyMatchedToProject.size();

			if (numCurrentlyMatchedToProject < groupSizeConstraint.minSize()) {
				return new NumAgentsTillQuorum(groupSizeConstraint.minSize() - numCurrentlyMatchedToProject);
			}

			if (numCurrentlyMatchedToProject >= groupSizeConstraint.minSize() && numCurrentlyMatchedToProject < groupSizeConstraint.maxSize()) {
				return new NumAgentsTillQuorum(groupSizeConstraint.maxSize() - numCurrentlyMatchedToProject);
			}

			if (numCurrentlyMatchedToProject == groupSizeConstraint.maxSize()) {
				return new NumAgentsTillQuorum(0);
			}

			var groupsFactorisation = GroupFactorization.cachedInstanceFor(groupSizeConstraint);
			var upperbound = project.slots().size() * groupSizeConstraint.maxSize();
			for (var i = numCurrentlyMatchedToProject; i <= upperbound; i++) {
				if (groupsFactorisation.isFactorableIntoValidGroups(i))
					return new NumAgentsTillQuorum(i - numCurrentlyMatchedToProject);
			}

			throw new RuntimeException("BUGCHECK: Something not working well");
		};

		var bla = WorstAmongBestProjectPairings.from(agents, projects, minQuorumRequirement, agents.datasetContext.allProjects().count());

		var eccentric = bla.orElseThrow();

		var mostEccentric = eccentric.pairingsAtK().stream()
			.min(Comparator.comparing(projectAgentsPairing -> projectAgentsPairing.agents().size()))
				.orElseThrow();

		return new WorstBestOffAgents(mostEccentric.project(), mostEccentric.agents());
	}

}
