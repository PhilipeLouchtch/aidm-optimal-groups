package nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class GroupFactorization
{
	public record Factorization(boolean isFactorable, int[] numGroupsOfSize) {}

	private List<Factorization> isFactorable;
	private final GroupSizeConstraint gsc;


	// TODO: capacities for num groups
	// TODO: weak references solution if to be used in production environment
	public static Map<GroupSizeConstraint, GroupFactorization> sharedInstances = new HashMap<>();
	public static synchronized GroupFactorization cachedInstanceFor(GroupSizeConstraint groupSizeConstraint)
	{
		return sharedInstances.computeIfAbsent(groupSizeConstraint,
			key -> new GroupFactorization(groupSizeConstraint, 1000)
		);
	}


	/* CTOR */
	public GroupFactorization(GroupSizeConstraint gsc, int expectedStudentsMax)
	{
		this.gsc = gsc;

		this.isFactorable = makeFreshLookupList(expectedStudentsMax);

		Assert.that(gsc.maxSize() - gsc.minSize() == 1)
			.orThrowMessage("Fix group factorization to support delta != 1");
	}


	public Factorization forGivenNumberOfStudents(int numStudents) {
		isFactorableIntoValidGroups(numStudents);

		return isFactorable.get(numStudents);
	}
		
	public boolean isFactorableIntoValidGroups(int numStudents)
	{
		var numGroupsOfSize = new int[gsc.maxSize()+1];
		var remainingStudents = numStudents;
		
		int groupSize = gsc.maxSize();
		
		int res = 0;
		while (remainingStudents > 0)
		{
			if (res == -1) {
				if (numGroupsOfSize[groupSize] == 0) {
					if (groupSize == gsc.maxSize())
						return false; // can't go higher - inpossible instance
					groupSize++; // go higher level
					continue;
				}
				else {
					numGroupsOfSize[groupSize]--;
					remainingStudents += groupSize;
					groupSize--; // go down again
					res = 0;
				}
			}
			
			var maxGrpsOfCurrentSize = remainingStudents / groupSize;
			remainingStudents -= maxGrpsOfCurrentSize * groupSize;
			
			numGroupsOfSize[groupSize] += maxGrpsOfCurrentSize;
			
			if (remainingStudents > 0 && groupSize > gsc.minSize()) {
				groupSize--;
			}
			else if (remainingStudents > 0 && groupSize == gsc.minSize()) {
				// Could not partition all students over groups, must revise factors
				res = -1; // some higher sized group must disband
				
				// reset for this group size
				remainingStudents += numGroupsOfSize[groupSize] * groupSize;
				numGroupsOfSize[groupSize] = 0;
				
				// continue with larger sized groups next
				groupSize++;
			}
		}
		
		
		// If larger size is requested, expand list
		isFactorable = copyIntoResized(isFactorable, numStudents);
		
//		&& Arrays.stream(numGroupsOfSize).sum() <= numGroupsUpperbound
		if (remainingStudents == 0) {
			var factorization = new Factorization(true, numGroupsOfSize);
			isFactorable.set(numStudents, factorization);
			return true;
		}
		else {
			var factorization = new Factorization(false, new int[gsc.maxSize()+1]);
			isFactorable.set(numStudents, factorization);
			return false;
		}
	}


	/* HELPER FNS */
	private static List<Factorization> makeFreshLookupList(int upToIndexInclusive)
	{
		return makeLookupList(upToIndexInclusive, index -> null);
	}

	private static List<Factorization> copyIntoResized(List<Factorization> old, int upToIndexInclusive)
	{
		// Function ensures value in copy are same as 'old' and any higher indexed elements (= 'new' elements)
		// are set to 'null' (= unknown yet)
		Function<Integer, Factorization> indexToValueFn = index -> {
			var value = index < old.size()
				? old.get(index)
				: null;

			return value;
		};

		return makeLookupList(upToIndexInclusive, indexToValueFn);
	}

	private static List<Factorization> makeLookupList(int upToIndexInclusive, Function<Integer, Factorization> indexToValueFn)
	{
		var list = new ArrayList<Factorization>(upToIndexInclusive);
		for (int i = 0; i <= upToIndexInclusive; i++)
		{
			list.add(null);
		}

		int upToExclusive = upToIndexInclusive + 1;
		IntStream.range(0, upToExclusive).forEach(index -> list.set(index, indexToValueFn.apply(index)));

		return list;
	}
}
