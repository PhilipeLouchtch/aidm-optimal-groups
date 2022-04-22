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
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class GeneratedDataContext implements DatasetContext
{
	private final String id;
	private final Agents agents;
	private final Projects projects;
	private final GroupSizeConstraint groupSizeConstraint;
	
	/**
	 * Constructs a new generated dataset without any pregrouping students, thus all students only have preferences over the projects
	 */
	public GeneratedDataContext(int numAgents, Projects projects, GroupSizeConstraint groupSizeConstraint, ProjectPreferenceGenerator projPrefGenerator)
	{
		this(numAgents, projects, groupSizeConstraint, projPrefGenerator, projPrefGenerator, PregroupingGenerator.none());
	}
	
	/**
	 * Constructs a new generated dataset whereby the single and pregrouping students project preferences are drawn from the same distribution
	 */
	public GeneratedDataContext(int numAgents, Projects projects, GroupSizeConstraint groupSizeConstraint, ProjectPreferenceGenerator projPrefGenerator, PregroupingGenerator pregroupingGenerator)
	{
		this(numAgents, projects, groupSizeConstraint, projPrefGenerator, projPrefGenerator, pregroupingGenerator);
	}
	
	/**
	 * Constructs a new generated dataset with single and pregrouping students whereby the distribution from which the two types of students
	 * (pregrouping or solo)
	 * @param numAgents
	 * @param projects
	 * @param groupSizeConstraint
	 * @param soloStudProjPrefGenerator
	 * @param pregroupStudProjPrefGenerator
	 * @param pregroupingGenerator
	 */
	public GeneratedDataContext(int numAgents, Projects projects, GroupSizeConstraint groupSizeConstraint,
	                            ProjectPreferenceGenerator soloStudProjPrefGenerator, ProjectPreferenceGenerator pregroupStudProjPrefGenerator,
	                            PregroupingGenerator pregroupingGenerator)
	{
		this.projects = projects;
		this.groupSizeConstraint = groupSizeConstraint;
		
		var agentsAsList = new ArrayList<Agent>();
		for (int i = 1; i <= numAgents; i = agentsAsList.size() + 1)
		{
			var groupSize = pregroupingGenerator.draw();
			var numAgentsToMakeRemaining = numAgents - i + 1;
			var numAgentsToMake = Math.min(groupSize, numAgentsToMakeRemaining);
			
			var idsOfAgentsToCreate = IntStream.range(i, i + numAgentsToMake).toArray();
			
			GroupPreference groupPref;
			ProjectPreference projectPreference;
			
			if (numAgentsToMake == 1)
			{
				projectPreference = soloStudProjPrefGenerator.generateNew();
				groupPref = GroupPreference.none();
			}
			else
			{
				projectPreference = pregroupStudProjPrefGenerator.generateNew();
				groupPref = new GroupPreference.LazyGroupPreference(this, idsOfAgentsToCreate);
			}
			
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
