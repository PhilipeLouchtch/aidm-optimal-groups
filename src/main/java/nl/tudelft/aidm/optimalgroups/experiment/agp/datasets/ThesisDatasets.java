package nl.tudelft.aidm.optimalgroups.experiment.agp.datasets;

import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

public interface ThesisDatasets
{
	public static DatasetContext CE10Like(int numAgents)
	{
		return GeneratedDataContext.withNormallyDistributedProjectPreferences(numAgents, 40, GroupSizeConstraint.manual(4,5), 4, PregroupingGenerator.none());
	}
}
