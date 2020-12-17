package nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.AllHumblePairingsSearch;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.model.MatchCandidate;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.MinQuorumRequirement;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.NumAgentsTillQuorum;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.WorstAmongBestProjectPairings;
import nl.tudelft.aidm.optimalgroups.dataset.DatasetContextTiesBrokenCommonly;
import nl.tudelft.aidm.optimalgroups.dataset.DatasetContextTiesBrokenIndividually;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatch;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.ListBasedMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.project.ListBasedProjects;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.util.Pair;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.util.stream.Collectors.*;

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
		var ce = DatasetContextTiesBrokenCommonly.from(CourseEdition.fromLocalBepSysDbSnapshot(10));
//		var ce = CourseEdition.fromLocalBepSysDbSnapshot(10);

		System.out.println(ce.identifier());

		var thing = new SDPCPessimism(ce.allAgents(), ce.allProjects(), ce.groupSizeConstraint());
//		thing.determineK();

		var matching = thing.matching();

		var metrics = new MatchingMetrics.StudentProject(matching);
		metrics.rankDistribution().displayChart();

		return;
	}

	public AgentToProjectMatching matching()
	{
		var remaningAgents = new LinkedList<>(agents.asCollection());
		var partialMatching = new SDPCPartialMatching(agents.datasetContext);

		int n = agents.count();
		for (int t = 1; t <= n;)
		{
			var activeProjects = new ActiveProjects(partialMatching, t);
//			DatasetContext datasetContext = null;

			// TODO: find 'eccentric' student
			var worstBestOffAgents = worstBestOffAgents(Agents.from(remaningAgents), activeProjects, partialMatching, groupSizeConstraint);
			var chosenProject = worstBestOffAgents.chosenProject();

			var agentsAdded = 0;
			for (var agent : worstBestOffAgents.agents())
			{
				partialMatching = partialMatching.withNewMatch(agent, chosenProject);

				System.out.printf("t: %s, worst: %s\n", t+agentsAdded, new MatchingMetrics.StudentProject(partialMatching).worstRank().asInt());

				remaningAgents.remove(agent);
				agentsAdded++;

				if (new ActiveProjects(partialMatching, t+agentsAdded).contains(chosenProject)) continue;
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

	private class SDPCPartialMatching extends ListBasedMatching<Agent, Project> implements AgentToProjectMatching
	{
		public SDPCPartialMatching(DatasetContext datasetContext)
		{
			super(datasetContext);
		}

		private SDPCPartialMatching(DatasetContext datasetContext, List<Match<Agent, Project>> matches)
		{
			super(datasetContext, matches);
		}

		public SDPCPartialMatching withNewMatch(Agent agent, Project project)
		{
			List<Match<Agent, Project>> matches = new ArrayList<>(this.asList());
			matches.add(new AgentToProjectMatch(agent, project));

			return new SDPCPartialMatching(datasetContext(), matches);
		}
	}

	public class ActiveProjects extends ListBasedProjects implements Projects
	{
		private final SDPCPartialMatching partialMatching;
		private final GroupFactorization groupFactorization;
		private final int currentStep;

		private Set<Project> asSet;
		private List<Project> asList;

		public ActiveProjects(SDPCPartialMatching partialMatching, int currentStep)
		{
			this.currentStep = currentStep;
			this.groupFactorization = GroupFactorization.cachedInstanceFor(groupSizeConstraint);
			this.partialMatching = partialMatching;
		}

		public boolean contains(Project project)
		{
			if (asSet == null) {
				asSet = new HashSet<>(projectList());
			}

			return asSet.contains(project);

		}

		@Override
		protected List<Project> projectList()
		{
			if (asList != null) {
				return asList;
			}

			var partialMatchingByProject = partialMatching.groupedByProject();

			// Definition 4 (i)
			var projectsNotFull = projects.asCollection().stream()
				.map(project -> Pair.create(project, partialMatchingByProject.getOrDefault(project, List.of())))
				.filter(projectAndMatchedAgents -> {
					var project = projectAndMatchedAgents.getKey();
					var agentsMatchedToProject = projectAndMatchedAgents.getValue();

					return agentsMatchedToProject.size() < (project.slots().size() * groupSizeConstraint.maxSize());
				})
				.collect(toMap(Pair::getKey, Pair::getValue));


			var breakpoint = partialMatchingByProject.entrySet().stream()
				.filter(proj -> partialMatchingByProject.containsKey(proj.getKey()))
				.filter(proj -> partialMatchingByProject.get(proj.getKey()).size() >= (groupSizeConstraint.maxSize() * proj.getKey().slots().size()))
				.filter(proj -> projectsNotFull.containsKey(proj.getKey()))
				.findFirst();

			// Put in all project that have no one assigned yet
//			for (var project : projects.asCollection()) {
//				if (partialMatchingByProject.getOrDefault(project, List.of()).size() < 5)
//					projectsNotFull.computeIfAbsent(project, __ -> List.of());
//			}

//			int sum = projectsNotFull.values().stream().mapToInt(value -> groupSizeConstraint.maxSize() - value.size()).sum();
//			Assert.that(sum == (agents.count() - (currentStep - 1)))
//				.orThrowMessage("Boe!");

			// Defintion 4 (ii)
			var activeAsList = projectsNotFull.entrySet().stream().
				filter(projectWithCapacityAndMatchedStudents -> {
					// p \in P
					var project = projectWithCapacityAndMatchedStudents.getKey();
					var agentsMatchedToProject = projectWithCapacityAndMatchedStudents.getValue();

					// Intuitively, the true question we're asking here, is that "if another student is matched to this
					// project, can these students be partitioned into valid groups? Or if not, then how many must be
					// matched to it still, such that valid groups _can_ be created?" Then we also ask the same question
					// regarding projects that have students assigned in the previous steps (that the student at 'current
					// step' did not pick). * We're not really in a step here, but assume per project that it is picked
					// and see if this would result in a feasible matching. If it is determined not feasible, the project
					// is considered "closed" (not selectable in this round).
					var projectQuorumIfNewAssigned = computeQuorum(project, agentsMatchedToProject.size() + 1);
					var studentsProjectStillNeeds = max(projectQuorumIfNewAssigned - (agentsMatchedToProject.size() + 1), 0);

					var neededForOtherProjects =
						//     SUM            [
						// p' \in P_t-1 / {p} [
						projectsNotFull.entrySet().stream()
							.filter(entry -> entry.getKey() != project)
							.filter(entry -> entry.getValue().size() > 0)
							.mapToInt(otherProjectWithMatchedStudents -> {
								int numMatchedToOther = otherProjectWithMatchedStudents.getValue().size();
								var otherQuorum = computeQuorum(project, numMatchedToOther);
								// max{q_p' - sigma_p'(t - 1), 0}
								return max(otherQuorum - numMatchedToOther, 0);
							})
						.sum();
						// ]
						// ]

					return studentsProjectStillNeeds + neededForOtherProjects <= ( agents.count() - currentStep);
				})
				.map(Map.Entry::getKey)
				.collect(toList());

			this.asList = activeAsList;
			return asList;
		}

		private int computeQuorum(Project project, int numStudents)
		{
			Assert.that(numStudents <= groupSizeConstraint.maxSize() * project.slots().size())
				.orThrowMessage(
					String.format("No quorum possible for project %s, numStudents %s exceeds max total capacity...", project, numStudents));

			// We're searching for the smallest number of students that can be partitioned into groups
			// of valid sizes. Where this smallest number is >= numStudents.
			// In the paper of Monte and Tumennsasan this is a simple number, in our case, projects have
			// capacities for multiple groups. So a quorum in our case is dynamic as it shifts
			// Example, with groupSizeConstraint = [4,5], when (agents assigned to project p) = ( |mu(p)| ) is less than 5,
			// the quorum is 4. When the |mu(p)| = 6, the quorum for p becomes 8 (two groups of size 4) and remains that until
			// |mu(p)| becomes larger than 10, then it is 12 and so on...
			int quorum = numStudents;
			while (!groupFactorization.isFactorableIntoValidGroups(quorum)) {
				quorum++;
			}

			// (next) number of students that can be partitioned into valid group(s)
			// quorum != numStudents if numStudents could not be partitioned
			return quorum;
		}
	}
}
