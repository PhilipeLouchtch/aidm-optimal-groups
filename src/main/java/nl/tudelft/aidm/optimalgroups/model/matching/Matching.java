package nl.tudelft.aidm.optimalgroups.model.matching;

import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.List;

/**
 * The result of a matchings algorithm
 */
public interface Matching<FROM, TO>
{
	/**
	 * The matchings as a list representation
	 * @return An unmodifiable list
	 */
	List<Match<FROM, TO>> asList();

	/**
	 * The dataset the matching was created from
	 * @return The source dataset
	 */
	DatasetContext datasetContext();
	
	/**
	 * @param from
	 * @return  All matches that are matched to the given FROM
	 */
	default List<Match<FROM, TO>> allMatchesFrom(FROM from)
	{
		return this.asList().stream().filter(match -> match.from().equals(from))
				.toList();
	}
	
	/**
	 * @param to
	 * @return All matches that are matched to the given TO
	 */
	default List<Match<FROM, TO>> allTo(TO to)
	{
		return this.asList().stream().filter(match -> match.to().equals(to))
				.toList();
	}
	
	
}
