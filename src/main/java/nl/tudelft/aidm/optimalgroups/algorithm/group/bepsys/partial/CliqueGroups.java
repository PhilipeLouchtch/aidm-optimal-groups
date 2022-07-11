package nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProjectPreference;
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
			tentativeCliques = cliquesExtractedFrom(agents);
		}

		return Collections.unmodifiableList(tentativeCliques);
	}

	private static List<Group.TentativeGroup> cliquesExtractedFrom(Agents agents)
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

			// TODO: What if not completely mutual, but has a subclique? So some members have included an additional student??
			if (GroupPreference.isMutual(student)) {
				var peers = student.groupPreference().asListOfAgents();
				var proposedGroup = Agents.from(student).with(peers);

				// TODO: Below is based on what BEPSys mechanism did: after making groups, the groups are matched to projects. Therefore, a group
				//  must have a single preference over the projects. This preference can be created though many ways. BEPSys aggregated the preference
				//  by ordering the projects by their Borda scores (basically a totally ordered preference list sorted by the average rank of projects).
				//  The project preferences abstraction and the rest of the code-base has support for unacceptible alternatives but the below appoach
				//  requires double checking (the aggregate preference is used for grouping constraints for the MILP/Gurobi approach)
				var tentativeGroup = new Group.TentativeGroup(proposedGroup, AggregatedProjectPreference.usingGloballyConfiguredMethod(proposedGroup));
				tentativelyFormed.add(tentativeGroup);

				available.removeAll(tentativeGroup.members().asCollection());
				unavailable.addAll(tentativeGroup.members().asCollection());
			}
		}

		return tentativelyFormed;
	}
}
