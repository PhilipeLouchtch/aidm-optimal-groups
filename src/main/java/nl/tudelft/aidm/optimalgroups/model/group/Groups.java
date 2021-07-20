package nl.tudelft.aidm.optimalgroups.model.group;

import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.GroupsFromCliques;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public interface Groups<G extends Group>
{
	Collection<G> asCollection();

	void forEach(Consumer<G> fn);

	int count();

	default Agents asAgents()
	{
//		var context = asCollection().stream().flatMap(g -> g.members().asCollection()).

		var agents = this.asCollection().stream()
			.flatMap(g -> g.members().asCollection().stream())
			.collect(Collectors.collectingAndThen(Collectors.toList(), Agents::from));

		return agents;
	}
	
	/**
	 * Filter groups to those only of the given size
	 * @param size Groups must contain this many students
	 * @return Groups of given size
	 */
	default Groups<G> ofSize(int size)
	{
		return this.asCollection().stream()
			        .filter(tentativeGroup -> tentativeGroup.members().count() == size)
			        .collect(collectingAndThen(toList(), Groups.ListBackedImpl<G>::new));
	}
	
	// =================================================
	
	/**
	 * @return a Groups collection with the given group
	 */
	static <G extends Group> Groups<G> of(G group)
	{
		return Groups.of(List.of(group));
	}
	
	/**
	 * @return a Groups collection with the given groups
	 */
	static <G extends Group> Groups<G> of(List<G> groups)
	{
		return new Groups.ListBackedImpl<>(groups);
	}
	
	// ==================================================
	
	/**
	 * A Groups collection impl backed by a list
	 * @param <G>
	 */
	class ListBackedImpl<G extends Group> extends ListBacked<G>
	{
		private List<G> asList;

		public ListBackedImpl(List<G> asList)
		{
			this.asList = asList;
		}

		@Override
		protected List<G> asList()
		{
			return asList;
		}
	}
	
	/**
	 * An abstract Groups collection impl backed by a list
	 * @param <G> The Group type
	 */
	abstract class ListBacked<G extends Group> implements Groups<G>
	{
		abstract protected List<G> asList();
		
		@Override
		public Collection<G> asCollection()
		{
			return Collections.unmodifiableCollection(this.asList());
		}
		
		@Override
		public void forEach(Consumer<G> fn)
		{
			this.asList().forEach(fn);
		}
		
		@Override
		public int count()
		{
			return this.asList().size();
		}
	}
}
