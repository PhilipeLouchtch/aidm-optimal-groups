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
		var preformedGroups = pregrouping.groups();
		var agentsPregrouping = pregrouping.groups()
		                                   .asAgents();
		
		var matchingPregroupedSatisfied = AgentToProjectMatching.from(matching.filteredBySubsets(preformedGroups))
		                                                        .filteredBy(agentsPregrouping);
		
		var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
		var pregroupingStudentsUnsatisfied = agentsPregrouping.without(pregroupingStudentsSatisfied);
		
		var unsatAsSet = new LinkedHashSet<>(pregroupingStudentsUnsatisfied.asCollection());
		return unsatAsSet;
	}
}
