package nl.tudelft.aidm.optimalgroups.model.pref;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.*;
import java.util.function.Function;

public interface GroupPreference
{
	// Array of id's sufficies for now
	Agent[] asArray();

	List<Agent> asListOfAgents();

	Integer count();
	
    /**
	 * Is the group proposed by the agent mutual? That is, do the agents
	 * that form part of the proposal have identical proposal? A.k.a. "a clique" of friends
	 * @return True if "friends" have 'identical' peer preferences
	 */
	static boolean isMutual(Agent agent)
	{
		if(agent.groupPreference().count() == 0) {
			// No preference, therefore also no clique
			return false;
		}

		// Function that maps the Agent's "group preferences" to a Set of agents (including himself)
		// intuitively, this is the agent's proposal for a (partial) group formation.
		Function<Agent, Set<Agent>> agentPreferencesToProposedGroup = (Agent x) -> {
			var groupProposal = new HashSet<Agent>();
			//Add agent himself to set to make comparing preferences easy
			groupProposal.add(x);
			groupProposal.addAll(x.groupPreference().asListOfAgents());

			return groupProposal;
		};

		// The proposal of the given agent
		var proposedGroupOfAgent = agentPreferencesToProposedGroup.apply(agent);

		// A group proposal is completely  mutual if all agents in the proposal
		// would propose exactly the same proposal
		var agentProposalIsCompletelyMutual = agent.groupPreference().asListOfAgents().stream()
			// Now check if the proposals of the peers, that are part of this agents proposal
			.map(agentPreferencesToProposedGroup)
			// are exactly the same as the proposal of "this" agent
			.allMatch(proposedGroupOfAgent::equals);

		return agentProposalIsCompletelyMutual;
	}
	
	static GroupPreference none()
	{
		return None.instance;
	}

	/* */
	class None implements GroupPreference
	{
		private static final None instance = new None();
		private static final Agent[] asArray = new SimpleAgent[0];
		
		private None()
		{
		}
		
		@Override
		public Agent[] asArray()
		{
			return asArray;
		}

		@Override
		public List<Agent> asListOfAgents()
		{
			return Collections.emptyList();
		}

		@Override
		public Integer count()
		{
			return 0;
		}
	}
	
	/**
	 * Group Preference implementation for when Agent objects are not
	 * yet available. Lazily resolves a given set of Agent id's to actual
	 * agent objects by looking them up in the given DatasetContext when needed
	 */
	class LazyGroupPreference implements GroupPreference
	{
		private final DatasetContext datasetContext;
		private final int[] agentSeqIds;
		
		private Agent[] asArray;
		
		public LazyGroupPreference(DatasetContext datasetContext, int... agentSeqIds)
		{
			this.datasetContext = datasetContext;
			this.agentSeqIds = agentSeqIds;
		}
		
		@Override
		public Agent[] asArray()
		{
			if (asArray == null) {
				try {
					this.asArray = Arrays.stream(agentSeqIds)
					                     .mapToObj(agentId -> datasetContext.allAgents().findBySequenceNumber(agentId).orElseThrow())
					                     .toArray(Agent[]::new);
				} catch (NoSuchElementException ex) {
					throw new RuntimeException(ex);
				}
			}
			
			return this.asArray;
		}
		
		@Override
		public List<Agent> asListOfAgents()
		{
			return Arrays.asList(this.asArray());
		}
		
		@Override
		public Integer count()
		{
			return asArray().length;
		}
	}
}
