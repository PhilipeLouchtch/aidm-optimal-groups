package nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProfilePreference;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class CliqueGroups extends Groups.ListBacked<Group.TentativeGroup>
{
	private final Agents agents;
	private List<Group.TentativeGroup> tentativeCliques;

	public CliqueGroups(Agents agents)
	{
		this.agents = agents;
	}
	
	public static CliqueGroups from(Agents agents)
	{
		return new CliqueGroups(agents);
	}

	@Override
	protected List<Group.TentativeGroup> asList()
	{
		if (tentativeCliques == null) {
			tentativeCliques = cliquesExtractedFrom(agents, agents.datasetContext.groupSizeConstraint());
		}

		return Collections.unmodifiableList(tentativeCliques);
	}

	private static List<Group.TentativeGroup> cliquesExtractedFrom(Agents agents, GroupSizeConstraint groupSizeConstraint)
	{
		var available = new HashSet<>(agents.asCollection());
		var unavailable = new HashSet<Agent>(available.size());

		var tentativelyFormed = new LinkedList<Group.TentativeGroup>();

		// Note: we cannot simply iterate over the "available" students because we need to modify that collection during
		// each iteration - such action results in a ConcurrentModificationException in Java. Therefore, we iterate over
		// agents.asCollection (which are the same agents that are 'available' at the start of the execution of the function
		// and simply skip already processed agents
		for (Agent student : agents.asCollection())
		{
			if (unavailable.contains(student)) {
				// already processed this guy, next!
				continue;
			}

			// Todo: move out to another layer? - this method should just identify cliques
			if (student.groupPreferenceLength() > groupSizeConstraint.maxSize()) {
				// Don't allow cliques larger than max group size
				continue;
			}

			// TODO: What if not completely mutual, but has a subclique? So some members have included an additional student??
			if (GroupPreference.isMutual(student)) {
				var peers = student.groupPreference.asListOfAgents();
				var proposedGroup = Agents.from(student).with(peers);

				var tentativeGroup = new Group.TentativeGroup(proposedGroup, AggregatedProfilePreference.usingGloballyConfiguredMethod(proposedGroup));
				tentativelyFormed.add(tentativeGroup);

				available.removeAll(tentativeGroup.members().asCollection());
				unavailable.addAll(tentativeGroup.members().asCollection());
			}
		}

		return tentativelyFormed;
	}
}