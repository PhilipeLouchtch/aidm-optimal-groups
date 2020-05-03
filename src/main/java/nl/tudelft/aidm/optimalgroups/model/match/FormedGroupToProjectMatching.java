package nl.tudelft.aidm.optimalgroups.model.match;

import nl.tudelft.aidm.optimalgroups.algorithm.project.GroupProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group.FormedGroup;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.List;

public class FormedGroupToProjectMatching extends ListBasedMatching<FormedGroup, Project> implements GroupProjectMatching<FormedGroup>
{
	public FormedGroupToProjectMatching(DatasetContext datasetContext, List<? extends Match<FormedGroup, Project>> list)
	{
		super(datasetContext, (List<Match<FormedGroup, Project>>) list);
	}
}
