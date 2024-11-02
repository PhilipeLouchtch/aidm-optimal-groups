package nl.tudelft.aidm.optimalgroups.model.comparison;

import nl.tudelft.aidm.optimalgroups.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParetoComparatorTest
{
	public static final ParetoComparator comparator = new ParetoComparator();

	@Test
	void test_paretoComparison_profilesSame()
	{
		var profile = Profile.fromZeroIndexed(1,4,3,2);
		var another = Profile.fromZeroIndexed(1,4,3,2);

		assertEquals(ParetoComparator.ParetoOutcome.SAME, comparator.compare(profile, profile));

		assertNotSame(profile, another);
		assertEquals(ParetoComparator.ParetoOutcome.SAME, comparator.compare(another, profile));
	}
	
	@Test
	void test_paretoComparison_profilesNotPareto()
	{
		var profile = Profile.fromZeroIndexed(1,4,3,2);
		var other = Profile.fromZeroIndexed(1,3,5,1);
		
		assertEquals(ParetoComparator.ParetoOutcome.NONE, comparator.compare(profile, other));
		assertEquals(ParetoComparator.ParetoOutcome.NONE, comparator.compare(other, profile));
	}
	
	@Test
	void test_paretoComparison_profilesPareto()
	{
		var profile = Profile.fromZeroIndexed(1,5,8,10);
		var other = Profile.fromZeroIndexed(1,4,9,10);
		
		assertEquals(ParetoComparator.ParetoOutcome.BETTER, comparator.compare(profile, other));
		assertEquals(ParetoComparator.ParetoOutcome.WORSE, comparator.compare(other, profile));
	}
	
	@Test
	void test_paretoComparison_profilesNotPareto2()
	{
		var profile = Profile.fromZeroIndexed(1,5,7,11);
		var other = Profile.fromZeroIndexed(1,4,9,10);
		
		var betterOrSame = comparator.isParetoBetterOrEqual(profile, other);
		
		assertFalse(betterOrSame);
	}
}