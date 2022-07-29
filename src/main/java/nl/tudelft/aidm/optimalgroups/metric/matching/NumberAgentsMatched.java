package nl.tudelft.aidm.optimalgroups.metric.matching;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;

public class NumberAgentsMatched
{
	private final int asInt;
	private final Matching matching;

	private NumberAgentsMatched(int numberOfStudents, Matching matching)
	{
		this.asInt = numberOfStudents;
		this.matching = matching;
	}

	public int asInt()
	{
		return asInt;
	}

	public static NumberAgentsMatched fromGroupMatching(Matching<? extends Group, ?> matching)
	{
		var numAgents = matching.asList().stream()
			.flatMap(match -> match.from().members().asCollection().stream())
			.distinct()
			.count();

		return new NumberAgentsMatched((int) numAgents, matching);
	}

	public static NumberAgentsMatched fromAgentMatching(Matching<Agent, ?> matching)
	{
		var numAgents = matching.asList().stream()
			.map(Match::from)
			.distinct()
			.count();

		return new NumberAgentsMatched((int) numAgents, matching);
	}
}
