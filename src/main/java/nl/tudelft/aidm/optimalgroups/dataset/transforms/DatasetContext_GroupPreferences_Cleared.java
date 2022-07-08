package nl.tudelft.aidm.optimalgroups.dataset.transforms;

import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import static java.util.stream.Collectors.*;

public class DatasetContext_GroupPreferences_Cleared implements DatasetContext
{
	private final CourseEdition originalDatasetContext;
	private final Agents agents;

	public DatasetContext_GroupPreferences_Cleared(CourseEdition courseEdition)
	{
		this.originalDatasetContext = courseEdition;
		this.agents = withoutPeerPrefs(courseEdition.allAgents(), this);
	}

	private static Agents withoutPeerPrefs(Agents agents, DatasetContext currentContext)
	{
		return agents.asCollection().stream()
			.map(agent -> {
				var modAgent = new SimpleAgent.AgentInDatacontext(agent.sequenceNumber(), agent.projectPreference(), GroupPreference.none(), currentContext);
				return (Agent) modAgent;
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
		return originalDatasetContext.identifier() + "-grouppref_cleared";
	}

	@Override
	public String toString()
	{
		return originalDatasetContext.toString() + "-grouppref_cleared";
	}
}
