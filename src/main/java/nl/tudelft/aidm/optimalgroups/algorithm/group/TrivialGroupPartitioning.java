package nl.tudelft.aidm.optimalgroups.algorithm.group;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.group.FormedGroups;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProjectPreference;
import plouchtch.assertion.Assert;

import java.util.*;

public class TrivialGroupPartitioning extends Groups.ListBacked<Group.FormedGroup> implements GroupFormingAlgorithm
{
	private final Agents agents;
	private final GroupFactorization groupFactorization;

	private FormedGroups groups;

	public TrivialGroupPartitioning(Agents agents)
	{
		this(agents, GroupFactorization.cachedInstanceFor(agents.datasetContext.groupSizeConstraint()));
	}

	public TrivialGroupPartitioning(Agents agents, GroupFactorization groupFactorization)
	{
		this.agents = agents;
		this.groupFactorization = groupFactorization;
		this.groups = null;
	}

	@Override
	protected List<Group.FormedGroup> asList()
	{
		if (groups == null) {
			var groupsTemp = new FormedGroups();

			var gsc = agents.datasetContext.groupSizeConstraint();
			var factorization = groupFactorization.forGivenNumberOfStudents(agents.count());

			Assert.that(factorization.isFactorable())
				.orThrowMessage(String.format(
					"Given number of students (%s) is not factorable into groups of sizes between (%s) and (%s)",
					agents.count(), gsc.minSize(), gsc.maxSize())
				);

			var agentsCopyToShuffled = new ArrayList<>(agents.asCollection());
			Collections.shuffle(agentsCopyToShuffled);

			var agentsToGroup = new Stack<Agent>();
			agentsToGroup.addAll(agentsCopyToShuffled);

			for (int grpSize = gsc.minSize(); grpSize <= gsc.maxSize(); grpSize++)
			{
				var numGroupsOfSize = factorization.numGroupsOfSize()[grpSize];
				
				for (int i = 0; i < numGroupsOfSize; i++)
				{
					var group = new ArrayList<Agent>();
					for (int j = 0; j < grpSize; j++)
					{
						group.add(agentsToGroup.pop());
					}
					
					var groupAsAgents = Agents.from(group);
					var projPrefs = AggregatedProjectPreference.usingGloballyConfiguredMethod(groupAsAgents);
					var tentativeGroup = new Group.TentativeGroup(Agents.from(group), projPrefs);
					groupsTemp.addAsFormed(tentativeGroup);
				}
			}

			Assert.that(agentsToGroup.isEmpty())
				.orThrowMessage("All agents should have been grouped now...");

			groups = groupsTemp;
		}

		return new ArrayList<>(groups.asCollection());
	}

	@Override
	public FormedGroups asFormedGroups()
	{
		return this.groups;
	}
}
