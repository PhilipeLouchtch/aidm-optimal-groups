package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface ExperimentSubResult
{
//	DatasetParams params();
//
//	GroupProjectAlgorithm mechanism();
//	GroupToProjectMatching<?> matching();
//
//	Duration runtime();
//	Integer trialRunNum();
//
//	Profile profileAllStudents();
//	Profile profileSingles();
//	Profile profilePregrouped();
//	Profile profileUnsatpregroup();
	
	List<Object> columnHeaders();
	List<Object> columnValues();
	
	static String serializeProfile(Profile profile)
	{
		var profileAsArray = new Integer[profile.maxRank()];
		
		for (int rank = 1; rank <= profile.maxRank(); rank++)
		{
			profileAsArray[rank-1] = profile.numAgentsWithRank(rank);
		}
		
		return Arrays.stream(profileAsArray)
		             .map(i -> i == null ? 0 : i)
		             .map(Object::toString)
		             .collect(Collectors.joining("|"));
	}
}
