package nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.PossibleGroupings;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.PossibleGroupingsByIndividual;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.model.DecrementableProjects;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.model.PessimismMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.model.PessimismSolution;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.MinQuorumRequirement;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.NumAgentsTillQuorum;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.pairing.WorstAmongBestProjectPairings;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.model.MatchCandidate;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import nl.tudelft.aidm.optimalgroups.search.DynamicSearch;
import org.jetbrains.annotations.NotNull;
import plouchtch.assertion.Assert;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class WorstAmongBestHumblePairingsSearch extends DynamicSearch<AgentToProjectMatching, PessimismSolution>
{

	// determine set of 'eccentric' students E - eccentric: student with lowest satisfaction
	// foreach s in E
	//     try all group combinations such that nobody in that group is worse off than s
	//     decrease slots of project p by 1


//	public static void thingy(String[] args)
//	{
//		int k = 8;
//
//		CourseEdition ce = CourseEditionFromDb.fromLocalBepSysDbSnapshot(10);
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
//		var ce = DatasetContextTiesBrokenIndividually.from(CourseEditionFromDb.fromLocalBepSysDbSnapshot(10));
		var ce = CourseEditionFromDb.fromLocalBepSysDbSnapshot(10);
		var thing = new WorstAmongBestHumblePairingsSearch(ce.allAgents(), ce.allProjects(), ce.groupSizeConstraint());
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

	public WorstAmongBestHumblePairingsSearch(Agents agents, Projects projects, GroupSizeConstraint groupSizeConstraint)
	{
		this(agents, projects, groupSizeConstraint, projects.count() + 1);
	}

	public WorstAmongBestHumblePairingsSearch(Agents agents, Projects projects, GroupSizeConstraint groupSizeConstraint, int worstRankBound)
	{
		super(PessimismSolution.emptyWithBoundedWorstRank(agents.datasetContext, worstRankBound));

		this.agents = agents;
		this.projects = projects;
		this.groupSizeConstraint = groupSizeConstraint;
		this.possibleGroups = new PossibleGroupingsByIndividual();

		this.groupFactorization = new GroupFactorization(groupSizeConstraint, agents.count());
	}

	public AgentToProjectMatching matching()
	{
		DatasetContext datsetContext = agents.datasetContext;

		var root = new PessimismSearchNode(agents, new DecrementableProjects(projects), groupSizeConstraint);

		// Run the algorithm with time constraints, after timeout we check best solution found up to that moment
		try {
			final ForkJoinPool forkJoinPool = new ForkJoinPool();

			// Threads of a parallel stream run in a pool, not as children of this thread
			// Hence, we provide a pool context which we control so that we can force shutdown
			forkJoinPool.execute(root::solution);
			forkJoinPool.awaitTermination(5, TimeUnit.MINUTES);
			if (bestSolutionSoFar.hasNonEmptySolution() == false) {
				// Give an extension...
				System.out.println("Pessimism: entering over-time...");
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

		public PessimismSearchNode(Agents agents, DecrementableProjects projects, GroupSizeConstraint groupSizeConstraint)
		{
			this.partial = PessimismSolution.empty(agents.datasetContext);
			this.agents = agents;
			this.projects = projects;
			this.groupSizeConstraint = groupSizeConstraint;
		}

		private PessimismSearchNode(PessimismSolution partial, Agents agents, DecrementableProjects projects, GroupSizeConstraint groupSizeConstraint)
		{
			this.partial = partial;
			this.agents = agents;
			this.projects = projects;
			this.groupSizeConstraint = groupSizeConstraint;
		}

		@Override
		public Optional<PessimismSolution> solve()
		{
			if (agents.count() == 0) {
				bestSolutionSoFar.potentiallyUpdateBestSolution(partial);
				return Optional.of(partial);
			}

			// BOUND
			// Check if the partial solution has a worse absolute worst rank than the best-so-far
			// If it is indeed worse, do not continue searching further with this partial solution (it will never become beter)
			boolean partialIsAlreadyWorseThanCurrentBest = partial.matching().asList().size() != 0 && bestSolutionSoFar.test(best -> best.isBetterThan(partial));
			if (partialIsAlreadyWorseThanCurrentBest) {
				return Optional.empty();
			}

			// If the remaining agents cannot be partitioned into groups of valid sizes,
			// then this search branch is to be terminated without a solution (aka empty solution)
			// (because we are looking for a complete matching)
			if (!groupFactorization.isFactorableIntoValidGroups(agents.count())) {
				return Optional.empty();
			}

			var bestWorstRankSoFar = bestSolutionSoFar.currentBest().metric().worstRank().asInt();

			MinQuorumRequirement minQuorumRequirement = new MinQuorumReqTillNextQuorum();
			var essentialPairing = WorstAmongBestProjectPairings.from(agents, projects, minQuorumRequirement, bestWorstRankSoFar);

			if (essentialPairing.isEmpty()) {
				return Optional.empty();
			}

			var solution = essentialPairing.stream().parallel()
				.flatMap(p -> p.pairingsAtK().stream())

				.flatMap(this::intoAllPossibleGroupCombinationsPerPairing)

				.takeWhile(x -> x.kRank() <= bestSolutionSoFar.currentBest().metric().worstRank().asInt())

				.map(this::assumeGroupAndRecurseDeeper)

				.map(SearchNode::solution)

				// Unpack optionals - filter out empty/invalid solutions
				.flatMap(Optional::stream)

				.max(Comparator.comparing(PessimismSolution::metric));

			return solution;
		}

		@NotNull
		private Stream<? extends GroupProjectPairing> intoAllPossibleGroupCombinationsPerPairing(MatchCandidate pairing)
		{
			// TODO OPT: group agents by "type" to decrease the amount of symmetric combinations. Where type is to be defined as
			//  "only the effective part of the agent's preference".


			// The MinTillQuorum approach:
			//  use as many "include" agents as possible, do not use possible group mates unless needed

			var possibleGrps = possibleGroups.of(pairing.agents(), pairing.possibleGroupmates(), groupSizeConstraint);
			return possibleGrps.map(possibleGroup -> new GroupProjectPairing(pairing.kRank(), pairing.project(), possibleGroup));
		}

		private PessimismSearchNode assumeGroupAndRecurseDeeper(GroupProjectPairing groupProjectPairing)
		{
			var group = groupProjectPairing.group();
			var project = groupProjectPairing.project();

			Assert.that(group.size() >= groupSizeConstraint.minSize()).orThrowMessage("Group is smaller than min size");
			Assert.that(group.size() <= groupSizeConstraint.maxSize()).orThrowMessage("Group is larger than max size");

			Agents remainingAgents = agents.without(group);
			DecrementableProjects remainingProjects = this.projects.decremented(project);

			var newPartial = partial.matching().withMatches(project, group);
			PessimismSolution solutionSoFar = PessimismSolution.fromMatching(newPartial);
			return new PessimismSearchNode(solutionSoFar, remainingAgents, remainingProjects, groupSizeConstraint);
		}

		@Override
		protected boolean candidateSolutionTest(AgentToProjectMatching candidateSolution)
		{
			// I think...
			return true;
		}

		private class MinQuorumReqTillNextQuorum implements MinQuorumRequirement
		{
			@Override
			public NumAgentsTillQuorum forProject(Project project)
			{
				var partialGroupedByProject = partial.matching().groupedByProject();
				var currentlyMatchedToProject = partialGroupedByProject.get(project);

				int numCurrentlyMatchedToProject = currentlyMatchedToProject == null ? 0 : currentlyMatchedToProject.size();

				if (numCurrentlyMatchedToProject < groupSizeConstraint.minSize()) {
					return new NumAgentsTillQuorum(groupSizeConstraint.minSize() - numCurrentlyMatchedToProject);
				}

				if (numCurrentlyMatchedToProject >= groupSizeConstraint.minSize() && numCurrentlyMatchedToProject < groupSizeConstraint.maxSize()) {
					return new NumAgentsTillQuorum(groupSizeConstraint.maxSize() - numCurrentlyMatchedToProject);
				}

//				if (numCurrentlyMatchedToProject == groupSizeConstraint.maxSize()) {
//					return new NumAgentsTillQuorum(0);
//				}

				// We're over the max-group-size, so we have to find the next number that is factorable into groups
				// for example, let g-min=4 and g-max=6, then if numCurrentlyMatched is 6, the NumAgentsTillQuorum is 2
				// because 2x groups of 4 is the next number of students that can be divided into a valid number of validly
				// sized groups.
				var groupsFactorisation = GroupFactorization.cachedInstanceFor(groupSizeConstraint);
				var upperbound = project.slots().size() * groupSizeConstraint.maxSize();
				for (var i = numCurrentlyMatchedToProject; i <= upperbound; i++) {
					if (groupsFactorisation.isFactorableIntoValidGroups(i))
						return new NumAgentsTillQuorum(i - numCurrentlyMatchedToProject);
				}

				throw new RuntimeException("BUGCHECK: Something not working well");
			}
		}
	}

	private static record GroupProjectPairing(int kRank, Project project, List<Agent> group) {}



}
