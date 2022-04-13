package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult.*;

public record SizeExperimentSubResult(SimpleDatasetParams params, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum) implements ExperimentSubResult
{
	public Profile profileAllStudents()
	{
		return Profile.of(
				AgentToProjectMatching.from(this.matching())
		);
	}
	
	public Profile profileSingles()
	{
		return Profile.fromProfileArray();
	}
	
	public Profile profilePregrouped()
	{
		return Profile.fromProfileArray();
	}
	
	public Profile profileUnsatpregroup()
	{
		return Profile.fromProfileArray();
	}
	
	@Override
	public List<Object> columnHeaders()
	{
		return List.of(
			"num_students",
			"num_projects",
			"num_slots",
			
			"proj_pref_type",
			"pregroup_type",
			"mechanism",
			"trial",
			
			"duration_ms",
			
			"profile_all",
			"profile_singles",
			"profile_pregrouped",
			"profile_unsatpregroup"
		);
	}
	
	@Override
	public List<Object> columnValues()
	{
		return List.of(
				params().numStudents(),
				params().numProjects(),
				params().numSlotsPerProj(),
		
				params().prefGenerator().shortName(),
				params().pregroupingGenerator().shortName(),
				mechanism().name(),
				trialRunNum(),
		
				runtime().toMillis(),
		
				serializeProfile(profileAllStudents()),
				serializeProfile(profileSingles()),
				serializeProfile(profilePregrouped()),
				serializeProfile(profileUnsatpregroup())
		);
	}
}
