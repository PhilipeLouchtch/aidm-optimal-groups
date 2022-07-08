package nl.tudelft.aidm.optimalgroups.dataset.transforms;

import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class DatasetContext_GroupPreferences_OnlyMaxSizeCliques implements DatasetContext
{
	private final CourseEdition originalDatasetContext;
	private final Agents agents;

	public DatasetContext_GroupPreferences_OnlyMaxSizeCliques(CourseEdition courseEdition)
	{
		this.originalDatasetContext = courseEdition;
		this.agents = onlyWithValidMaxCliquePeerPrefsOrNone(courseEdition.allAgents(), this);
	}

	private static Agents onlyWithValidMaxCliquePeerPrefsOrNone(Agents agents, DatasetContext currentContext)
	{
		var maxCliqueGroups = CliqueGroups.from(agents).ofSize(currentContext.groupSizeConstraint().maxSize());
		
		return agents.asCollection().stream()
				.map(agent -> {
					// If agent doesn't have peer prefs, don't do anything
					if (agent.groupPreference().count() == 0)
						return agent;
					
					// If agent has peer preferences, they are only valid if it leads to a max-clique group
					var agentsPeerPreferencesIsAValidMaxClique = maxCliqueGroups.contains(agent);
					if (agentsPeerPreferencesIsAValidMaxClique) {
						return agent;
					}
					
					return new SimpleAgent.AgentInDatacontext(agent.sequenceNumber(), agent.projectPreference(), GroupPreference.none(), currentContext);
				})
				.collect(collectingAndThen(toList(), Agents::from));
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
		return originalDatasetContext.identifier() + "-grouppref_maxonly";
	}

	@Override
	public String toString()
	{
		return originalDatasetContext.toString() + "-grouppref_maxonly";
	}
}
