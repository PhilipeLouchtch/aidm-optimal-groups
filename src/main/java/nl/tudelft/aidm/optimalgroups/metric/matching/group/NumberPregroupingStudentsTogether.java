package nl.tudelft.aidm.optimalgroups.metric.matching.group;

import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

public class NumberPregroupingStudentsTogether
{
	private final int asInt;
	
	public NumberPregroupingStudentsTogether(GroupToProjectMatching<?> matching, Groups<?> proposedGroups)
	{
		var numStudentsTogether = proposedGroups.asCollection().stream()
				.filter(proposed -> isTogether(proposed, matching))
				.mapToInt(proposed -> proposed.members().count())
				.sum();
		
		this.asInt = (int) numStudentsTogether;
	}
	
	private static Boolean isTogether(Group proposed, GroupToProjectMatching<? extends Group> matching)
	{
		return matching.asList().stream().anyMatch(actual -> actual.from().members().containsAll(proposed.members()));
	}
	
	public int asInt()
	{
		return this.asInt;
	}
}
