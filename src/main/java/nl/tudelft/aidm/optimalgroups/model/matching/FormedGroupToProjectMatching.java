package nl.tudelft.aidm.optimalgroups.model.matching;

import nl.tudelft.aidm.optimalgroups.algorithm.group.TrivialGroupPartitioning;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.BepSysImprovedGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.BepSysReworked;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Group.FormedGroup;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FormedGroupToProjectMatching extends ListBasedMatching<FormedGroup, Project> implements GroupToProjectMatching<FormedGroup>
{
	public FormedGroupToProjectMatching(DatasetContext datasetContext, List<? extends Match<FormedGroup, Project>> list)
	{
		super(datasetContext, (List<Match<FormedGroup, Project>>) list);
	}

	public static FormedGroupToProjectMatching byTriviallyPartitioning(AgentToProjectMatching agentToProjectMatching)
	{
		var datasetContext = agentToProjectMatching.datasetContext();

		var matchingAsList = new ArrayList<Match<Group.FormedGroup, Project>>();
		
		agentToProjectMatching.groupedByProject().forEach((project, agents) -> {
			var groups = new TrivialGroupPartitioning(Agents.from(agents));
			
			for (var group : groups.asCollection())
			{
				var match = new GroupToProjectMatch<>(group, project);
				matchingAsList.add(match);
			}
		});

		return new FormedGroupToProjectMatching(datasetContext, matchingAsList);
	}
}
