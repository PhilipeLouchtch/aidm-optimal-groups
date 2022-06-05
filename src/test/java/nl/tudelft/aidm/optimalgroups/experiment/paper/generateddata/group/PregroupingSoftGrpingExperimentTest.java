package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PregroupingSoftGrpingExperimentTest
{
	@Test
	void name()
	{
		assertEquals(new PregroupingSoftGrpingExperiment.Proportion(0.1).asString(), "10 %");
		assertEquals(new PregroupingSoftGrpingExperiment.Proportion(0.2).asString(), "20 %");
		assertEquals(new PregroupingSoftGrpingExperiment.Proportion(0.6).asString(), "60 %");
		assertEquals(new PregroupingSoftGrpingExperiment.Proportion(1.0).asString(), "100 %");
	}
}