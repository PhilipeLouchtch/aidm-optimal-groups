package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives;

import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.model.UtilitarianWeightsObjective;

public class MinimizeSumOfExpRanks implements ObjectiveFunction
{
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints)
	{
		var maxRank = assignmentConstraints.datasetContext.worstRank().orElse(0);
		
		UtilitarianWeightsObjective.WeightScheme weightScheme = rank -> -1 * Math.pow(2, Math.max(0, maxRank - rank));
		
		var obj = new UtilitarianWeightsObjective(assignmentConstraints, weightScheme);
        obj.apply(model);
	}
	
	@Override
	public String name()
	{
		return "sor-exp";
	}
}
