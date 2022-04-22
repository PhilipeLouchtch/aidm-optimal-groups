package nl.tudelft.aidm.optimalgroups.model.matching;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.List;
import java.util.stream.Collectors;

public interface GroupToProjectMatching<G extends Group> extends Matching<G, Project>
{
	/**
	 * The pregrouping that was determined in the dataset and was used in the making of the matching.
	 *
	 * Included in the GroupToProjectMatching to pass the information on for metrics and etc. Especially
	 * for thesis results collection. Additionally, there (may) exist multiple algorithms that determine
	 * the cliques/pregroupings in the dataset, by including their results we do not need to keep track
	 * of how the pregrouping was established
	 * @return The pregrouping which was determined by some method before running the mechanism (supported mechanisms)
	 */
//	Pregrouping pregrouping();
	
	static GroupToProjectMatching<Group.FormedGroup> byTriviallyPartitioning(AgentToProjectMatching agentToProjectMatching)
	{
		return FormedGroupToProjectMatching.byTriviallyPartitioning(agentToProjectMatching);
	}
	
	/**
	 * Filters the matching by groups that are supersets of the given groups
	 * @param groups The groups to filter by (subsets)
	 * @return A matching holding holding only groups that are (super)sets of the given groups
	 */
	default GroupToProjectMatching<G> filteredBySubsets(Groups<? extends Group> groups)
	{
		var filteredMatches = this.asList().stream()
			    // Filter out any groups that are not a superset of one of the given groups
				.filter(match -> groups.asCollection().stream().anyMatch(givenGroup -> match.from().members().containsAll(givenGroup.members())))
				.collect(Collectors.toList());
		
		return new GroupToProjectMatching<>()
		{
			@Override
			public List<Match<G, Project>> asList()
			{
				return filteredMatches;
			}
			
			@Override
			public DatasetContext datasetContext()
			{
				return GroupToProjectMatching.this.datasetContext();
			}
		};
	}
}
