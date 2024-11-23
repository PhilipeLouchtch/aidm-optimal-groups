package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.model;

import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public final class PregroupingDistribution
{
	private final Groups<?> pregroupings;
	private final int maxAllowedGroupSize;
	
	private PregroupingDistribution(Groups<?> pregroupings, int maxAllowedGroupSize)
	{
		this.pregroupings = pregroupings;
		this.maxAllowedGroupSize = maxAllowedGroupSize;
	}
	
	public static PregroupingDistribution from(Groups<?> pregroupings, int maxAllowedGroupSize)
	{
		return new PregroupingDistribution(pregroupings, maxAllowedGroupSize);
	}
	
	/**
	 * @return a |-delimited string of the number of students per pregrouping size. Basically a histogram
	 * of the number of students per size of pregrouping. </p>
	 * For example: if there are 3x pregroupings of size 2, and 3 of size 4, then the resulting string is "0|6|0|12"
	 */
	@NotNull
	public String ofStudentsAsString()
	{
		return IntStream.rangeClosed(2, maxAllowedGroupSize)
				       .map(i -> i * pregroupings.ofSize(i).count())
				       .mapToObj(Integer::toString)
				       .collect(joining("|"));
	}
}
