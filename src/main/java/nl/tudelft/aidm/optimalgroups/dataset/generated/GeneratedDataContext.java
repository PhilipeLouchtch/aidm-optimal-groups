package nl.tudelft.aidm.optimalgroups.dataset.generated;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.time.Instant;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class GeneratedDataContext implements DatasetContext
{
	private final String id;
	private final Agents agents;
	private final Projects projects;
	private final GroupSizeConstraint groupSizeConstraint;

	public GeneratedDataContext(int numAgents, int numProjects, GroupSizeConstraint groupSizeConstraint)
	{
		this.groupSizeConstraint = groupSizeConstraint;

		var hexEpochSeconds = Long.toHexString(Instant.now().getEpochSecond());
		id = String.format("DC[RND_a%s_p%s_%s]_%s", numAgents, numProjects, hexEpochSeconds, groupSizeConstraint);

		projects = Projects.from(
			IntStream.rangeClosed(1, numProjects)
				.mapToObj(Project.withDefaultSlots::new)
				.collect(Collectors.toList())
		);

		var generator = new SimpleSinglePeakedGenerator(projects);
		agents = Agents.from(
			IntStream.rangeClosed(1, numAgents)
				.mapToObj(i -> new Agent.AgentInDatacontext(i, generator.generateNew(), GroupPreference.none(), this))
				.collect(Collectors.toList())
		);
	}

	@Override
	public String identifier()
	{
		return id;
	}

	@Override
	public Projects allProjects()
	{
		return projects;
	}

	@Override
	public Agents allAgents()
	{
		return agents;
	}

	@Override
	public GroupSizeConstraint groupSizeConstraint()
	{
		return groupSizeConstraint;
	}
}
