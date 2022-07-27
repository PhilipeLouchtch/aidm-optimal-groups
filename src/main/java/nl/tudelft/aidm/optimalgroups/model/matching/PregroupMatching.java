package nl.tudelft.aidm.optimalgroups.model.matching;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;

public interface PregroupMatching
{
	static GroupToProjectMatching containingPregroups(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		return matching.filteredBySubsets(pregrouping.groups());
	}
	
	static AgentToProjectMatching ofPregroupingSatisfiedStudents(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		return AgentToProjectMatching.from(matching).filteredBy(pregrouping.groups().asAgents());
	}
}
