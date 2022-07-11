package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model;

import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.ConditionalGroupConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.HardGroupingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.SoftGroupConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.SoftGroupEpsilonConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a pregrouping "variant" or "type":
 *  - how the eligable pregroups are determined
 *  - the related constraint for the variant/scenario - how the pregroups are to be handled
 *
 * Once instantiated for a dataset, it results in a {@link Pregrouping}
 *
 * Originally, the Pregrouping and PregroupingType types were imagened to be used with the Chiarandini-based
 * mechanisms but are extended to also be used for other mechanisms that do not support constraints and thus
 * have their own ways of handling pregroups. The use was extended because it was necessary to have a same way
 * of configuring the determining of pregroups and this abstraction was pretty close
 */
public interface PregroupingType
{
	String simpleName();
	
	Pregrouping instantiateFor(Agents agents);
	
	default Pregrouping instantiateFor(DatasetContext datasetContext)
	{
		return instantiateFor(datasetContext.allAgents());
	}
	
	/* Factory Methods */
	
	// ANY CLIQUE
	static PregroupingType anyCliqueHardGrouped()
	{
		return new NamedLambda(
				"anyClique_hardGrp",
				(agents) -> new Pregrouping.anyClique(agents, HardGroupingConstraint::new)
		);
	}
	
	static PregroupingType anyCliqueSoftGrouped()
	{
		return new NamedLambda(
				"anyClique_softGrp",
				(agents) -> new Pregrouping.anyClique(agents, SoftGroupConstraint::new)
		);
	}
	
	static PregroupingType anyCliqueSoftGroupedEpsilon()
	{
		return new NamedLambda(
				"anyClique_softGrpEps",
				(agents) -> new Pregrouping.anyClique(agents, SoftGroupEpsilonConstraint::new)
		);
	}
	
	static PregroupingType anyCliqueConditionallyGrouped(int upToIncludingRank)
	{
		return new NamedLambda(
				"anyClique_condGrp" + upToIncludingRank,
				(agents) -> new Pregrouping.anyClique(agents, groups -> new ConditionalGroupConstraint(groups, upToIncludingRank))
		);
	}
	
	/// SIZED
	static PregroupingType sizedCliqueHardGrouped(Integer... sizes)
	{
		var sizesSetNotation = Arrays.stream(sizes).map(Object::toString).collect(Collectors.joining(",", "{", "}"));
		return new NamedLambda(
				"sizedCliques"+sizesSetNotation+"_hardGrp",
				(agents) -> new Pregrouping.sizedClique(agents, HardGroupingConstraint::new, sizes)
		);
	}
	
	static PregroupingType sizedCliqueSoftGrouped(Integer... sizes)
	{
		var sizesSetNotation = Arrays.stream(sizes).map(Object::toString).collect(Collectors.joining(",", "{", "}"));
		return new NamedLambda(
				"sizedCliques"+sizesSetNotation+"_softGrp",
				(agents) -> new Pregrouping.sizedClique(agents, SoftGroupConstraint::new, sizes)
		);
	}
	
	static PregroupingType sizedCliqueConditionallyGrouped(int upToIncludingRank, Integer... sizes)
	{
		var sizesSetNotation = Arrays.stream(sizes).map(Object::toString).collect(Collectors.joining(",", "{", "}"));
		return new NamedLambda(
				"sizedCliques"+sizesSetNotation+"_condGrp",
				(agents) -> new Pregrouping.sizedClique(agents,  groups -> new ConditionalGroupConstraint(groups, upToIncludingRank), sizes)
		);
	}
	
	/// SIZED - MAX-SIZE ONLY
	static PregroupingType maxCliqueHardGrouped()
	{
		return new NamedLambda(
				"maxCliques_hardGrp",
				(agents) -> new Pregrouping.sizedClique(agents, HardGroupingConstraint::new, agents.datasetContext.groupSizeConstraint().maxSize())
		);
	}
	
	static PregroupingType maxCliqueSoftGrouped()
	{
		return new NamedLambda(
				"maxCliques_softGrp",
				(agents) -> new Pregrouping.sizedClique(agents, SoftGroupConstraint::new, agents.datasetContext.groupSizeConstraint().maxSize())
		);
	}
	
	static PregroupingType maxCliqueSoftGroupedEps()
	{
		return new NamedLambda(
				"maxCliques_softGrpEps",
				(agents) -> new Pregrouping.sizedClique(agents, SoftGroupEpsilonConstraint::new, agents.datasetContext.groupSizeConstraint().maxSize())
		);
	}
	
	/// SIZED - ANY EXCEPT MAX-1 (e.g. if max = 5, then 2, 3 and 5 are allowed)
	static PregroupingType exceptSubmaxCliqueSoftGrouped()
	{
		Function<Integer, Integer[]> determineAllowedSizes = (Integer maxSize) -> IntStream.rangeClosed(2, maxSize)
		                                                                                   .filter(value -> value != maxSize - 1)
		                                                                                   .boxed()
		                                                                                   .toArray(Integer[]::new);
		return new NamedLambda(
				"exceptSubmaxCliques_softGrp",
				(agents) -> new Pregrouping.sizedClique(agents, SoftGroupConstraint::new, determineAllowedSizes.apply(agents.datasetContext.groupSizeConstraint().maxSize()))
		);
	}
	
	static PregroupingType exceptSubmaxCliqueSoftEpsGrouped()
	{
		Function<Integer, Integer[]> determineAllowedSizes = (Integer maxSize) -> IntStream.rangeClosed(2, maxSize)
		                                                                                   .filter(value -> value != maxSize - 1)
		                                                                                   .boxed()
		                                                                                   .toArray(Integer[]::new);
		return new NamedLambda(
				"exceptSubmaxCliques_softGrpEps",
				(agents) -> new Pregrouping.sizedClique(agents, SoftGroupEpsilonConstraint::new, determineAllowedSizes.apply(agents.datasetContext.groupSizeConstraint().maxSize()))
		);
	}
	
	/**
	 * The no-pregrouping type. Does not do any pregrouping.
	 * @return
	 */
	static PregroupingType none()
	{
		return new NamedLambda("no_grouping",
				agents -> new Pregrouping.None()
		);
	}
	
	/* */
	record NamedLambda(String simpleName, Function<Agents, Pregrouping> function) implements PregroupingType
	{
		@Override
		public Pregrouping instantiateFor(Agents agents)
		{
			return function.apply(agents);
		}
	}
	
}
