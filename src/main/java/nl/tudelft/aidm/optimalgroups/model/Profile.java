package nl.tudelft.aidm.optimalgroups.model;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public interface Profile
{
	/**
	 * Iterates in a 'binned' fashion. For each rank the number of agents is given.
	 * For example, a profile (1,1,1,2,2,2,2,5).forEach will give the following results:
	 *  (1, 3), (2,4), (5,1)
	 *
	 *  The iteration order is from rank 1 -> rank n
	 * @param consumer The function accepting iterations
	 */
	void forEach(ProfileConsumer consumer);

	int numAgentsWithRank(int rank);

	/**
	 * The number of agents of whom the profile is made
	 */
	int numAgents();

	/**
	 *	The higest rank contained in the profile
	 */
	int maxRank();
	
	default Profile subtracted(Profile other)
	{
		var maxRank = Math.max(this.maxRank(), other.maxRank());
		var profileDelta = new int[maxRank+1];
		
		for (int i = 1; i < maxRank; i++)
		{
			profileDelta[i] = this.numAgentsWithRank(i) - other.numAgentsWithRank(i);
		}
		
		return new Simple(profileDelta);
	}
	
	
	
	/* Factory methods  */
	static Profile of(Matching<Agent, Project> matching)
	{
		return Profile.of(matching, matching.asList().stream().map(Match::from).collect(Agents.collector));
	}
	
	static Profile of(Matching<Agent, Project> matching, Agents agentsToProfile)
	{
		return matching.asList().stream()
				// Only agents that are to be included
				.filter(match -> agentsToProfile.contains(match.from()))
				.filter(match -> !match.from().projectPreference().isCompletelyIndifferent())
				// A profile is a sorted list of ranks
				.map(match -> {
				   var rank = match.from().projectPreference().rankOf(match.to());
				   Assert.that(rank.isPresent()).orThrowMessage("Rank not present, handle this case"); // should use RankInPref over integers in that case
				   return rank.asInt();
				})
				.sorted()
				.collect(collectingAndThen(toList(), Profile::fromRanks));
	}
	
	static Profile fromRanks(List<Integer> ranks)
	{
		var numStudentsInProfile = ranks.size();
		
		var numPerRank = ranks.stream()
			.collect(Collectors.groupingBy(i -> i, Collectors.counting()));

		var maxRank = numPerRank.keySet().stream().mapToInt(value -> value).max()
			.orElse(0); // Profile is empty
		
		var binnedProfile = new int[maxRank + 1]; // must be up to including maxRank
		binnedProfile[0] = 0; // nobody - rank 0 doesn't exist

		for (int i = 1; i <= maxRank; i++)
		{
			var numStudentsWithRankI = numPerRank.getOrDefault(i, 0L);
			binnedProfile[i] = numStudentsWithRankI.intValue();
		}
		
		return new Simple(binnedProfile);
	}
	
	static Profile fromProfileArray(int... profileAsArray)
	{
		return new Simple(profileAsArray);
	}

	
	/* Supporting types */
	interface ProfileConsumer
	{
		void apply(int rank, int count);
	}


	/* Implementations */
	class Simple implements Profile
	{
		private final int maxRank;
		private final int numStudentsInProfile;
		private final int[] numStudentsByRank;
		
		public Simple(int[] numStudentsByRank)
		{
			this.numStudentsByRank = numStudentsByRank;
			this.maxRank = numStudentsByRank.length - 1;
			this.numStudentsInProfile = Arrays.stream(numStudentsByRank).sum();
		}

		@Override
		public int numAgentsWithRank(int rank)
		{
			Assert.that(0 <= rank).orThrowMessage("Rank out of bounds");
			
			if (rank >= numStudentsByRank.length) {
				return 0;
			}
			else {
				return numStudentsByRank[rank];
			}
		}

		@Override
		public void forEach(ProfileConsumer consumer)
		{
			for (int rank = 1; rank <= maxRank(); rank++)
			{
				int numWithRank = numAgentsWithRank(rank);
				consumer.apply(rank, numWithRank);
			}
		}

		@Override
		public int numAgents()
		{
			return numStudentsInProfile;
		}

		@Override
		public int maxRank()
		{
			return maxRank;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (!(o instanceof Simple))
				return false;
			Simple simple = (Simple) o;
			return Arrays.equals(numStudentsByRank, simple.numStudentsByRank);
		}
		
		@Override
		public int hashCode()
		{
			return Arrays.hashCode(numStudentsByRank);
		}
	}
}
