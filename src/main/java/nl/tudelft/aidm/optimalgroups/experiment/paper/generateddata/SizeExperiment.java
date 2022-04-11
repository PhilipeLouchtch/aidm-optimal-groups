package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.*;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import plouchtch.lang.exception.ImplementMe;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SizeExperiment extends GeneratedDataExperiment
{
	private static GroupedDatasetParams paramsForExperiments()
	{
		List<Integer> nums_students = List.of(80, 160, 240, 320, 400, 480, 560, 640, 720, 800);
		List<Integer> nums_projects = List.of(5, 10, 20, 40, 60, 80, 100, 120, 140, 160, 180);
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
		
		var paramGroups = new ArrayList<GroupedDatasetParams.Group>();
		
		for (var num_students : nums_students)
		{
			var datasetParamGroup = new ArrayList<DatasetParams>(nums_projects.size() * nums_slots.size() * gscs.size() * projectPrefGenerators.size());
			
			for (var num_projects : nums_projects)
			for (var num_slots : nums_slots)
			for (var gsc : gscs)
			for (var projPrefsGen : projectPrefGenerators)
			{
				// Don't make infeasible params
				var numStudentsSupported = num_projects * num_slots * gsc.maxSize();
				if (num_students > numStudentsSupported)
					continue;
					
					var paramsForDataset = new DatasetParams(num_students, num_projects, num_slots, gsc, projPrefsGen, pregroupingGen);
					
					datasetParamGroup.add(paramsForDataset);
			}
			
			paramGroups.add(new GroupedDatasetParams.Group("stud[%s]".formatted(num_students), datasetParamGroup));
		}
		
		return new GroupedDatasetParams(paramGroups);
	}
	
	
	public SizeExperiment(String identifier, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		super(identifier, paramsForExperiments(), algos, numToGenPerParam, runs);
	}
	
	
	@Override
	protected ExperimentResultsCollector newExperimentResultsFile(String filePath)
	{
		return new ExperimentResultsFile(filePath);
	}
}
