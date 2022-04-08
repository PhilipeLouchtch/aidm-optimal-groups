package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentAlgorithmSubresult;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.DatasetParams;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResult;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import org.apache.commons.lang3.tuple.Pair;
import plouchtch.assertion.Assert;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
	
	protected final List<DatasetParams> datasetParamsCollection;
	protected final List<GroupProjectAlgorithm> algos;
	
	private final int numToGenPerParam;
	private final int runs;
	
//	private final Lazy<List<ExperimentAlgorithmSubresult>> results;
	
	// TODO? give <list of params> for data generation, generate data here. An experiment is the dataset space...
	public GeneratedDataExperiment(String identifier, List<DatasetParams> datasetParams, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		this.identifier = identifier;
		this.datasetParamsCollection = datasetParams;
		
		this.algos = algos;
		
		this.numToGenPerParam = numToGenPerParam;
		this.runs = runs;
//		this.results = new Lazy<>(this::readOrGenerateResults);
	}
	
	/**
	 * Writes out the relevant data of the experiment to a file represented by the given FileWriter
	 *
	 * The implementer is responsible with extracting the necessary data from the DatasetGenParams,
	 * Algorithms and resulting matchings (collect your own metrics). The file format and everything else
	 * is also up to the implementer.
	 * @param writer The opened FileWrite to which the data must be written
	 * @param results
	 * @throws IOException Don't worry about handling exceptions
	 */
	protected abstract void exportData(PrintWriter writer, List<ExperimentResult> results) throws IOException;
	
	public final void run()
	{
		this.generateAndWriteResults();
		return;
	}
	
	protected String serializeProfile(Profile profile)
	{
		var profileAsArray = new Integer[profile.maxRank()+1];
		
		profile.forEach((rank, count) -> {
			profileAsArray[rank] = count;
		});
		
		return Arrays.stream(profileAsArray)
				.map(i -> i == null ? 0 : i).map(Object::toString)
				.collect(Collectors.joining("|"));
	}
	
	private /*List<ExperimentAlgorithmSubresult>*/ void generateAndWriteResults()
	{
		// just very simple and naive way of checking if experiment has already been done before
		var file = new File(experimentDataLocation + identifier + ".csv");
		
		if (!file.exists())
		{
			var results = generateResults();
			
			// write file
			writeToFile(file, results);
		}
		
		// why read????
		return /* readFile */;
	}
	
	private List<ExperimentResult> generateResults()
	{
		ArrayList<ExperimentResult> results =
				new ArrayList<>(datasetParamsCollection.size() * numToGenPerParam * algos.size() * runs);
		
		System.out.printf("Experiment: %s", identifier);
		
		for (var datasetParams : datasetParamsCollection)
		{
			System.out.print("\n  exp run for: " + datasetParams.toString());
			for (int j = 0; j < numToGenPerParam; j++)
			{
				var datasets = IntStream.rangeClosed(1, numToGenPerParam)
						.mapToObj(__ -> datasetParams.intoNewlyGeneratedDataset())
						.collect(Collectors.toList());
				
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
							
							var runtime = Duration.between(start, end).abs();
							
							var result = new ExperimentResult(datasetParams, algo, matching, runtime, run_no);
							
							results.add(result);
						}
					}
				}
			
			} // Genned dataset
			
		}
		
		return results;
	}
	
	private void writeToFile(File file, List<ExperimentResult> results)
	{
		// Todo: buffered
		try (var writer = new PrintWriter(new BufferedWriter(new FileWriter(file))))
		{
			exportData(writer, results);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
}
