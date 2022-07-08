package nl.tudelft.aidm.optimalgroups.dataset.transforms;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.complete.ProjectPreferenceAugmentedWithMissingAlternativesCmmnRnd;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class DatasetContext_AugmentedPreferences_AppendedCommonly implements DatasetContext
{
	private final DatasetContext originalDatasetContext;

	private final Agents agents;

	public static DatasetContext from(DatasetContext datasetContext)
	{
		return new DatasetContext_AugmentedPreferences_AppendedCommonly(datasetContext);
	}
	protected DatasetContext_AugmentedPreferences_AppendedCommonly(DatasetContext datasetContext)
	{
		this.originalDatasetContext = datasetContext;
		this.agents = tiesBrokenCommonly(datasetContext.allAgents(), datasetContext.allProjects(), this);
	}

	private static Agents tiesBrokenCommonly(Agents agents, Projects projects, DatasetContext currentContext)
	{
		var projectsShuffled = new ArrayList<>(projects.asCollection());
		Collections.shuffle(projectsShuffled);

		var agentsWithTiesBroken = agents.asCollection().stream()
			.map(agent -> {
				var origPrefs = agent.projectPreference();
				var newPrefs = new ProjectPreferenceAugmentedWithMissingAlternativesCmmnRnd(origPrefs, projects);
				
				// Also have to remap group preferences as they refer to agent instances in the old datasetcontext
				// Luckily, the new agents have the same seqIds so we can use the LazyGroupPreference impl to look up the
				// new instances in the new datasetcontext
				var oldGroupPrefs = agent.groupPreference();
				var groupPrefAsSeqIds = oldGroupPrefs.asListOfAgents().stream().mapToInt(Agent::sequenceNumber).toArray();
				var newGroupPrefs = new GroupPreference.LazyGroupPreference(currentContext, groupPrefAsSeqIds);
				
				return (Agent) new SimpleAgent.AgentInDatacontext(agent.sequenceNumber(), newPrefs, newGroupPrefs, currentContext);
			})
			.collect(Collectors.collectingAndThen(Collectors.toUnmodifiableList(), Agents::from));

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
		return originalDatasetContext.identifier() + "-aug_common";
	}

	@Override
	public String toString()
	{
		return originalDatasetContext.toString() + "-aug_common";
	}
}