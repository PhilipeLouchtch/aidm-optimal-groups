package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import java.util.List;

public record GroupedDatasetParams(List<Group> groups)
{
	public record Group(String groupIdentifier, List<DatasetParams> asList) {}
}
