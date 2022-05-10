package nl.tudelft.aidm.optimalgroups.dataset.generated.agents;

import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import plouchtch.assertion.Assert;

import java.util.HashSet;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

/**
 * Generator to generate agents with pregrouping preference of given size. Will generate {@code floor(count / size)} groups.
 *
 * @param size        The size of pregroupings
 * @param projPrefGen Project preference generator to use
 */
public record PregroupingAgentsGenerator(int size, ProjectPreferenceGenerator projPrefGen) implements AgentGenerator
{
	public PregroupingAgentsGenerator
	{
		Assert.that(size > 0)
		      .orThrowMessage("Pregrouping size cannot be 0");
	}
	
	@Override
	public Agents generate(DatasetContext context, Integer count, IntSupplier sequenceNumberSupplier)
	{
		var agentSet = new HashSet<Agent>(count);
		
		// Make groups
		for (int i = 0; i < count / size; i++)
		{
			var agentIds = IntStream.range(0, size)
			                        .map(__ -> sequenceNumberSupplier.getAsInt())
			                        .toArray();
			
			var mutualPregroupingPreference = new GroupPreference.LazyGroupPreference(this.context, agentIds);
			var mutualProjectPreference = projPrefGen.generateNew();
			
			// Make agents of group
			for (int agentId : agentIds)
			{
				var agent = new Agent.AgentInDatacontext(
						agentId,
						mutualProjectPreference,
						mutualPregroupingPreference,
						this.context
				);
				
				agentSet.add(agent);
			}
		}
		
		return Agents.from(agentSet);
	}
}
