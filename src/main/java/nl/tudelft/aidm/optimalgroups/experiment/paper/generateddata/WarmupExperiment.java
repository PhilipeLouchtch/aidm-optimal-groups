package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.*;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WarmupExperiment extends GeneratedDataExperiment
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
	
		var datasetParamCombinations = new ArrayList<DatasetParams>(
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
			
			var paramsForDataset = new DatasetParams(num_students, num_projects, num_slots, gsc, projPrefsGen, pregroupingGen);
			
			datasetParamCombinations.add(paramsForDataset);
		}
		
		var group = new GroupedDatasetParams.Group("warmup_all", datasetParamCombinations);
		return new GroupedDatasetParams(List.of(group));
	}
	
	public WarmupExperiment(List<GroupProjectAlgorithm> algos)
	{
		super(String.valueOf(Instant.now().toEpochMilli()), paramsForExperiments(), algos, 5, 5);
	}
	
	
	@Override
	protected ExperimentResultsCollector newExperimentResultsFile(String filePath)
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
		};
	}
}
