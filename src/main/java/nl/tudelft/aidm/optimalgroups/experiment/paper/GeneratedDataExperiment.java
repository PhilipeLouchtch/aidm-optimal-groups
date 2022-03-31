package nl.tudelft.aidm.optimalgroups.experiment.paper;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentAlgorithmSubresult;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import plouchtch.lang.Lazy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An experiment consisting of multiple datasets that are batched together as a single experiment.
 * Created for the purpose of bundling together experiments of generated datasets, more specifically,
 * generated datasets that are generated from the same parameters.
 */
public class GeneratedDataExperiment
{
	private static final String experimentDataLocation = "/results/thesis/";
	
	private final String identifier;
	
	private final int numToGenPerParam;
	private final int runs;
	
	private final List<DatasetGenParams> params;
	private final List<GroupProjectAlgorithm> algos;
	
	private final Lazy<List<ExperimentAlgorithmSubresult>> results;
	
	// TODO? give list of params for data generation, generate data here. An experiment is the dataset space...
	public GeneratedDataExperiment(String identifier, List<DatasetGenParams> params, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		this.identifier = identifier;
		
		this.numToGenPerParam = numToGenPerParam;
		this.runs = runs;
		
		this.params = params;
		this.algos = algos;
		
		this.results = new Lazy<>(this::readOrGenerateResults);
	}
	
	public void run()
	{
		this.results.get();
		return;
	}
	
	private List<ExperimentAlgorithmSubresult> generateAndWriteResults()
	{
		var file = new File(experimentDataLocation + identifier + ".csv");
		
		if (!file.exists()) {
			var results = generateResults();
			
			// write file
			writeToFile(file, results);
		}
		
		// why read????
		return /* readFile */;
	}
	
	private List<ExperimentAlgorithmSubresult> generateResults()
	{
		var results = new ArrayList<ExperimentAlgorithmSubresult>(params.size() * algos.size() * runs);
		
		// TODO: Must have params to record input -> output mappings
		for (var params : params)
		{
			var dataset = params.generateDataset();
			
			for (GroupProjectAlgorithm algo : algos)
			{
				for (int i = 0; i < runs; i++)
				{
					var matching = algo.determineMatching(dataset);
					
					var algoResult = new ExperimentAlgorithmSubresult(algo, matching);
					
					// actually, we don't care about the matching, we care about the metrics that come out of it...
					results.add(algoResult);
				}
			}
		}
		
		return results;
	}
	
	private void writeToFile(File file, List<ExperimentAlgorithmSubresult> results)
	{
		try (var writer = new FileWriter(file))
		{
			// identifier is sufficient for file name, but here we need params!
			writer.append("identifier").append(",")
					.append("")
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
}
