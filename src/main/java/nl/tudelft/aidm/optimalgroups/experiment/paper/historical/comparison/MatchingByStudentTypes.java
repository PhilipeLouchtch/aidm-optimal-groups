package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

public record MatchingByStudentTypes(GroupToProjectMatching<?> matchingAll, AgentToProjectMatching singles, AgentToProjectMatching pregrouped, AgentToProjectMatching pregroupingUnsatisfied)
{
	public static MatchingByStudentTypes from(GroupToProjectMatching<?> matchingAll, Pregrouping pregrouping)
	{
		var preformedGroups = pregrouping.groups();
		var agentsPregrouping = pregrouping.groups()
		                                   .asAgents();
		var agentsSingle = matchingAll.datasetContext()
		                              .allAgents()
		                              .without(agentsPregrouping);
		
		var matchingIndividualsToProjects = AgentToProjectMatching.from(matchingAll);
		
		var matchingPregroupedSatisfied = AgentToProjectMatching.from(matchingAll.filteredBySubsets(preformedGroups))
		                                                        .filteredBy(agentsPregrouping);
		
		var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
		var pregroupingStudentsUnsatisfied = agentsPregrouping.without(pregroupingStudentsSatisfied);
		
		var matchingSingles = matchingIndividualsToProjects.filteredBy(agentsSingle);
		var matchingPregrouped = matchingIndividualsToProjects.filteredBy(agentsPregrouping);
		var matchingPregroupedUnsatisfied = matchingIndividualsToProjects.filteredBy(pregroupingStudentsUnsatisfied);
		
		return new MatchingByStudentTypes(matchingAll, matchingSingles, matchingPregrouped, matchingPregroupedUnsatisfied);
	}
}
