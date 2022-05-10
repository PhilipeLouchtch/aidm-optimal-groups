package nl.tudelft.aidm.optimalgroups.dataset.generated.agents;

import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.function.IntSupplier;

import static java.util.Arrays.stream;

public interface AgentGenerator
{
	/**
	 * Try generating as many agents as possible, but never more than the given {@code count} number.
	 * <p>
	 * If the implementation is generating groups of agents, it must ensure that it does not exceed the given number.
	 * For example, if a generator makes groups of 5 but is given a {@code count} of 11, it can make at most 10 agents
	 * (2 groups of 5 each)
	 *
	 * @param context
	 * @param count   The upperbound for the number of agents to generate
	 */
	Agents generate(DatasetContext context, Integer count, IntSupplier sequenceNumberSupplier);
	
}
