package nl.tudelft.aidm.optimalgroups.experiment.paper.runtime;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.ExponentiallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.UniformProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.pref.base.ListBasedProjectPreferences;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.jetbrains.annotations.NotNull;
import plouchtch.assertion.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RuntimeExperiments
{
	static String fileNameResults = "reports/" + "runtimes_big" + ".csv";
	
	static List<Integer> studentCounts = List.of(80, 160, 240, 320, 400, 480, 560, 640, 720, 800);
	static List<Integer> projectCounts = List.of(5, 10, 20, 40, 60, 80, 100, 120, 140, 160, 180);
	static List<Integer> slotCounts = List.of(1);//, 2, 3, 4, 5);
	
	public static void main(String[] args)
	{
		// skeleton for experiment
		
		// Need to evaluate each mechanism. Each mechanism has 4 variants,
		// assuming only one obj function. The variants consist of the grouping
		// type choice, which is:
		//  - none
		//  - hard
		//  - soft
		//  - conditional
		
		// Mechanisms
		var bepsys = new GroupProjectAlgorithm.BepSys_reworked();
		var normal_none = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.none());
		var fairness_none = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.none());
		
		var mechanisms = List.of(bepsys, normal_none, fairness_none);
		
		// create datasets
		// vary:
		//  - #students
		//  - #projects
		// under the preference types of:
		//  *singleton     *(sligtly perturbed singleton pref)      *chaotic/random
		//       |-------------|--------------------------------------------|
		
		var gsc = new GroupSizeConstraint.Manual(4, 5);

		var expResults = new LinkedList<ExpResult>();
		
		for (Integer numStudents : studentCounts)
		{
			for (Integer numProjects : projectCounts)
			{
				for (Integer numSlotsPerProject : slotCounts)
				{
					var numStudentsSupported = numProjects * numSlotsPerProject * gsc.maxSize();
					if (numStudents > numStudentsSupported) continue;
					
					var projects = Projects.generated(numProjects, numSlotsPerProject);
					
					// generate the right 'type' of project preferences
					// todo: would be nice to have outside the loop
					var prefGenerators = List.of(
							new NamedPrefGenerator("singleton", () -> new ListBasedProjectPreferences(new ArrayList<>(projects.asCollection()))), // flat linear
							new NamedPrefGenerator("linear_perturbed_1", new ExponentiallyDistributedProjectPreferencesGenerator(projects, 1)), // some perturbation
							new NamedPrefGenerator("linear_perturbed_4", new ExponentiallyDistributedProjectPreferencesGenerator(projects, 4)), // more chaos
							new NamedPrefGenerator("random", new UniformProjectPreferencesGenerator(projects)) // random
					);
					
					for (var prefGenType : prefGenerators)
					{
						var pregroupGen = new NamedPregroupingGenerator(PregroupingGenerator.none(), "none");
						
						// make dataset
						var datasetParams = new DatasetParams(numStudents, projects, numSlotsPerProject, gsc, prefGenType, pregroupGen);
						
//						AvgPreferenceRankOfProjects.ofAgentsInDatasetContext(dataset).displayChart();
						
						var newResults = experiment(mechanisms, datasetParams);
						expResults.addAll(newResults);
					}
				}
			}
		}
		
		// export results into csv
		exportResults(expResults);
		
		return;
	}
	
	static Collection<ExpResult> experiment(Collection<? extends GroupProjectAlgorithm> mechanisms, DatasetParams datasetParams)
	{
		System.out.print("Exp run for: " + datasetParams.toString() + "\n");
		
		final var numWarmupRuns = 1;
		final var numTrials = 5;
		final var numRunsPerTrial = 3;
		
		var results = new ArrayList<ExpResult>(numTrials * mechanisms.size());
		
		// pregenerate datasets so that each mechanism is run with exactly the same datasets
		record TrialRun(DatasetContext dataset, Integer trialRunNum) {}
		var trials = IntStream.range(0, numTrials).mapToObj(trialRunNum -> new TrialRun(datasetParams.intoNewlyGeneratedDataset(), trialRunNum)).toList();
		
		for (GroupProjectAlgorithm mechanism : mechanisms)
		{
			System.out.printf("  %s = ", mechanism.name());
			
			// warmup run x times
			for (int i = 0; i < numWarmupRuns; i++)
			{
				var datasetContext = datasetParams.intoNewlyGeneratedDataset();
				
				System.out.print("w");
				mechanism.determineMatching(datasetContext);
				System.out.print("|");
			}
			
			for (var trial : trials)
			{
				for (int i = 0; i < numRunsPerTrial; i++)
				{
					System.out.print("-");
					
					var instantStartRun = Instant.now();
					var matching = mechanism.determineMatching(trial.dataset());
					var instantEndRun = Instant.now();
					
					// todo: get better validation
					Assert.that(trial.dataset().allAgents().count() == AgentToProjectMatching.from(matching).countDistinctStudents())
							.orThrowMessage("Invalid result");
					
					var runtime = Duration.between(instantStartRun, instantEndRun).abs();
					
					var expResult = new ExpResult(datasetParams, mechanism, runtime, trial.trialRunNum());
					results.add(expResult);
					
					System.out.print("\b.");
				}
				
				// Progress bar per trial etc
				for (int i = 0; i < numRunsPerTrial; i++) System.out.print("\b");
				System.out.print("t|");
			}
			System.out.println();
		}
		
		return results;
	}
	
	record ExpResult(DatasetParams datasetParams, GroupProjectAlgorithm mechanism, Duration runDuration, Integer trialRunNum) {}
	
		static void exportResults(Collection<ExpResult> results)
	{
		var file = new File(fileNameResults);
		try (var writer = new PrintWriter(new BufferedWriter(new FileWriter(file))))
		{
			var cols = List.of("num_students", "num_projects", "num_slots", "proj_pref_type", "pregroup_type", "mechanism", "duration_ms", "trial").toArray(String[]::new);
			var format = Arrays.stream(cols).collect(Collectors.joining(",", "", "\n"));
			writer.printf(format, cols);
			
			for (ExpResult result : results)
			{
				writer.printf(format,
						result.datasetParams().numStudents(),
						result.datasetParams().projects().count(),
						result.datasetParams().numSlotsPerProj(),
						result.datasetParams().prefGenerator().shortName(),
						result.datasetParams().pregroupingGenerator().name(),
						result.mechanism().name(),
						result.runDuration().toMillis(),
						result.trialRunNum()
					);
			}
			
			writer.flush();
		}
		catch (Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}
	
	record NamedPregroupingGenerator(PregroupingGenerator generator, String name)
	{ }
	
	record NamedPrefGenerator(String shortName, ProjectPreferenceGenerator generator)
	{ }
	
	record DatasetParams(Integer numStudents, Projects projects, Integer numSlotsPerProj, GroupSizeConstraint gsc, NamedPrefGenerator prefGenerator, NamedPregroupingGenerator pregroupingGenerator)
	{
		public DatasetContext intoNewlyGeneratedDataset()
		{
			final var prefGenerator = this.prefGenerator.generator();
			return new GeneratedDataContext(numStudents, projects, gsc, prefGenerator, pregroupingGenerator.generator());
		}
		
		@Override
		public String toString()
		{
			return ("DatasetParams[ st#%s, pr#%s[%s], pp[%s], gp[%s] ]").formatted(
						numStudents,
						projects.count(),
						numSlotsPerProj,
						prefGenerator.shortName(),
						pregroupingGenerator().name()
					);
		}
	}
}
