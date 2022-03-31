package nl.tudelft.aidm.optimalgroups.dataset.generated;

import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.NormallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.UniformProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import plouchtch.assertion.Assert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator.ChancePerTypeBased.*;

public class GeneratedDataContext implements DatasetContext
{
	private final String id;
	private final Agents agents;
	private final Projects projects;
	private final GroupSizeConstraint groupSizeConstraint;

	public GeneratedDataContext(int numAgents, Projects projects, GroupSizeConstraint groupSizeConstraint, ProjectPreferenceGenerator projPrefGenerator, PregroupingGenerator pregroupingGenerator)
	{
		this.projects = projects;
		this.groupSizeConstraint = groupSizeConstraint;
		
		Supplier<Integer> cliqueSizeSupplier = null;
		
		var agentsAsList = new ArrayList<Agent>();
		for (int i = 1; i < numAgents; i = agentsAsList.size())
		{
			var groupSize = pregroupingGenerator.draw();
			var numAgentsToMakeRemaining = numAgents - i;
			var numAgentsToMake = Math.min(groupSize, numAgentsToMakeRemaining);
			
			final var projectPreference = projPrefGenerator.generateNew();
			
			var idsOfAgentsToCreate = IntStream.range(i, i + numAgentsToMake).boxed().toArray(Integer[]::new);
			
			var groupPref = numAgentsToMake > 1
					? new GroupPreference.LazyGroupPreference(this, idsOfAgentsToCreate)
					: GroupPreference.none();
			
			for (Integer newAgentSeqId : idsOfAgentsToCreate)
			{
				var agent = new SimpleAgent.AgentInDatacontext(newAgentSeqId, projectPreference, groupPref, this);
				agentsAsList.add(agent);
			}
		}
		
		this.agents = Agents.from(agentsAsList);
		Assert.that(this.agents.count() == numAgents).orThrowMessage("Bugcheck: not enough agents generated");
		
		var hexEpochSeconds = Long.toHexString(Instant.now().getEpochSecond());
		this.id = "DC[RND_a%s_p%s_%s]_%s".formatted(numAgents, projects.count(), hexEpochSeconds, groupSizeConstraint);
	}

	public static GeneratedDataContext withNormallyDistributedProjectPreferences(int numAgents, int numProjects, GroupSizeConstraint groupSizeConstraint, double curveSteepness, PregroupingGenerator pregroupingGenerator)
	{
		var projects = Projects.generated(numProjects, 5);
		var generator = new NormallyDistributedProjectPreferencesGenerator(projects, curveSteepness);
		
		return new GeneratedDataContext(numAgents, projects, groupSizeConstraint, generator, pregroupingGenerator);
	}

	public static GeneratedDataContext withUniformlyDistributedProjectPreferences(int numAgents, int numProjects, GroupSizeConstraint groupSizeConstraint, PregroupingGenerator pregroupingGenerator)
	{
		var projects = Projects.generated(numProjects, 5);
		var generator = new UniformProjectPreferencesGenerator(projects);
				
		return new GeneratedDataContext(numAgents, projects, groupSizeConstraint, generator, pregroupingGenerator);
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
