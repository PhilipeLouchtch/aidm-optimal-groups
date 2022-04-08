package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ChiarandiniGroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import plouchtch.assertion.Assert;

public record ChiarandiniBaseModel(DatasetContext datasetContext, ObjectiveFunction objectiveFunction, Constraint... constraints)
{
	public GroupToProjectMatching<Group.FormedGroup> doIt()
	{
		try {
			return doItDirty();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		catch (Error err) {
			throw new RuntimeException("Error occurred and I was able to handle this thing (but not completely, TODO for OOM or Gurobi stuff)", err);
		}
	}

	private GroupToProjectMatching<Group.FormedGroup> doItDirty() throws GRBException
	{
		var env = new GRBEnv(true);
		
		// silent solving
		env.set(GRB.IntParam.OutputFlag, 0);
		
		// NOTE: Single thread for benchmarking!
		env.set(GRB.IntParam.Threads, 1);
		
		env.start();

		var model = new GRBModel(env);

		AssignmentConstraints assignmentConstraints = AssignmentConstraints.createInModel(model, datasetContext);
		
		objectiveFunction.apply(model, assignmentConstraints);

		// Apply all constraints
		for (Constraint constraint : constraints)
		{
			model.update();
			constraint.apply(model, assignmentConstraints);
		}

		model.optimize();
		
		var status = model.get(GRB.IntAttr.Status);
		
		Assert.that(status == GRB.OPTIMAL).orThrowMessage("Model not solved");

		// extract x's and map to matching
		var matching = new ChiarandiniGroupToProjectMatching(assignmentConstraints.xVars, datasetContext);

		// Gurobi manual: dispose any and all used models and then dispose the env
		model.dispose();
		env.dispose();

		return matching;
	}
}
