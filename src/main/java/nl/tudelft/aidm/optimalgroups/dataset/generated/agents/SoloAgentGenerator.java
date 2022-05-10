package nl.tudelft.aidm.optimalgroups.dataset.generated.agents;

import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;

import java.util.HashSet;
import java.util.function.IntSupplier;

public record SoloAgentGenerator(ProjectPreferenceGenerator projPrefGen) implements AgentGenerator
{
	@Override
	public Agents generate(DatasetContext context, Integer count, IntSupplier sequenceNumberSupplier)
	{
		var agentSet = new HashSet<Agent>(count);
		
		for (var i = 0; i < count; i++)
		{
			var newAgent = new Agent.AgentInDatacontext(
					sequenceNumberSupplier.getAsInt(),
					projPrefGen.generateNew(),
					GroupPreference.none(),
					context
			);
			
			agentSet.add(newAgent);
		}
		
		return Agents.from(agentSet);
	}
}
