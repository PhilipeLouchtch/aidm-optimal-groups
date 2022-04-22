package nl.tudelft.aidm.optimalgroups.model.matching;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public interface AgentToProjectMatching extends Matching<Agent, Project>
{
	static AgentToProjectMatching from(Matching<? extends Group, Project> groupMatching)
	{
		return new AgentPerspectiveGroupProjectMatching(groupMatching);
	}

	/**
	 * <p>Provides a default, cacheless implementation of groupedByProject.</p>
	 * <p>The implementation is advised to create its own implementation that
	 * either pre-computes this or caches the result of this base method.</p>
	 * @return A mappting of Agents assigned to the given Project
	 */
	default Map<Project, List<Agent>> groupedByProject()
	{
		return this.asList().stream()
			.map(AgentToProjectMatch::from)
			.collect(
				groupingBy(AgentToProjectMatch::project,
					IdentityHashMap::new, // type of map
					mapping(AgentToProjectMatch::agent, toList())
				)
			);
	}

	default int countDistinctStudents()
	{
		return (int) this.asList().stream().map(Match::from).distinct().count();
	}
	
	default AgentToProjectMatching filteredBy(Agents toKeep)
	{
		if (toKeep.count() == 0) {
			return new Simple(datasetContext());
		}
		
		return new FilteredAgentToProjectMatching(this, toKeep);
	}
	
	default Agents agents()
	{
		return this.asList().stream().map(Match::from)
				.distinct() // what-if
				.collect(Agents.collector);
	}

	/**
	 * Default, simple implementation of AgentToProjectMatching
	 */
	class Simple extends ListBasedMatching<Agent, Project> implements AgentToProjectMatching
	{
		/**
		 * Constructs an empty matching
		 */
		public Simple(DatasetContext datasetContext)
		{
			super(datasetContext);
		}

		public Simple(DatasetContext datasetContext, List<Match<Agent, Project>> backingList)
		{
			super(datasetContext, backingList);
		}

		@Override
		public void add(Match<Agent, Project> match)
		{
			Assert.that(match.from().datasetContext().equals(datasetContext())).orThrowMessage("Cannot include match in matching, datasetcontext mismatch");
			// Projects are context-less ...
//			Assert.that(match.to().context.equals(datasetContext())).orThrowMessage("Cannot include match in matching, datasetcontext mismatch");
			super.add(match);
		}
	}
	
	class FilteredAgentToProjectMatching implements AgentToProjectMatching
	{
		private final Simple matching;
		
		FilteredAgentToProjectMatching(AgentToProjectMatching original, Agents agentsToKeep)
		{
			Assert.that(original.datasetContext() == agentsToKeep.datasetContext).orThrowMessage("Dataset contexts mismatch");
			
			var filtered = original.asList().stream()
		           .filter(match -> agentsToKeep.contains(match.from()))
		           .collect(Collectors.toList());
			
			this.matching = new AgentToProjectMatching.Simple(original.datasetContext(), filtered);
		}
		
		@Override
		public List<Match<Agent, Project>> asList()
		{
			return this.matching.asList();
		}
		
		@Override
		public DatasetContext datasetContext()
		{
			return this.matching.datasetContext();
		}
	}
}
