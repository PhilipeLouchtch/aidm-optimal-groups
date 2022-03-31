package nl.tudelft.aidm.optimalgroups.metric;

import nl.tudelft.aidm.optimalgroups.Algorithm;
import nl.tudelft.aidm.optimalgroups.metric.rank.AssignedRank;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A matching is popular if there are more agents preferring it to the alternative.
 * Non-transitive: a,b,c in Algorithm then if: (a P b /\  b P c) does not imply a P c
 */
public class PopularityMatrix2<MATCHING extends Matching<Agent, Project>, ALGORES extends Algorithm.Result<? extends Algorithm, MATCHING>>
{
	private final List<ALGORES> algoResults;
	private final List<MatchingPopularityComparison<ALGORES>> algoPopularityComparisons;
	
	/**
	 * A popularity matrix of students in the given results
	 * @param results The results to compare
	 * @param filter Optional param: will only consider the given students -
	 *               NOTE: this is a vararg parameter, but at most one value can be given!
	 *               (Misused as an optional argument)
	 */
	public static <MATCHING extends Matching<Agent, Project>, ALGORES extends Algorithm.Result<? extends Algorithm, MATCHING>>
			PopularityMatrix2<MATCHING, ALGORES> from(List<ALGORES> results, Agents... filter)
	{
		Assert.that(filter.length <= 1).orThrowMessage("Give at most one filter param!");
		
		var allAgents = (results.stream().map(result -> result.producedMatching().datasetContext()).findAny().orElseThrow()).allAgents();
		
		var agentsToConsider = filter.length == 1 ? filter[0] : allAgents;
		
		return new PopularityMatrix2<>(results, agentsToConsider);
	}
	
	/**
	 * A popularity matrix of students in the given results
	 * @param results The results to compare
	 * @param agentsToConsider The agents to include in the "voting"
	 */
	public PopularityMatrix2(List<ALGORES> results, Agents agentsToConsider)
	{
		this.algoResults = results;

		var allMatchingsBasedOnSameDatasetContext = results.stream()
			.map(algoResult -> algoResult.producedMatching().datasetContext())
			.distinct()
			.count() == 1;

		Assert.that(allMatchingsBasedOnSameDatasetContext)
				.orThrowMessage("Popularity metrix can only be determined for matchings based of same dataset context.");
		
		var allAgents = (results.stream().map(result -> result.producedMatching().datasetContext()).findAny().orElseThrow()).allAgents();

		this.algoPopularityComparisons = new ArrayList<>();

		for (var result : results) {
			for (var otherResult : results) {

				if (result != otherResult) {
					var comparison = new MatchingPopularityComparison(result, otherResult, agentsToConsider);
					algoPopularityComparisons.add(comparison);
				}

			}
		}
	}

	public Set<MatchingPopularityComparison<ALGORES>> asSet()
	{
		return new HashSet<>(algoPopularityComparisons);
	}
	
	public List<MatchingPopularityComparison<ALGORES>> asList()
	{
		return Collections.unmodifiableList(algoPopularityComparisons);
	}
	
	public List<MatchingPopularityComparison<ALGORES>> deduplicatedByWinner()
	{
		var asSet = this.asSet();
		
		var wins = asSet.stream().map(MatchingPopularityComparison::winner)
				.flatMap(Optional::stream)
				.collect(Collectors.groupingBy(a -> a, Collectors.counting()));
		
		var algos = asList().stream()
				.mapMulti((MatchingPopularityComparison<ALGORES> compResult, Consumer<Algorithm> consumer) -> {
					consumer.accept(compResult.resultA.algo());
					consumer.accept(compResult.resultB.algo());
				})
				.distinct()
				.toList();
		
		var results = new HashMap<Algorithm, List<MatchingPopularityComparison<ALGORES>>>();
		for (Algorithm thisAlgo : algos)
		{
			var winningOrTiedComps = asList().stream()
					// consider all comparisons where this algo is on the left-hand-side (resultA) - we can do this because we have comparisons for both sides in the list
					//  then, of those, consider only where this algo wins or ties (in which case, there is no winner, we use a trick with Optional#orElse)
					.filter(comp ->
							(comp.resultA.algo()).equals(thisAlgo) &&
							(comp.winner().orElse(thisAlgo)).equals(thisAlgo)
					)
					.toList();
			
			results.put(thisAlgo, winningOrTiedComps);
		}
		
		return algos.stream().sorted((Comparator.comparing(a -> wins.getOrDefault(a, 0L))).reversed())
				.mapMulti((Algorithm algorithm, Consumer<MatchingPopularityComparison<ALGORES>> putIntoStream) -> {
					results.get(algorithm).forEach(putIntoStream);
				})
				.toList();
	}

	public static class MatchingPopularityComparison<ALGORES extends Algorithm.Result<? extends Algorithm, ? extends Matching<Agent, Project>>>
	{
		private final ALGORES resultA;
		private final ALGORES resultB;
		
		private final int numAgentsPreferingA;
		private final int numAgentsPreferingB;
		
		public MatchingPopularityComparison(ALGORES resultA, ALGORES resultB, Agents agentsToConsider)
		{
			this.resultA = resultA;
			this.resultB = resultB;
			
			// Ranking of assigned project by Agents in matching A
			var rankInMatchingA = AssignedRank.ProjectToStudent.inStudentMatching(resultA.producedMatching())
				.collect(Collectors.toMap(AssignedRank.ProjectToStudent::student, AssignedRank.ProjectToStudent::asInt));

			// Ranking of assigned project by Agents in matching B
			var rankInMatchingB = AssignedRank.ProjectToStudent.inStudentMatching(resultB.producedMatching())
				.collect(Collectors.toMap(AssignedRank.ProjectToStudent::student, AssignedRank.ProjectToStudent::asInt));


			numAgentsPreferingA = (int) agentsToConsider.asCollection().stream()
				.filter(agent -> {
					int rankInA = rankInMatchingA.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
					int rankInB = rankInMatchingB.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
					// All agents who prefer matching A to B (rank the assigned project in matching A higher than in B)
					return rankInA < rankInB;
				})
				.count();

			numAgentsPreferingB = (int) agentsToConsider.asCollection().stream()
				.filter(agent -> {
					int rankInB = rankInMatchingB.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
					int rankInA = rankInMatchingA.getOrDefault(agent, OptionalInt.empty()).orElse(Integer.MAX_VALUE);
					return rankInB < rankInA;
				})
				.count();
		}
		
		/**
		 * @return The winning algorithm of this comparison, empty if tie
		 */
		public Optional<Algorithm> winner()
		{
			if (numAgentsPreferingA > numAgentsPreferingB) return Optional.of(resultA.algo());
			if (numAgentsPreferingB > numAgentsPreferingA) return Optional.of(resultB.algo());
			return Optional.empty();
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
			MatchingPopularityComparison that = (MatchingPopularityComparison) o;
			
			// Algo are same in both comparisons
			if (this.resultA.algo() == that.resultA.algo() && this.resultB.algo() == that.resultB.algo())
			{
				Assert.that(this.numAgentsPreferingA == that.numAgentsPreferingA && this.numAgentsPreferingB == that.numAgentsPreferingB).orThrowMessage("Algos match, results do not");
				return true;
			}
			
			// The inverse case, this A is that B
			if (this.resultA.algo() == that.resultB.algo() && this.resultB.algo() == that.resultA.algo())
			{
				Assert.that(this.numAgentsPreferingA == that.numAgentsPreferingB && this.numAgentsPreferingB == that.numAgentsPreferingA).orThrowMessage("Algos match, results do not");
				return true;
			}
			
			return false;
		}

		@Override
		public int hashCode()
		{
			// Ensure hashCode is invarant over the order of params
			// (comparison(a, b).equals( comparison(b,a) ) implies comparison(a,b).hashCode == comaprison(b,a).hashCode
			
			if (numAgentsPreferingA >= numAgentsPreferingB)
				return Objects.hash(resultA.algo(), resultB.algo());
			
			return Objects.hash(resultB.algo(), resultA.algo());
		}
	}


}
