package nl.tudelft.aidm.optimalgroups.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileTest
{
	Profile empty;
	Profile simple;
	
	@BeforeEach
	void setUp()
	{
		empty = Profile.fromZeroIndexed();
		simple = Profile.fromZeroIndexed(1, 2, 3, 4, 5);
	}
	
	@Test
	void maxRank()
	{
		assertEquals(0, empty.maxRank());
		assertEquals(5, simple.maxRank());
	}

	@Test
	void minus() {
		var lhs = empty;
		var rhs = simple;

		// empty - simple = -simple
		Profile.ProfileDelta delta = empty.minus(simple);

		assertArrayEquals(new int[] { -1, -2, -3, -4, -5 }, delta.asZeroIndexed());
	}
}