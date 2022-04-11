package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

public interface ExperimentResultsCollector
{
	boolean resultsCollectionCanBeSkipped();
	
	void add(ExperimentSubResult subResult);
}
