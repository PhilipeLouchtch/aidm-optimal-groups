package nl.tudelft.aidm.optimalgroups.model.agent;

import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Collection class for Agent
 * TODO: Refactor into interface
 */
public class Agents implements Iterable<Agent>
{
	public final DatasetContext datasetContext;

	// list for now
	private Set<Agent> asSet;
	private Agent[] asArray;


	public Agents(DatasetContext datasetContext, LinkedHashSet<Agent> asSet)
	{
		this.datasetContext = datasetContext;

		// Ensure agents are ordered in same way as given (for consistency
		// and one experimental algorithm assumes this afaik)
		this.asSet = new LinkedHashSet<>(asSet);
		
		var maxSeqNum = asSet.stream().mapToInt(agent -> agent.sequenceNumber()).max().orElse(0);

//		// Better to change the ctor signature
//		Assert.that(this.agents.size() == agents.size()).orThrowMessage("Agents contained duplicates");

		asArray = new Agent[maxSeqNum+1]; // sequenceNum starts at 1
		for (Agent agent : asSet)
		{
			asArray[agent.sequenceNumber()] = agent;
		}
	}

	public int count()
	{
		return asSet.size();
	}
	
	public boolean contains(Agent agent)
	{
		return this.asSet.contains(agent);
	}
	
	public boolean containsAll(Agents agents)
	{
		return this.asSet.containsAll(agents.asSet);
	}
	
	public Optional<Agent> findBySequenceNumber(Integer sequenceNumber)
	{
		if (sequenceNumber <= 0 || count() < sequenceNumber)
			return Optional.empty();
		
		return Optional.of(this.asArray[sequenceNumber]);
	}

	public Collection<Agent> asCollection()
	{
		return Collections.unmodifiableCollection(asSet);
	}

	public void useCombinedPreferences() {
		for (Agent a : this.asCollection()) {
			a.replaceProjectPreferenceWithCombined(this);
		}
	}

	public void useDatabasePreferences() {
		for (Agent a : this.asCollection()) {
			a.useDatabaseProjectPreferences();
		}
	}

	// =================================
	// WITH / WITHOUT
	
	public Agents with(Agent other)
	{
		return this.with(Agents.from(other));
	}
	
	public Agents with(Collection<Agent> other)
	{
		return this.with(Agents.from(other));
	}

	public Agents with(Agents other)
	{
		Assert.that(/*((datasetContext == null && other.datasetContext != null) || (datasetContext != null && other.datasetContext == null) || (datasetContext != null && other.datasetContext != null))*/
			datasetContext.equals(other.datasetContext)).orThrowMessage("Cannot combine Agents: datasetcontext mismatch");

		var copyAgents = new LinkedHashSet<Agent>(this.asSet.size() + other.asSet.size());
		copyAgents.addAll(this.asSet);
		copyAgents.addAll(other.asSet);

		return new Agents(datasetContext, copyAgents);
	}

	public Agents without(Agent agent)
	{
		return this.without(Agents.from(agent));
	}

	public Agents without(Collection<Agent> other)
	{
		return this.without(Agents.from(other));
	}

	public Agents without(Agents other)
	{
		if (other.count() == 0) return this;

		Assert.that(datasetContext.equals(other.datasetContext))
			.orThrowMessage("Cannot remove Agents: datasetcontext mismatch");

		LinkedHashSet<Agent> without = new LinkedHashSet<>(Math.max(this.asSet.size() - other.count(), 0));
		for (var agent : this.asSet) {
			if (!other.asSet.contains(agent)) {
				without.add(agent);
			}
		}

		return new Agents(datasetContext, without);
	}
	
	@Override
	public Iterator<Agent> iterator()
	{
		return asSet.iterator();
	}
	
	public static Agents from(Agent... agents)
	{
		return Agents.from(List.of(agents));
	}

	public static Agents from(Collection<Agent> agents)
	{
		var datasetContext = agents.stream().map(agent -> agent.datasetContext())
			.findAny().orElseGet(() -> {
//				System.out.print("Warning: creating Agents from empty collection, datasetContext is set to null!\n");
				return null;
			});

		return new Agents(datasetContext, new LinkedHashSet<>(agents));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof Agents)) return false;
		Agents other = (Agents) o;
		return datasetContext.equals(other.datasetContext) &&
			asSet.equals(other.asSet);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(asSet, datasetContext);
	}
	
	public static final AgentsCollector collector = new AgentsCollector();
	
	private static class AgentsCollector implements Collector<Agent, List<Agent>, Agents>
	{
		@Override
		public Supplier<List<Agent>> supplier()
		{
			return LinkedList::new;
		}
		
		@Override
		public BiConsumer<List<Agent>, Agent> accumulator()
		{
			return List::add;
		}
		
		@Override
		public BinaryOperator<List<Agent>> combiner()
		{
			return (agents, agents2) -> {
				agents.addAll(agents2);
				return agents;
			};
		}
		
		@Override
		public Function<List<Agent>, Agents> finisher()
		{
			return Agents::from;
		}
		
		@Override
		public Set<Characteristics> characteristics()
		{
			return Set.of();
		}
	}
	
}
