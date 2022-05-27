package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import java.util.List;

public record GroupedDatasetParams<DATASET_PARAMS extends DatasetParams>(List<Group<DATASET_PARAMS>> groups)
{
	public record Group<DATASET_PARAMS extends DatasetParams>(String groupIdentifier, List<DATASET_PARAMS> asList)
	{
		public void add(DATASET_PARAMS params)
		{
			this.asList.add(params);
		}
	}
}
