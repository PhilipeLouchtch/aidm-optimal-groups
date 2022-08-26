package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model;


import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * A Groups type containing only groups that are pregroups. Thus are groups of students
 * who are grouped a-priori together. Groups need not be valid (size-wise) and contain only
 * students that want to be "together".
 *
 * If a dataset contains 10 students, a valid group size is only 5 and of the 10 students, the first 6 express to be together
 * in the following manner: (1st with 2nd, 3rd with 4th and 5th with 6), then the following are considered pregroups: { {s1, s2}, {s3, s4}, {s5, s6} }
 */
public class PregroupGroups<G extends Group> extends Groups.ListBackedImpl<G>
{
	public PregroupGroups(List<G> asList)
	{
		super(asList);
	}
	
	/**
	 * Filter groups to those only of the given sizes
	 * @param sizes The acceptible sizes of groups
	 * @return Groups of given sizes
	 */
	@Override
	public Groups<G> ofSizes(Integer... sizes)
	{
		var sizesDedup = new HashSet<>(List.of(sizes));
		
		var list = new ArrayList<G>();
		for (int size : sizesDedup)
		{
			list.addAll(this.ofSize(size).asCollection());
		}
		
		return new PregroupGroups<>(list);
	}
	
	public PregroupGroups<MatchedGroup> ofWhichSatisfiedIn(GroupToProjectMatching<?> matching)
	{
		var asList = new LinkedList<MatchedGroup>();
		for (G pregroup : this.asCollection())
		{
			var match = matching.asList().stream()
			                 .filter(m -> m.from().contains(pregroup))
	                         .findFirst();
			
			match.ifPresent(m -> asList.add(new MatchedGroup(pregroup, m.to())));
		}
		
		
		return new PregroupGroups<>(asList);
	}
	
//	public PregroupGroups ofWhichUnsatisfied(GroupToProjectMatching<?> matching)
//	{
//
//	}
	
	public static class MatchedGroup extends Group.TentativeGroup
	{
		public MatchedGroup(Group group, Project match)
		{
			super(group);
		}
	}
	
	
}
