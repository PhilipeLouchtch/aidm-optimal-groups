package nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism.groups.PossibleGroupings;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism.groups.PossibleGroupingsByIndividual;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism.model.EmptyMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism.model.PessimismMetric;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism.model.PessimismSolution;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import nl.tudelft.aidm.optimalgroups.search.DynamicSearch;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Pessimistic extends DynamicSearch<AgentToProjectMatching, PessimismSolution>
{

	// determine set of 'eccentric' students E - eccentric: student with lowest satisfaction
	// foreach s in E
	//     try all group combinations such that nobody in that group is worse off than s
	//     decrease slots of project p by 1


//	public static void thingy(String[] args)
//	{
//		int k = 8;
//
//		CourseEdition ce = CourseEdition.fromLocalBepSysDbSnapshot(10);
//		int minGroupSize = ce.groupSizeConstraint().minSize();
//
//		var result = ce.allAgents().asCollection().stream()
//			.map(agent -> agent.projectPreference().asListOfProjects())
//			.map(projectPreference -> topNElements(projectPreference, k))
//			.flatMap(Collection::stream)
//			.collect(Collectors.groupingBy(project -> project)).entrySet().stream()
//			.map(entry -> Pair.create(entry.getKey(), entry.getValue().size() / minGroupSize))
//			.filter(pair -> pair.getValue() > 0)
//			.sorted(Comparator.comparing((Pair<Project, Integer> pair) -> pair.getValue()))
//	//			.mapToInt(pair -> pair.getValue())
//	//			.sum();
////			.count();
//				.collect(Collectors.toList());
//
////		ce = new CourseEditionModNoPeerPref(ce);
//		var bepSysMatchingWhenNoPeerPrefs = new GroupProjectAlgorithm.BepSys().determineMatching(ce);
//
//		var metrics = new MatchingMetrics.StudentProject(AgentToProjectMatching.from(bepSysMatchingWhenNoPeerPrefs));
//
//		return;
//	}

	public static void main(String[] args)
	{
		CourseEdition ce = CourseEdition.fromLocalBepSysDbSnapshot(10);
		var thing = new Pessimistic(ce.allAgents(), ce.allProjects(), ce.groupSizeConstraint());
//		thing.determineK();

		var matching = thing.matching();

		var metrics = new MatchingMetrics.StudentProject(matching);

		return;
	}

	private final Agents agents;
	private final Projects projects;
	private final GroupSizeConstraint groupSizeConstraint;
	private final PossibleGroupings possibleGroups;
	private final GroupFactorization groupFactorization;

	public Pessimistic(Agents agents, Projects projects, GroupSizeConstraint groupSizeConstraint)
	{
		super(PessimismSolution.empty(agents.datasetContext));

		this.agents = agents;
		this.projects = projects;
		this.groupSizeConstraint = groupSizeConstraint;
		this.possibleGroups = new PossibleGroupingsByIndividual();

		this.groupFactorization = new GroupFactorization(groupSizeConstraint, agents.count());
	}

	public AgentToProjectMatching matching()
	{
		DatasetContext datsetContext = agents.datasetContext;
		var emptySolution = PessimismSolution.empty(datsetContext);

		var root = new PessimismSearchNode(emptySolution, agents, new DecrementableProjects(projects), groupSizeConstraint);

		// Run the algorithm with time constraints, after timeout we check best solution found up to that moment
		try {
			final ForkJoinPool forkJoinPool = new ForkJoinPool();

			// Threads of a parallel stream run in a pool, not as children of this thread
			// Hence, we provide a pool context which we control so that we can force shutdown
			forkJoinPool.execute(root::solution);
			forkJoinPool.awaitTermination(5, TimeUnit.MINUTES);
			if (bestSolutionSoFar.hasNonEmptySolution() == false) {
				// Give an extension...
				System.out.println("Pessimism: going in over-time...");
				forkJoinPool.awaitTermination(10, TimeUnit.MINUTES);
			}
			forkJoinPool.shutdownNow();
		}
		catch (Exception e) {
			Thread.currentThread().interrupt();
		}

		Assert.that(bestSolutionSoFar.hasNonEmptySolution())
			.orThrowMessage("Pessimism-search did not find a single valid solution :");

		var matching = bestSolutionSoFar.currentBest().matching();

		// Check all students matched
		Assert.that(agents.count() == matching.countDistinctStudents())
			.orThrowMessage("Not all agents were matched");

		// Check if all size constraints met as well
		var matchingGroupedByProject = matching.groupedByProject();
		for (var projectWithMatches : matchingGroupedByProject.entrySet()) {
			var project = projectWithMatches.getKey();
			var matches = projectWithMatches.getValue();
			Assert.that(groupFactorization.isFactorableIntoValidGroups(matches.size()))
				.orThrowMessage("Students matched to a project cannot be partitioned into groups");
		}

		return matching;
	}

	public class PessimismSearchNode extends SearchNode
	{
		private final PessimismSolution partial;

		private final Agents agents;
		private final DecrementableProjects projects;
		private final GroupSizeConstraint groupSizeConstraint;

		PessimismSearchNode(PessimismSolution partial, Agents agents, DecrementableProjects projects, GroupSizeConstraint groupSizeConstraint)
		{
			this.partial = partial;
			this.agents = agents;
			this.projects = projects;
			this.groupSizeConstraint = groupSizeConstraint;
		}

		@Override
		public Optional<PessimismSolution> solve()
		{
			// BOUND
			// Check if the partial solution has a worse absolute worst rank than the best-so-far
			// If it is indeed worse, do not continue searching further with this partial solution (it will never become beter)
//			if (bestSolutionSoFar.test(best -> best.metric.absoluteWorstRank.asInt() < partial.metric.absoluteWorstRank.asInt())) {
//				return Optional.empty();
//			}

			// If node has no agents to group, the partial solution is considered to be done
			if (agents.count() == 0) {
				bestSolutionSoFar.potentiallyUpdateBestSolution(bestSoFar -> {
					if (bestSoFar.metric().compareTo(partial.metric()) < 0) {
						return Optional.of(partial);
					}

					return Optional.empty();
				});

				return Optional.of(partial);
			}

			// If the remaining agents cannot be partitioned into groups of valid sizes,
			// then this search branch is to be terminated without a solution (aka empty solution)
			if (!groupFactorization.isFactorableIntoValidGroups(agents.count())) {
				return Optional.empty();
			}

			var kProjects = KProjectAgentsPairing.from(agents, projects, groupSizeConstraint);

			if (bestSolutionSoFar.test(solution -> solution.metric().worstRank().asInt() < kProjects.k())) {
				return Optional.empty();
			}

			var solution = kProjects.pairingsAtK()
//				.stream()
				.parallelStream()

				.flatMap(pairing -> {
					var possibleGroupmates = new LinkedHashSet<>(pairing.possibleGroupmates());
					// TODO HERE: group agents by "type"
					var possibleGrps =  possibleGroups.of(pairing.agents(), possibleGroupmates, groupSizeConstraint);
					return possibleGrps.stream()
						.map(possibleGroup -> new PairingWithPossibleGroup(pairing, possibleGroup));
				})
//				.parallel()

				.map(pairingWithPossibleGroup -> {
					var possibleGroup = pairingWithPossibleGroup.possibleGroup();
					var pairing = pairingWithPossibleGroup.pairing();

					Agents remainingAgents = agents.without(possibleGroup);
					DecrementableProjects projectsWithout = this.projects.decremented(pairing.project());

					var newPartial = partial.matching().withMatches(pairing.project(), possibleGroup);
					return new PessimismSearchNode(PessimismSolution.fromMatching(newPartial), remainingAgents, projectsWithout, groupSizeConstraint);
				})

				.map(SearchNode::solution)

				// Unpack optionals - filter out empty/invalid solutions
				.flatMap(Optional::stream)

				.max(Comparator.comparing(PessimismSolution::metric));

			return solution;
		}

		@Override
		protected boolean candidateSolutionTest(AgentToProjectMatching candidateSolution)
		{
			// I think...
			return true;
		}
	}

	private static record PairingWithPossibleGroup(ProjectAgentsPairing pairing, Set<Agent> possibleGroup) {}

}
