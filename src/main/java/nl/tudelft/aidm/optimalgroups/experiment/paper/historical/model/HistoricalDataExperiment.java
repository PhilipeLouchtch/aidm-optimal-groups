package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.model;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.experiment.paper.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResultsCollector;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult;
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
public abstract class HistoricalDataExperiment implements Experiment
{
	private static final String experimentDataLocation = "results/thesis/";
	
	protected final String identifier;
	
	protected final List<DatasetContext> problemInstances;
	protected final List<GroupProjectAlgorithm> algos;
	
	private final int runs;
	
	public HistoricalDataExperiment(String identifier, List<DatasetContext> problemInstances, List<GroupProjectAlgorithm> algos, int runs)
	{
		this.identifier = identifier;
		this.problemInstances = problemInstances;
		
		this.algos = algos;
		
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
	
	abstract protected ExperimentSubResult newExperimentSubResult(DatasetContext datasetContext, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum);
	
	// MAIN FN - make choices here
	private void generateAndWriteResults()
	{
		System.out.printf("\n\nExperiment: %s", identifier);
		
		try (var resultsCollector = newExperimentResultsCollector(resultsFileName("")))
		{
			
			for (var problemInstance : problemInstances)
			{
				System.out.print("\n  exp run for: " + problemInstance.toString());
				
				for (GroupProjectAlgorithm algo : algos)
				{
					System.out.printf("\n  - %s = ", algo.name());

					for (int run_no = 0; run_no < runs; run_no++)
					{
						System.out.print("-"); // running
						
						var start = Instant.now();
						var matching = algo.determineMatching(problemInstance);
						var end = Instant.now();
						
						Assert.that(problemInstance.allAgents().count() == AgentToProjectMatching.from(matching).countDistinctStudents())
						      .orThrowMessage("Invalid result");
						
						System.out.print("\b."); // done
						
						var runtime = Duration.between(start, end)
						                      .abs();
						
						var result = newExperimentSubResult(problemInstance, algo, matching, runtime, run_no);
						
						resultsCollector.add(result);
					}
					
				} // algos
				
			} // single dataset param
			
		} catch (Exception ex) {
			// Needed to do a try-with-resources
			throw new RuntimeException(ex);
		}
	}
	
}
