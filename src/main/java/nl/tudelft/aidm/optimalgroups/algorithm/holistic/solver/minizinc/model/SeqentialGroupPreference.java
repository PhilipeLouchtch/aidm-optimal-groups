package nl.tudelft.aidm.optimalgroups.algorithm.holistic.solver.minizinc.model;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import plouchtch.lang.Lazy;

import java.util.List;
import java.util.stream.Collectors;

public class SeqentialGroupPreference implements GroupPreference
{
	private final SequentualDatasetContext seqDatasetContext;

	private final GroupPreference original;
	private List<SequentualAgents.SequentualAgent> asList;
	
	private Lazy<Agent> owner;

	public static SeqentialGroupPreference fromOriginal(SequentualDatasetContext seqDatasetContext, GroupPreference original)
	{
		// lazy because the context is not yet finished creating when this object is instantiated
		return new SeqentialGroupPreference(seqDatasetContext, original);
	}

	private SeqentialGroupPreference(SequentualDatasetContext seqDatasetContext, GroupPreference original)
	{
		this.seqDatasetContext = seqDatasetContext;
		this.original = original;
	}

	@Override
	public Agent[] asArray()
	{
		return this.asListOfAgents().toArray(Agent[]::new);
	}

	@Override
	public List<Agent> asListOfAgents()
	{
		if (asList == null) {
			asList = original.asListOfAgents().stream().map(agent -> seqDatasetContext.allAgents().correspondingSeqAgentOf(agent))
				.collect(Collectors.toList());
		}

		return (List<Agent>) ((Object) asList);
	}

	@Override
	public Integer count()
	{
		return asList.size();
	}
}
