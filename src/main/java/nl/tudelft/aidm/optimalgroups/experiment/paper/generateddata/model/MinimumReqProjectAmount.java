package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import gurobi.*;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import plouchtch.assertion.Assert;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public record MinimumReqProjectAmount(GroupSizeConstraint gsc, int numberOfStudents)
{
	record Combination(GroupSizeConstraint gsc, int numStudents) {}
	private final static Map<Combination, Integer> computationCache = new HashMap<>();
	
	public Integer asInt()
	{
		Combination key = new Combination(gsc, numberOfStudents);
		return computationCache.computeIfAbsent(key, MinimumReqProjectAmount::calcOrDie);
	}
	
	private static Integer calcOrDie(Combination combination)
	{
		try {
			return tryCalc(combination);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		catch (Error err) {
			throw new RuntimeException("Error occurred and I was able to handle this thing (but not completely, TODO for OOM or Gurobi stuff)", err);
		}
	}

	private static Integer tryCalc(Combination combination) throws GRBException
	{
		var numberOfStudents = combination.numStudents;
		var gsc = combination.gsc;
		
		var env = new GRBEnv(true);
		
		// silent solving
		env.set(GRB.IntParam.OutputFlag, 0);
		
		// NOTE: Single thread for benchmarking!
		env.set(GRB.IntParam.Threads, 1);
		
		env.start();

		var model = new GRBModel(env);
		
		// Contants: group size bounds, e.g. 4, 5, 6 if bounds between 4 and 6 (incl)
		// Variables: number of groups per size,
		// Optimize: min sum (number of groups)
		
		var allowedGroupSizesVars = new ArrayList<GRBVar>(gsc.maxSize() - gsc.minSize());
		var numGroupsOfSizeVars = new GRBVar[gsc.maxSize() + 1];
		
		var objective = new GRBLinExpr();
		var numStudentsInGroupsSum = new GRBLinExpr();
		for (int x = gsc.minSize(); x <= gsc.maxSize(); x++)
		{
			var numGroupsOfSizeXVar = model.addVar(0, numberOfStudents / x, 0, GRB.INTEGER, "var_grps_of_" + x);
			numGroupsOfSizeVars[x] = numGroupsOfSizeXVar;
			
			objective.addTerm(1, numGroupsOfSizeXVar);
			numStudentsInGroupsSum.addTerm(x, numGroupsOfSizeXVar);
		}
		model.addConstr(numStudentsInGroupsSum, GRB.EQUAL, numberOfStudents, "constr_all_students_grouped");
		model.setObjective(objective, GRB.MINIMIZE);
		
		model.optimize();
		
		var status = model.get(GRB.IntAttr.Status);
		
		Assert.that(status == GRB.OPTIMAL).orThrowMessage("Model not solved");

		Integer result = IntStream.rangeClosed(gsc.minSize(), gsc.maxSize())
				                 .mapToDouble(groupSize -> Try.getting(() -> numGroupsOfSizeVars[groupSize].get(GRB.DoubleAttr.X))
				                                              .or(Rethrow.asRuntime()))
				                 .mapToLong(Math::round)
				                 .mapToInt(value -> (int) value)
				                 .sum();
		
		// Gurobi manual: dispose any and all used models and then dispose the env
		model.dispose();
		env.dispose();

		return result;
	}
}
