package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.experiment.paper.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.*;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
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
public abstract class GeneratedDataExperiment<DATASET_PARAMS extends DatasetParams> implements Experiment
{
	private static final String experimentDataLocation = "results/thesis/";
	
	protected final String identifier;
	
	protected final GroupedDatasetParams<DATASET_PARAMS> groupedDatasetParams;
	protected final List<GroupProjectAlgorithm> algos;
	
	private final int numToGenPerParam;
	private final int runs;
	
	
	public GeneratedDataExperiment(String identifier, GroupedDatasetParams<DATASET_PARAMS> groupedDatasetParams, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		this.identifier = identifier;
		this.groupedDatasetParams = groupedDatasetParams;
		
		this.algos = algos;
		
		this.numToGenPerParam = numToGenPerParam;
		this.runs = runs;
	}
	
	@Override
	public final void run()
	{
		this.generateAndWriteResults();
	}
	
	protected String resultsFileName(String suffix)
	{
		return experimentDataLocation + identifier + "_%s".formatted(suffix) + ".csv";
	}
	
	abstract protected ExperimentResultsCollector newExperimentResultsCollector(String filePath);
	
	abstract protected ExperimentSubResult newExperimentSubResult(DATASET_PARAMS params, DatasetContext datasetContext, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum);
	
	// MAIN FN - make choices here
	private void generateAndWriteResults()
	{
		System.out.printf("\n\nExperiment: %s", identifier);
		
		for (var paramGroup : groupedDatasetParams.groups())
		{
			try (var resultsCollector = newExperimentResultsCollector(resultsFileName(paramGroup.groupIdentifier())))
			{
				if (resultsCollector.resultsCollectionCanBeSkipped())
					// Ok, this one has been done already
					continue;
				
				for (var datasetParams : paramGroup.asList())
				{
					System.out.print("\n  exp run for: " + datasetParams.toString());
					
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
								
								Assert.that(dataset.allAgents().count() == AgentToProjectMatching.from(matching).countDistinctStudents())
								      .orThrowMessage("Invalid result");
								
								System.out.print("\b."); // done
								
								var runtime = Duration.between(start, end)
								                      .abs();
								
								var result = newExperimentSubResult(datasetParams, dataset, algo, matching, runtime, run_no);
								
								resultsCollector.add(result);
							}
						}
						
					} // algos
					
				} // single dataset param
			}
			catch (Exception ex) {
				// Needed to do a try-with-resources
				throw new RuntimeException(ex);
			}
			
		} // group
	}
	
}
