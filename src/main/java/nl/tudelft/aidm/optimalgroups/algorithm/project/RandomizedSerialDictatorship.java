package nl.tudelft.aidm.optimalgroups.algorithm.project;

import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.FormedGroups;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.FormedGroupToProjectSlotMatch;
import nl.tudelft.aidm.optimalgroups.model.matching.FormedGroupToProjectSlotMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.util.*;

public class RandomizedSerialDictatorship implements GroupToProjectMatching<Group.FormedGroup>
{
	private final DatasetContext datasetContext;
	private final FormedGroups groups;
	private final Projects projects;

	public RandomizedSerialDictatorship(DatasetContext datasetContext, FormedGroups groups, Projects projects)
	{
		this.datasetContext = datasetContext;
		this.groups = groups;
		this.projects = projects;
	}

	@Override
	public List<Match<Group.FormedGroup, Project>> asList()
	{
		return result().toProjectMatchings().asList();
	}

	public FormedGroupToProjectSlotMatching result()
	{
		if (this.projects.countAllSlots() < this.groups.count())
			throw new RuntimeException("Too little project slots to assign all groups");

		FormedGroupToProjectSlotMatching result = new FormedGroupToProjectSlotMatching(datasetContext());

		// Map from projectIds to amount of used slots
		Map<Integer, Integer> usedSlots = new HashMap<>();

		List<Group.FormedGroup> shuffledGroups = new ArrayList<>(this.groups.asCollection());
		Collections.shuffle(shuffledGroups);

		// Iterate over the groups is a random order
		for (Group.FormedGroup group : shuffledGroups) {

			// Iterate the preference in order, assign as soon as possible
			// use standard for loop here to be able to break, idk how to do it in a foreach with a consumer function
			Integer[] groupPreference = group.projectPreference().asArray();
			for (int i = 0; i < groupPreference.length; i++) {
				int projectId = groupPreference[i];
				int currentlyUsedSlots = (usedSlots.containsKey(projectId)) ? usedSlots.get(projectId) : 0;

				// If there is still a spot available for this project
				if (currentlyUsedSlots < this.projects.slotsForProject(projectId).size()) {
					usedSlots.put(projectId, currentlyUsedSlots + 1);

					// Retrieve the slot to use (if the currentlyUsedSlots is 0, get index 0, etc)
					Project.ProjectSlot unusedSlot = this.projects.slotsForProject(projectId).get(currentlyUsedSlots);
					FormedGroupToProjectSlotMatch newMatch = new FormedGroupToProjectSlotMatch(group, unusedSlot);
					result.add(newMatch);
					break;
				}
			}
		}

		return result;
	}

	@Override
	public DatasetContext datasetContext()
	{
		return datasetContext;
	}
}
