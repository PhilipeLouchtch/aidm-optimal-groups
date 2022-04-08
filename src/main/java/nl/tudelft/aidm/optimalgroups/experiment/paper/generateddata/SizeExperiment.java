package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.DatasetParams;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResult;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.NamedPrefGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.NamedPregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SizeExperiment extends GeneratedDataExperiment
{
	private static List<DatasetParams> paramsForExperiments()
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
		
		return datasetParamCombinations;
	}
	
	public SizeExperiment(String identifier, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		super(identifier, paramsForExperiments(), algos, numToGenPerParam, runs);
	}
	
	@Override
	protected void exportData(PrintWriter writer, List<ExperimentResult> results) throws IOException
	{
		
		// metrics: only rank profile, generate metrics in R
		// runtime: do record runtime duration
		
		var cols = List.of(
				"num_students", "num_projects", "num_slots",
				
				"proj_pref_type", "pregroup_type", "mechanism", "trial",
				
				"duration_ms",
				
				"profile_all", "profile_singles", "profile_pregrouped", "profile_unsatpregroup"
			);
		
		// A row of placeholders for printf of proper length
		var rowFormat = IntStream.range(0, cols.size()).mapToObj(__ -> "%s")
				.collect(Collectors.joining(",", "", "\n"));
		
		writer.printf(rowFormat, cols);
		
		for (var result : results)
		{
			// TODO: Need 4 variants,
			//  - all, individuals, grouped, grouped-unsatisfied
			// but for now, the size experiment only has one variant: all
			var profileOfAllStudentRanks = Profile.of(AgentToProjectMatching.from(result.matching()));
			
			writer.printf(rowFormat,
					result.params().numStudents(),
					result.params().numProjects(),
					result.params().numSlotsPerProj(),
					
					result.params().prefGenerator().shortName(),
					result.params().pregroupingGenerator().shortName(),
					result.mechanism().name(),
					result.trialRunNum(),
					
					result.runtime().toMillis(),
					serializeProfile(profileOfAllStudentRanks),
					serializeProfile(Profile.fromProfileArray()),
					serializeProfile(Profile.fromProfileArray()),
					serializeProfile(Profile.fromProfileArray())
				);
			
			writer.flush();
		}
	}
	
}
