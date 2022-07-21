package nl.tudelft.aidm.optimalgroups.metric.group;

import nl.tudelft.aidm.optimalgroups.model.group.Groups;

import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public record NumStudentsPerGroupSizeDist(Groups<?> pregroupings, int maxAllowedGroupSize)
{
	public String asString()
	{
		return IntStream.rangeClosed(2, maxAllowedGroupSize)
		         .mapToObj(i -> Integer.toString(pregroupings.ofSize(i).count() * i))
		         .collect(joining("|"));
	}
}
