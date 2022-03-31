package nl.tudelft.aidm.optimalgroups.metric;

import nl.tudelft.aidm.optimalgroups.Algorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.AgentProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.metric.rank.AssignedRank;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * A matching is popular if there are more agents preferring it to the alternative.
 * Non-transitive: a,b,c in Algorithm then if: (a P b /\  b P c) does not imply a P c
 */
public class PopularityMatrix<MATCHING extends Matching, ALGO extends Algorithm, ALGORES extends Algorithm.Result<ALGO, MATCHING>>
{
	/**
	 * Popularity Matrix from a {@link GroupToProjectMatching} type matching
	 */
	public static class TopicGroup extends PopularityMatrix<GroupToProjectMatching<Group.FormedGroup>, GroupProjectAlgorithm, GroupProjectAlgorithm.Result>
	{
		public TopicGroup(List<? extends GroupProjectAlgorithm.Result> resultingMatchings)
		{
			super(resultingMatchings, MatchingPopularityComparisonTopicGroup::new);
		}

		static class MatchingPopularityComparisonTopicGroup extends MatchingPopularityComparison<GroupProjectAlgorithm.Result>
		{
			public MatchingPopularityComparisonTopicGroup(GroupProjectAlgorithm.Result a, GroupProjectAlgorithm.Result b)
			{
				super(a, b);

				var allAgents = a.producedMatching().datasetContext().allAgents();

				// Ranking of assigned project by Agents in matching A
				var rankInMatchingA = AssignedRank.ProjectToStudent.inGroupMatching(a.producedMatching())
					.collect(Collectors.toMap(AssignedRank.ProjectToStudent::student, AssignedRank.ProjectToStudent::asInt));

				// Ranking of assigned project by Agents in matching B
				var rankInMatchingB = AssignedRank.ProjectToStudent.inGroupMatching(b.producedMatching())
					.collect(Collectors.toMap(AssignedRank.ProjectToStudent::student, AssignedRank.ProjectToStudent::asInt));


				numAgentsPreferingA = (int) allAgents.asCollection().stream()
					.filter(agent -> {
						int rankInA = rankInMatchingA.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						int rankInB = rankInMatchingB.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						// All agents who prefer matching A to B (rank the assigned project in matching A higher than in B)
						return rankInA < rankInB;
					})
					.count();

				numAgentsPreferingB = (int) allAgents.asCollection().stream()
					.filter(agent -> {
						int rankInB = rankInMatchingB.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						int rankInA = rankInMatchingA.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						return rankInB < rankInA;
					})
					.count();
			}
		}
	}

	public static class StudentProject extends PopularityMatrix<AgentToProjectMatching, AgentProjectAlgorithm, AgentProjectAlgorithm.Result>
	{
		public StudentProject(List<? extends AgentProjectAlgorithm.Result> resultingMatchings)
		{
			super(resultingMatchings, MatchingPopularityComparisonStudentTopic::new);
		}

		static class MatchingPopularityComparisonStudentTopic extends MatchingPopularityComparison<AgentProjectAlgorithm.Result>
		{
			public MatchingPopularityComparisonStudentTopic(AgentProjectAlgorithm.Result a, AgentProjectAlgorithm.Result b)
			{
				super(a, b);

				var allAgents = a.producedMatching().datasetContext().allAgents();

				// Ranking of assigned project by Agents in matching A
				var rankInMatchingA = AssignedRank.ProjectToStudent.inStudentMatching(a.producedMatching())
					.collect(Collectors.toMap(AssignedRank.ProjectToStudent::student, AssignedRank.ProjectToStudent::asInt));

				// Ranking of assigned project by Agents in matching B
				var rankInMatchingB = AssignedRank.ProjectToStudent.inStudentMatching(b.producedMatching())
					.collect(Collectors.toMap(AssignedRank.ProjectToStudent::student, AssignedRank.ProjectToStudent::asInt));


				numAgentsPreferingA = (int) allAgents.asCollection().stream()
					.filter(agent -> {
						int rankInA = rankInMatchingA.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						int rankInB = rankInMatchingB.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						// All agents who prefer matching A to B (rank the assigned project in matching A higher than in B)
						return rankInA < rankInB;
					})
					.count();

				numAgentsPreferingB = (int) allAgents.asCollection().stream()
					.filter(agent -> {
						int rankInB = rankInMatchingB.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						int rankInA = rankInMatchingA.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
						return rankInB < rankInA;
					})
					.count();
			}
		}
	}

	/* POPULARITY MATRIX IMPL BELOW */
	private List<? extends ALGORES> algoResults;
	private List<MatchingPopularityComparison<ALGORES>> algoPopularityComparisons;

	public PopularityMatrix(List<? extends ALGORES> resultingMatchings, BiFunction<ALGORES, ALGORES, MatchingPopularityComparison<ALGORES>> compare)
	{
		this.algoResults = resultingMatchings;

		var allMatchingsBasedOnSameDatasetContext = resultingMatchings.stream()
			.map(algoResult -> algoResult.producedMatching().datasetContext())
			.distinct()
			.count() == 1;

		Assert.that(allMatchingsBasedOnSameDatasetContext).orThrow(RuntimeException.class,
			"Popularity metrix can only be determined for matchings based of same dataset context.");

		this.algoPopularityComparisons = new ArrayList<>();

		for (var result : resultingMatchings) {
			for (var otherResult : resultingMatchings) {

				if (result != otherResult) {
					var comparison = compare.apply(result, otherResult);
					algoPopularityComparisons.add(comparison);
				}

			}
		}
	}

	public Set<MatchingPopularityComparison<ALGORES>> asSet()
	{
		return new HashSet<>(algoPopularityComparisons);
	}

	public static abstract class MatchingPopularityComparison<ALGORES extends Algorithm.Result>
	{
		private final ALGORES resultA;
		private final ALGORES resultB;

		int numAgentsPreferingA;
		int numAgentsPreferingB;

		public MatchingPopularityComparison(ALGORES resultA, ALGORES resultB)
		{
			this.resultA = resultA;
			this.resultB = resultB;
		}

		@Override
		public String toString()
		{
			String winner = "?";
			if (numAgentsPreferingA > numAgentsPreferingB) winner = " > ";
			else if (numAgentsPreferingA < numAgentsPreferingB) winner = " < ";
			else if (numAgentsPreferingA == numAgentsPreferingB) winner = " = ";

			return String.format("%s (%s) %s (%s) %s ", resultA.algo().name(), numAgentsPreferingA, winner, numAgentsPreferingB, resultB.algo().name());
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof MatchingPopularityComparison)) return false;
			MatchingPopularityComparison<?> that = (MatchingPopularityComparison<?>) o;
			return numAgentsPreferingA == that.numAgentsPreferingA &&
				numAgentsPreferingB == that.numAgentsPreferingB &&
				resultA.equals(that.resultA) &&
				resultB.equals(that.resultB);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(resultA, resultB, numAgentsPreferingA, numAgentsPreferingB);
		}
	}


}
