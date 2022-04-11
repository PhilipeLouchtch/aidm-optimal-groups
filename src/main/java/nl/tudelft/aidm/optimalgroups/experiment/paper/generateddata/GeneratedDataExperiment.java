package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResultsCollector;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResultForNoPregroupings;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.GroupedDatasetParams;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import plouchtch.assertion.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

/**
 * An experiment consisting of multiple datasets that are batched together as a single experiment.
 * Created for the purpose of bundling together experiments of generated datasets, more specifically,
 * generated datasets that are generated from the same parameters.
 */
public abstract class GeneratedDataExperiment
{
	private static final String experimentDataLocation = "results/thesis/";
	
	protected final String identifier;
	
	protected final GroupedDatasetParams groupedDatasetParams;
	protected final List<GroupProjectAlgorithm> algos;
	
	private final int numToGenPerParam;
	private final int runs;
	
	
	// TODO? give <list of params> for data generation, generate data here. An experiment is the dataset space...
	public GeneratedDataExperiment(String identifier, GroupedDatasetParams groupedDatasetParams, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		this.identifier = identifier;
		this.groupedDatasetParams = groupedDatasetParams;
		
		this.algos = algos;
		
		this.numToGenPerParam = numToGenPerParam;
		this.runs = runs;
	}
	
	
	public final void run()
	{
		this.generateAndWriteResults();
	}
	
	// SKIP
//	private /*List<ExperimentAlgorithmSubresult>*/ void generateAndWriteResults()
//	{
//		// If the file doesn't exist, or this is a "warmup the javaVM" experiment -> exec the experiment
//		if (this instanceof WarmupExperiment || !file.exists())
//		{
//			var results = generateResults();
//
//			// write results to file
//			writeToFile(file, results);
//		}
//
//		// why read????
//		return /* readFile */;
//	}
	
	protected String resultsFileName(String suffix)
	{
		return experimentDataLocation + identifier + "_%s".formatted(suffix) + ".csv";
	}
	
	abstract protected ExperimentResultsCollector newExperimentResultsFile(String filePath);
	
	// MAIN FN - make choices here
	private void generateAndWriteResults()
	{
		System.out.printf("\n\nExperiment: %s", identifier);
		
		for (GroupedDatasetParams.Group paramGroup : groupedDatasetParams.groups())
		{
			var resultsCollector = newExperimentResultsFile(resultsFileName(paramGroup.groupIdentifier()));
			
			if (resultsCollector.resultsCollectionCanBeSkipped())
				// Ok, next group
				continue;
			
			for (var datasetParams : paramGroup.asList())
			{
				System.out.print("\n  exp run for: " + datasetParams.toString());
				for (int j = 0; j < numToGenPerParam; j++)
				{
					var datasets = IntStream.rangeClosed(1, numToGenPerParam)
					                        .mapToObj(__ -> datasetParams.intoNewlyGeneratedDataset())
					                        .toList();
					
					for (GroupProjectAlgorithm algo : algos)
					{
						System.out.printf("\n  - %s = ", algo.name());
						
						for (DatasetContext dataset : datasets)
						{
							for (int run_no = 0; run_no < runs; run_no++)
							{
								System.out.print("-"); // running
								
								var start = Instant.now();
								var matching = algo.determineMatching(dataset);
								var end = Instant.now();
								
								Assert.that(dataset.allAgents()
								                   .count() == AgentToProjectMatching.from(matching)
								                                                     .countDistinctStudents())
								      .orThrowMessage("Invalid result");
								
								System.out.print("\b."); // done
								
								var runtime = Duration.between(start, end)
								                      .abs();
								
								// todo
								var result = new ExperimentSubResultForNoPregroupings(datasetParams, algo, matching, runtime, run_no);
								
								resultsCollector.add(result);
							}
						}
						
					} // algos
					
				} // Genned dataset
				
			} // single dataset param
			
		} // group
	}
	
}
