package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.util.LinkedHashSet;

public class SatisfiedPregroupingAgents extends Agents
{
	public SatisfiedPregroupingAgents(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		super(matching.datasetContext(), satisfiedAsSet(matching, pregrouping));
	}
	
	static private LinkedHashSet<Agent> satisfiedAsSet(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		var preformedGroups = pregrouping.groups();
		
		var agentsPregrouping = pregrouping.groups().asAgents();
		
		var matchingPregroupedSatisfied = AgentToProjectMatching.from(matching.filteredBySubsets(preformedGroups))
		                                                        .filteredBy(agentsPregrouping);
		
		var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
		
		var satisfiedAsSet = new LinkedHashSet<>(pregroupingStudentsSatisfied.asCollection());
		return satisfiedAsSet;
	}
}
