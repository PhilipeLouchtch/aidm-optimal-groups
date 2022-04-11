package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.time.Duration;

public interface ExperimentSubResult
{
	DatasetParams params();
	
	GroupProjectAlgorithm mechanism();
	GroupToProjectMatching<?> matching();
	
	Duration runtime();
	Integer trialRunNum();
	
	Profile profileAllStudents();
	Profile profileSingles();
	Profile profilePregrouped();
	Profile profileUnsatpregroup();
}
