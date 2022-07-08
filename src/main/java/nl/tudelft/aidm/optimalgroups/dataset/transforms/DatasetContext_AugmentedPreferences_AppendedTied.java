package nl.tudelft.aidm.optimalgroups.dataset.transforms;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.complete.ProjectPreferenceAugmentedWithMissingTiedLast;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.util.stream.Collectors;

public class DatasetContext_AugmentedPreferences_AppendedTied implements DatasetContext
{
	private final DatasetContext originalDatasetContext;

	private final Agents agents;

	public static DatasetContext from(DatasetContext datasetContext)
	{
		return new DatasetContext_AugmentedPreferences_AppendedTied(datasetContext);
	}
	
	protected DatasetContext_AugmentedPreferences_AppendedTied(DatasetContext datasetContext)
	{
		this.originalDatasetContext = datasetContext;
		this.agents = tiesBrokenIndividually(datasetContext.allAgents(), datasetContext.allProjects(), this);
	}

	private static Agents tiesBrokenIndividually(Agents agents, Projects projects, DatasetContext currentContext)
	{
		var agentsWithTiesBroken = agents.asCollection().stream()
			.map(agent -> {
				var origPrefs = agent.projectPreference();
				var newPrefs = new ProjectPreferenceAugmentedWithMissingTiedLast(origPrefs, projects);
				
				// Also have to remap group preferences as they refer to agent instances in the old datasetcontext
				// Luckily, the new agents have the same seqIds so we can use the LazyGroupPreference impl to look up the
				// new instances in the new datasetcontext
				var oldGroupPrefs = agent.groupPreference();
				var groupPrefAsSeqIds = oldGroupPrefs.asListOfAgents().stream().mapToInt(Agent::sequenceNumber).toArray();
				var newGroupPrefs = new GroupPreference.LazyGroupPreference(currentContext, groupPrefAsSeqIds);
				
				return (Agent) new SimpleAgent.AgentInDatacontext(agent.sequenceNumber(), newPrefs, newGroupPrefs, currentContext);
			})
			.collect(Agents.collector);

		return agentsWithTiesBroken;
	}

	@Override
	public Projects allProjects()
	{
		return originalDatasetContext.allProjects();
	}

	@Override
	public Agents allAgents()
	{
		return agents;
	}

	@Override
	public GroupSizeConstraint groupSizeConstraint()
	{
		return originalDatasetContext.groupSizeConstraint();
	}
	
	@Override
	public String identifier()
	{
		return originalDatasetContext.identifier() + "-aug_tiedlast";
	}
	
	@Override
	public String toString()
	{
		return originalDatasetContext.toString() + "-aug_tiedlast";
	}
}
