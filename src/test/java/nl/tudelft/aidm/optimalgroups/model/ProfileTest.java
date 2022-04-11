package nl.tudelft.aidm.optimalgroups.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileTest
{
	Profile simpleEmpty;
	Profile simpleManyLong;
	
	@BeforeEach
	void setUp()
	{
		simpleEmpty = new Profile.Simple(new int[0]);
		simpleManyLong = new Profile.Simple(new int[] {0, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5});
	}
	
	@Test
	void maxRank()
	{
		assertEquals(0, simpleEmpty.maxRank());
		assertEquals(13, simpleManyLong.maxRank());
	}
}