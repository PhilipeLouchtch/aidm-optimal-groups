package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.time.Duration;

public record ExperimentSubResultForNoPregroupings(DatasetParams params, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum) implements ExperimentSubResult
{
	@Override
	public Profile profileAllStudents()
	{
		return Profile.of(
				AgentToProjectMatching.from(this.matching())
		);
	}
	
	@Override
	public Profile profileSingles()
	{
		return Profile.fromProfileArray();
	}
	
	@Override
	public Profile profilePregrouped()
	{
		return Profile.fromProfileArray();
	}
	
	@Override
	public Profile profileUnsatpregroup()
	{
		return Profile.fromProfileArray();
	}
}
