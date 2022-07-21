package nl.tudelft.aidm.optimalgroups.metric.group;

import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public record NumGroupsPerGroupSizeDist(Groups<?> pregroupings, int maxAllowedGroupSize)
{
	@Override
	public String toString()
	{
		return IntStream.rangeClosed(2, maxAllowedGroupSize)
		         .mapToObj(i -> Pair.of(i, Integer.toString(pregroupings.ofSize(i).count())))
		         .map(pair -> "%s: %s".formatted(pair.getKey(), pair.getValue()))
		         .collect(joining(", "));
	}
}