package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model;

import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.ConditionalGroupConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.HardGroupingConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.SoftGroupConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping.SoftGroupEpsilonConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface PregroupingType
{
	String simpleName();
	Pregrouping instantiateFor(DatasetContext datasetContext);
	
	/* Factory Methods */
	
	// ANY CLIQUE
	static PregroupingType anyCliqueHardGrouped()
	{
		return new NamedLambda(
				"anyClique_hardGrp",
				(datasetContext) -> new Pregrouping.anyClique(datasetContext, HardGroupingConstraint::new)
		);
	}
	
	static PregroupingType anyCliqueSoftGrouped()
	{
		return new NamedLambda(
				"anyClique_softGrp",
				(datasetContext) -> new Pregrouping.anyClique(datasetContext, SoftGroupConstraint::new)
		);
	}
	
	static PregroupingType anyCliqueSoftGroupedEpsilon()
	{
		return new NamedLambda(
				"anyClique_softGrpEps",
				(datasetContext) -> new Pregrouping.anyClique(datasetContext, SoftGroupEpsilonConstraint::new)
		);
	}
	
	static PregroupingType anyCliqueConditionallyGrouped(int upToIncludingRank)
	{
		return new NamedLambda(
				"anyClique_condGrp" + upToIncludingRank,
				(datasetContext) -> new Pregrouping.anyClique(datasetContext, groups -> new ConditionalGroupConstraint(groups, upToIncludingRank))
		);
	}
	
	/// SIZED
	static PregroupingType sizedCliqueHardGrouped(Integer... sizes)
	{
		var sizesSetNotation = Arrays.stream(sizes).map(Object::toString).collect(Collectors.joining(",", "{", "}"));
		return new NamedLambda(
				"sizedCliques"+sizesSetNotation+"_hardGrp",
				(datasetContext) -> new Pregrouping.sizedClique(datasetContext, HardGroupingConstraint::new, sizes)
		);
	}
	
	static PregroupingType sizedCliqueSoftGrouped(Integer... sizes)
	{
		var sizesSetNotation = Arrays.stream(sizes).map(Object::toString).collect(Collectors.joining(",", "{", "}"));
		return new NamedLambda(
				"sizedCliques"+sizesSetNotation+"_softGrp",
				(datasetContext) -> new Pregrouping.sizedClique(datasetContext, SoftGroupConstraint::new, sizes)
		);
	}
	
	static PregroupingType sizedCliqueConditionallyGrouped(int upToIncludingRank, Integer... sizes)
	{
		var sizesSetNotation = Arrays.stream(sizes).map(Object::toString).collect(Collectors.joining(",", "{", "}"));
		return new NamedLambda(
				"sizedCliques"+sizesSetNotation+"_condGrp",
				(datasetContext) -> new Pregrouping.sizedClique(datasetContext,  groups -> new ConditionalGroupConstraint(groups, upToIncludingRank), sizes)
		);
	}
	
	/**
	 * The no-pregrouping type. Does not do any pregrouping.
	 * @return
	 */
	static PregroupingType none()
	{
		return new NamedLambda("no_grouping",
				datasetContext -> new Pregrouping()
				{
					@Override
					public Groups<Group.TentativeGroup> groups()
					{
						return Groups.of(List.of());
					}
					
					@Override
					public Constraint constraint()
					{
						return new Constraint()
						{
							@Override
							public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
							{
							}
							
							@Override
							public String simpleName()
							{
								return "Empty constraint";
							}
						};
					}
				}
		);
	}
	
	/* */
	record NamedLambda(String simpleName, Function<DatasetContext, Pregrouping> function) implements PregroupingType
	{
		@Override
		public Pregrouping instantiateFor(DatasetContext datasetContext)
		{
			return function.apply(datasetContext);
		}
	}
	
}
