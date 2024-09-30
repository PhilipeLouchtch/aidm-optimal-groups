package nl.tudelft.aidm.optimalgroups.experiment.paper.synthetic.model;

public interface ExperimentResultsCollector extends AutoCloseable
{
	boolean resultsCollectionCanBeSkipped();
	
	void add(ExperimentSubResult subResult);
}
