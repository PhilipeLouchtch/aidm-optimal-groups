package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

public interface ExperimentResultsCollector extends AutoCloseable
{
	boolean resultsCollectionCanBeSkipped();
	
	void add(ExperimentSubResult subResult);
}
