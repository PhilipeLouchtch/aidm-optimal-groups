package nl.tudelft.aidm.optimalgroups.model.group;

import java.util.Collection;
import java.util.function.Consumer;

public interface Groups<G extends Group>
{
	Collection<G> asCollection();
	void forEach(Consumer<G> fn);
	int count();
}