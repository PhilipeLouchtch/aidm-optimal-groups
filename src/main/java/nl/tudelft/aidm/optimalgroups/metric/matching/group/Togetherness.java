package nl.tudelft.aidm.optimalgroups.metric.matching.group;

import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;

public record Togetherness(Integer numGroups, Integer numStudents)
{
	public static Togetherness from(GroupToProjectMatching<?> matching, Groups<?> proposedGroups)
	{
		// Note that an actual group may consist of one or more proposed groups
		var together = proposedGroups.asCollection().stream()
				.filter(proposed -> isTogether(proposed, matching))
                .toList();
		
		var numGroups = together.size();
		var numStudents = together.stream().mapToInt(group -> group.members().count()).sum();
		
		return new Togetherness(numGroups, numStudents);
	}
	
	private static Boolean isTogether(Group proposed, GroupToProjectMatching<? extends Group> matching)
	{
		return matching.asList().stream()
				       .map(Match::from)
				       .anyMatch(actual -> actual.contains(proposed));
	}
	
	@Override
	public String toString()
	{
		throw new RuntimeException("Not implemented");
	}
}
