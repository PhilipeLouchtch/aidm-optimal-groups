package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives;

import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.model.UtilitarianWeightsObjective;

public class MinimizeSumOfRanks implements ObjectiveFunction
{
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints)
	{
		var obj = new UtilitarianWeightsObjective(assignmentConstraints, rank -> rank);
		obj.apply(model);
	}
	
	@Override
	public String name()
	{
		return "sor";
	}
}
