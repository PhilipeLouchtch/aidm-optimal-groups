package nl.tudelft.aidm.optimalgroups.dataset.generated.agents;

import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import plouchtch.assertion.Assert;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntSupplier;

import static java.util.Arrays.stream;

/**
 * A compound generator (consisting of other generators) that works in proportions/ratios. Useful
 * for generating pregroupings of multuple sizes. E.g.: 20% (0.2) of size 3, 40% (0.4) of size 4 and
 * 40% (0.4) of size 5. Note that this generator is better not used for "solo" students directly, because
 * the proportion-approach is not very exact, the {@code count} parameter of {@link AgentGenerator#generate(DatasetContext, Integer, IntSupplier)}
 * is only an upperbound, the interface does not give guarantees on actually generating that many agents (best effort).
 * For combining solo and pregrouping agent generators, use {@link SoloAndPregroupingAgentsGenerator} generator.
 *
 * @param pregroupingGenerators The generators for generating pregroupings of various sizes or varying project preferences,
 *                              with the expected (approx) proportion of number of agents (!!! not groups of agents) to be
 *                              generated by that generator.
 */
public record ProportionalAgentGenerator(SubGen... pregroupingGenerators) implements AgentGenerator
{
	public record SubGen(AgentGenerator generator, Double proportion)
	{
	}
	
	public ProportionalAgentGenerator
	{
		var sum = Arrays.stream(pregroupingGenerators)
		                .mapToDouble(SubGen::proportion)
		                .sum();
		
		Assert.that(sum == 1.0)
		      .orThrowMessage("Proportions must sum exactly to 1 was %s".formatted(sum));
	}
	
	@Override
	public Agents generate(DatasetContext context, Integer numAgentsToGen, IntSupplier sequenceNumberSupplier)
	{
		var generatedAgents = Agents.empty();
		
		// Sort proportions from largest to smallest as it is easier to fill-up
		var sortedProportions = stream(pregroupingGenerators)
				                        .sorted(Comparator.comparing(SubGen::proportion))
				                        .toList();
		
		var agentsToGenRemaining = numAgentsToGen;
		for (var proportion : sortedProportions)
		{
			var subCount = (int) Math.round(numAgentsToGen * proportion.proportion());
			var subCountBound = Math.min(subCount, agentsToGenRemaining); // never more than we can
			var generated = proportion.generator.generate(context, subCountBound, sequenceNumberSupplier);
			
			agentsToGenRemaining -= generated.count();
			generatedAgents = generatedAgents.with(generated);
		}
		
		// note: the above does not guarantee exactly numAgentsToGen are generated (may be less)
		// use this generator with SoloAndPregroupingAgentsGenerator to fill up the remainder with 'solo' agents
		
		return generatedAgents;
	}
}
