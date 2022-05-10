package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.*;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WarmupExperiment extends GeneratedDataExperiment<SimpleDatasetParams>
{
	private static GroupedDatasetParams paramsForExperiments()
	{
		List<Integer> nums_students = List.of(80);
		List<Integer> nums_projects = List.of(20);
		List<Integer> nums_slots = List.of(1);
		
		List<NamedPrefGenerator> projectPrefGenerators = List.of(
				ProjPrefVariations.singleton(),
				ProjPrefVariations.linearPerturbedSlightly(),
				ProjPrefVariations.linearPerturbedMore(),
				ProjPrefVariations.random(),
				ProjPrefVariations.realistic()
		);
		
		List<GroupSizeConstraint> gscs = List.of(
				GroupSizeConstraint.manual(4, 5)
		);
	
		var datasetParamCombinations = new ArrayList<SimpleDatasetParams>(
				nums_students.size() * nums_projects.size() * nums_slots.size()
				* projectPrefGenerators.size() * gscs.size()
		);
		
		var pregroupingGen = new NamedPregroupingGenerator(PregroupingGenerator.none(), "none");
		
		for (var num_students : nums_students)
		for (var num_projects : nums_projects)
		for (var num_slots : nums_slots)
		for (var gsc : gscs)
		for (var projPrefsGen : projectPrefGenerators)
		{
			// Don't make infeasible params
			var numStudentsSupported = num_projects * num_slots * gsc.maxSize();
			if (num_students > numStudentsSupported) continue;
			
			var paramsForDataset = new SimpleDatasetParams(num_students, num_projects, num_slots, gsc, projPrefsGen, pregroupingGen);
			
			datasetParamCombinations.add(paramsForDataset);
		}
		
		var group = new GroupedDatasetParams.Group("warmup_all", datasetParamCombinations);
		return new GroupedDatasetParams(List.of(group));
	}
	
	public WarmupExperiment(List<GroupProjectAlgorithm> algos)
	{
		super(String.valueOf("Warmup @" + Instant.now().toEpochMilli()), paramsForExperiments(), algos, 5, 5);
	}
	
	
	@Override
	protected ExperimentResultsCollector newExperimentResultsCollector(String filePath)
	{
		// "ExperimentResultsBlackhole"
		return new ExperimentResultsCollector()
		{
			@Override
			public boolean resultsCollectionCanBeSkipped()
			{
				return false;
			}
			
			@Override
			public void add(ExperimentSubResult subResult)
			{
				// ignore
			}
			
			@Override
			public void close() throws Exception
			{
				// nothing
			}
		};
	}
	
	@Override
	protected ExperimentSubResult newExperimentSubResult(SimpleDatasetParams params, DatasetContext datasetContext, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum)
	{
		return new SizeExperimentSubResult(params, mechanism, matching, runtime, trialRunNum);
	}
}
