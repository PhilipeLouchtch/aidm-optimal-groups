package nl.tudelft.aidm.optimalgroups.dataset.generated.agents;

import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.function.IntSupplier;

/**
 * Generates a set of agents of which the given proportion is generated with the {@code pregroupedGenerator}, the rest with {@code soloGenerator}
 * Note that to control the sizes of pregroupings created and proportion between them, use {@link ProportionalAgentGenerator} generator.
 *
 * @param soloGenerator        Generator generating 'solo' students
 * @param pregroupedGenerator  Generator generating students that are pregrouped (have pregrouping preference)
 * @param proportionPregrouped Proportion of pregrouped students (a value in the open interval (0, 1) )
 */
public record SoloAndPregroupingAgentsGenerator(SoloAgentGenerator soloGenerator, AgentGenerator pregroupedGenerator, double proportionPregrouped) implements AgentGenerator
{
	@Override
	public Agents generate(DatasetContext context, Integer count, IntSupplier sequenceNumberSupplier)
	{
		var countPregrouped = (int) Math.ceil(count * proportionPregrouped);
		
		var pregrouped = pregroupedGenerator.generate(context, countPregrouped, sequenceNumberSupplier);
		var solo = soloGenerator.generate(context, count - countPregrouped, sequenceNumberSupplier);
		
		return pregrouped.with(solo);
	}
}
