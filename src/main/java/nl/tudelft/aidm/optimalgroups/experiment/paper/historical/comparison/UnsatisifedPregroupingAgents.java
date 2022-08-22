package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;

public class UnsatisifedPregroupingAgents extends Agents
{
	public UnsatisifedPregroupingAgents(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		super(matching.datasetContext(), unsatAsSet(matching, pregrouping));
	}
	
	@NotNull
	private static LinkedHashSet<Agent> unsatAsSet(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		var pregroupings = pregrouping.groups();
		var pregroupingAgents = pregroupings.asAgents();
		
		var satisfiedPregroups = pregrouping.groups().ofWhichSatisfiedIn(matching);
		
		var satisfiedAgents = satisfiedPregroups.asAgents();
		var unsatisfiedAgents = pregroupingAgents.without(satisfiedAgents);
		
		var unsatAsSet = new LinkedHashSet<>(unsatisfiedAgents.asCollection());
		return unsatAsSet;
	}
}
