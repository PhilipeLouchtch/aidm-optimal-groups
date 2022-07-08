package nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class GroupFactorization
{
	public record Factorization(boolean isFactorable, int[] numGroupsOfSize) {}

	private List<Factorization> factorizations;
	private final GroupSizeConstraint gsc;
	
	
	// TODO: capacities for num groups
	// TODO: weak references solution if to be used in production environment
	public static Map<GroupSizeConstraint, GroupFactorization> sharedInstances = new HashMap<>();
	public static synchronized GroupFactorization cachedInstanceFor(GroupSizeConstraint groupSizeConstraint)
	{
		var x = sharedInstances.computeIfAbsent(groupSizeConstraint,
			gsc -> new GroupFactorization(gsc, 1000)
		);
		
		return x;
	}


	/* CTOR */
	public GroupFactorization(GroupSizeConstraint gsc, int expectedStudentsMax)
	{
		this.gsc = gsc;

		this.factorizations = makeFreshLookupList(expectedStudentsMax);
	}


	public Factorization forGivenNumberOfStudents(int numStudents) {
		isFactorableIntoValidGroups(numStudents);

		var factorization = factorizations.get(numStudents);
		
		// Bugfix: copy the factorization - the array is modifiable and is being modified by SetOfGroupSizesThatCanStillBeCreated
		// choosing to make a copy here than letting the users take care of it
		return new Factorization(factorization.isFactorable, factorization.numGroupsOfSize);
	}
		
	public boolean isFactorableIntoValidGroups(int numStudents)
	{
		// check cache
		var cached = factorizations.get(numStudents);
		if (cached != null)
			return cached.isFactorable;
		
		// catch an impossible instance:
		if (gsc.maxSize() - gsc.minSize() == 0 && numStudents % gsc.maxSize() > 0) {
			markAsNotFactorible(numStudents);
			return false;
		}
		
		var numGroupsPerSize = new int[gsc.maxSize()+1];
		var remainingStudents = numStudents;
		
		int groupSize = gsc.maxSize();
		
		int res = 0;
		while (remainingStudents > 0)
		{
			if (res == -1) {
				if (numGroupsPerSize[groupSize] == 0) {
					if (groupSize == gsc.maxSize())
						return false; // can't go higher - impossible instance
					groupSize++; // go higher level
					continue;
				}
				else {
					numGroupsPerSize[groupSize]--;
					remainingStudents += groupSize;
					groupSize--; // go down again
					res = 0;
				}
			}
			
			var maxGrpsOfCurrentSize = remainingStudents / groupSize;
			remainingStudents -= maxGrpsOfCurrentSize * groupSize;
			
			numGroupsPerSize[groupSize] += maxGrpsOfCurrentSize;
			
			if (remainingStudents > 0 && groupSize > gsc.minSize()) {
				groupSize--;
			}
			else if (remainingStudents > 0 && groupSize == gsc.minSize()) {
				// Could not partition all students over groups, must revise factors
				res = -1; // some higher sized group must disband
				
				// reset for this group size
				remainingStudents += numGroupsPerSize[groupSize] * groupSize;
				numGroupsPerSize[groupSize] = 0;
				
				// continue with larger sized groups next
				groupSize++;
			}
		}
		
		if (remainingStudents == 0) {
			markAsFactorizable(numStudents, numGroupsPerSize);
			return true;
		}
		else {
			markAsNotFactorible(numStudents);
			return false;
		}
	}
	
	private void markAsFactorizable(int numStudents, int[] factorization)
	{
		// If larger size is requested, expand list
		factorizations = copyIntoResized(factorizations, numStudents);
		
		var f = new Factorization(true, factorization);
		factorizations.set(numStudents, f);
	}
	
	private void markAsNotFactorible(int numStudents)
	{
		// If larger size is requested, expand list
		factorizations = copyIntoResized(factorizations, numStudents);
		
		var f = new Factorization(false, new int[gsc.maxSize()+1]);
		factorizations.set(numStudents, f);
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
