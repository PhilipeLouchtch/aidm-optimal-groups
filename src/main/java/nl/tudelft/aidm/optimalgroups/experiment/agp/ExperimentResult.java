package nl.tudelft.aidm.optimalgroups.experiment.agp;

import nl.tudelft.aidm.optimalgroups.metric.PopularityMatrix;
import nl.tudelft.aidm.optimalgroups.metric.PopularityMatrix2;
import plouchtch.lang.Lazy;

import java.util.Collections;
import java.util.List;

public class ExperimentResult
{
	private final Experiment experiment;
	public final List<ExperimentAlgorithmSubresult> results;
	
	private final Lazy<PopularityMatrix<?, ?, ?>> popularityMatrix;

	public ExperimentResult(Experiment experiment, List<ExperimentAlgorithmSubresult> results)
	{
		this.experiment = experiment;
		this.results = Collections.unmodifiableList(results);

		this.popularityMatrix = new Lazy<>(() -> new PopularityMatrix.TopicGroup(results));
	}
	
	public PopularityMatrix<?,?,?> popularityMatrix()
	{
		return popularityMatrix.get();
	}
}
