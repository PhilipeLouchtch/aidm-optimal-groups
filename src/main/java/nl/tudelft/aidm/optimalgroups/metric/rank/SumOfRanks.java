package nl.tudelft.aidm.optimalgroups.metric.rank;

import nl.tudelft.aidm.optimalgroups.model.HasProjectPrefs;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.RankInPref;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.UnacceptableAlternativeRank;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.Objects;
import java.util.function.Function;

public final class SumOfRanks
{
	private final Matching<? extends HasProjectPrefs, Project> matching;
	private final RankToWeightFn rankToWeightFn;
	
	public SumOfRanks(Matching<? extends HasProjectPrefs, Project> matching)
	{
		this(matching, RankToWeightFn.simple(r -> r));
	}
	
	public static SumOfRanks of(Matching<? extends HasProjectPrefs, Project> matching)
	{
		return new SumOfRanks(matching);
	}
	
	public SumOfRanks(Matching<? extends HasProjectPrefs, Project> matching, RankToWeightFn rankToWeightFn)
	{
		this.matching = matching;
		this.rankToWeightFn = rankToWeightFn;
	}
	
	public int asInt()
	{
		return matching.asList().stream()
				.map(match -> match.from().projectPreference().rankOf(match.to()))
				.mapToInt(rankToWeightFn::weightOf)
				.sum();
	}
	
	public Matching<? extends HasProjectPrefs, Project> matching()
	{
		return matching;
	}
	
	
	public interface RankToWeightFn
	{
		Integer weightOf(RankInPref rankInPref);
		
		static RankToWeightFn simple(Function<Integer, Integer> numericalRanksOnly)
		{
			return rankInPref -> {
				if (rankInPref instanceof UnacceptableAlternativeRank) return Integer.MAX_VALUE;
				if (rankInPref.isCompletelyIndifferent()) return 0;
				return numericalRanksOnly.apply(rankInPref.asInt());
			};
		}
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		var that = (SumOfRanks) obj;
		return Objects.equals(this.matching, that.matching);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(matching);
	}
	
	@Override
	public String toString()
	{
		return "SumOfRanks[" +
				"matching=" + matching + ']';
	}
	
}
