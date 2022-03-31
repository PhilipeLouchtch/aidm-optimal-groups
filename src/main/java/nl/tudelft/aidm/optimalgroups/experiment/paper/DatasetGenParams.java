package nl.tudelft.aidm.optimalgroups.experiment.paper;

import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.List;

public interface DatasetGenParams
{
	interface PlottingData
	{
		public List<String> headers();
		public List<String> values();
	}
	
	PlottingData plottingData();
	DatasetContext generateDataset();
}
