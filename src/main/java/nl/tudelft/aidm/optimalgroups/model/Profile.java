package nl.tudelft.aidm.optimalgroups.model;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	 *	The highest rank contained in the profile. Returns 0 if profile is empty
	 */
	int maxRank();
	
	default ProfileDelta minus(Profile other)
	{
		var maxRank = Math.max(this.maxRank(), other.maxRank());
		var profileDelta = new int[maxRank+1];
		
		for (int i = 1; i <= maxRank; i++)
		{
			profileDelta[i] = this.numAgentsWithRank(i) - other.numAgentsWithRank(i);
		}
		
		return new ProfileDelta(profileDelta);
	}

	default int[] asZeroIndexed() {
		var zeroIndexed = new int[this.maxRank()];
		for (int i = 0; i < this.maxRank(); i++) {
			zeroIndexed[i] = this.numAgentsWithRank(i+1);
		}
		return zeroIndexed;
	}
	
	/* Factory methods  */
	static Profile of(Matching<Agent, Project> matching)
	{
		Agents all = matching.asList().stream().map(Match::from).collect(Agents.collector);
		return Profile.of(matching, all);
	}
	
	static Profile of(Matching<Agent, Project> matching, Agents agentsToProfile)
	{
		return matching.asList().stream()
				// Only agents that are to be included
				.filter(match -> agentsToProfile.contains(match.from()))
				// ignore indifferent agents, they have no rank
				.filter(match -> !match.from().projectPreference().isCompletelyIndifferent())
				// A profile is a sorted list of ranks
				.map(match -> {
				   var rank = match.from().projectPreference().rankOf(match.to());
				   
				   // HACK: remove me after data collection
					if (!rank.isPresent())
						return match.from().projectPreference().maxRank().getAsInt() + 1;
				   
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
		
		var profile = new int[maxRank + 1]; // must be up to including maxRank
		profile[0] = 0; // profile is 1-index based

		for (int i = 1; i <= maxRank; i++)
		{
			var numStudentsWithRankI = numPerRank.getOrDefault(i, 0L);
			profile[i] = numStudentsWithRankI.intValue();
		}
		
		return ArrayBased.fromOneIndexed(profile);
	}
	
	static Profile empty()
	{
		return ArrayBased.fromZeroIndexed();
	}

	/* Supporting types */
	interface ProfileConsumer
	{
		void apply(int rank, int count);
	}

	static Profile fromZeroIndexed(int... zeroIndexed) {
		return ArrayBased.fromZeroIndexed(zeroIndexed);
	}

	static Profile fromOneIndexed(int... oneIndexed) {
		return ArrayBased.fromOneIndexed(oneIndexed);
	}

	/* Implementations */
	class ArrayBased implements Profile
	{
		protected final int maxRank;
		protected final int numStudentsInProfile;
		protected final int[] numStudentsByRank;

		private static ArrayBased fromZeroIndexed(int... zeroIndexed) {
			int[] oneIndexed = new int[zeroIndexed.length+1];
			oneIndexed[0] = 0;
            System.arraycopy(zeroIndexed, 0, oneIndexed, 1, zeroIndexed.length);
            return new ArrayBased(oneIndexed);
		}

		private static ArrayBased fromOneIndexed(int... oneIndexed) {
			Assert.that(oneIndexed.length >= 1).orThrowMessage("1-Indexed must be at least of length 1");
			return new ArrayBased(oneIndexed);
		}
		
		/**
		 * Initializes this Array-backed Profile implementation from the given array
		 *
		 * The Array is direct mapping between ranks and the number of agents with that rank.
		 * Rank 0 is not a valid rank and must be empty or will trigger a bugcheck.
		 * @param numStudentsByRank
		 */
		private ArrayBased(int... numStudentsByRank)
		{
			Assert.that(!(numStudentsByRank.length > 0) || numStudentsByRank[0] == 0)
					.orThrowMessage("There cannot be any students with rank 0, bug?");
			
			this.numStudentsByRank = numStudentsByRank;
			this.maxRank = Math.max(0, numStudentsByRank.length - 1);
			this.numStudentsInProfile = Arrays.stream(numStudentsByRank).sum();
		}

		@Override
		public int numAgentsWithRank(int rank)
		{
			Assert.that(0 <= rank).orThrowMessage("Rank out of bounds");
			if (rank >= numStudentsByRank.length) {
				return 0;
			}

			return numStudentsByRank[rank];
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
		public String toString()
		{
			
			return Arrays.stream(numStudentsByRank)
			             .skip(1) // skip element at index 0
			             .mapToObj(String::valueOf)
			             .collect(Collectors.joining(" ", "[", "]"));
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (!(o instanceof ArrayBased))
				return false;
			ArrayBased simple = (ArrayBased) o;
			return Arrays.equals(numStudentsByRank, simple.numStudentsByRank);
		}
		
		@Override
		public int hashCode()
		{
			return Arrays.hashCode(numStudentsByRank);
		}
	}
	
	class ProfileDelta extends ArrayBased
	{
		private ProfileDelta(int[] numStudentsByRank)
		{
			super(numStudentsByRank);
		}
		
		@Override
		public String toString()
		{
			Function<Integer, String> int2string = value -> {
				if (value == 0) return " " + value;
				else if (value < 0) return Integer.toString(value);
				else return "+" + value;
			};
			
			return Arrays.stream(numStudentsByRank)
			             .skip(1) // skip element at index 0
			             .mapToObj(int2string::apply)
			             .collect(Collectors.joining(" ", "[", "]"));
		}
	}
}
