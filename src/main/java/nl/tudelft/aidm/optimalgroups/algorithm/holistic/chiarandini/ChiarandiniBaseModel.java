package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.EpsilonConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.epsiloncnstr.EpsilonSearchLinear;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ChiarandiniGroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import plouchtch.assertion.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

public record ChiarandiniBaseModel(DatasetContext datasetContext, ObjectiveFunction objectiveFunction, Constraint... constraints)
{
	public static class InfeasbileMatchingException extends RuntimeException {	}
	
	public GroupToProjectMatching<Group.FormedGroup> doIt()
	{
		try {
			return doItDirty();
		}
		catch (InfeasbileMatchingException ex) {
			// Do not process the "Infeasible" exception (see code below)
			throw ex;
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
		
		try
		{
			var num_epsilon_constraints = Arrays.stream(constraints).filter(c -> c instanceof EpsilonConstraint).count();
			
			if (epsilonConstraintPresent()) {
				// epsilon solver version
				var matching = buildAndSolveModelEpsilon(env);
				return matching;
			}
			else {
				var matching = buildAndSolveModel(env);
				return matching;
			}
			
		}
		finally
		{
			// Gurobi manual: dispose any and all used models and then dispose the env
			// The buildAndSolve may throw an exception, in which case we still need to clean up and the model & env
			// to prevent OOM's in long-running instances (such as during benchmarking or always-on microservices etc)
			model.dispose();
			env.dispose();
		}
	}

	private GroupToProjectMatching<Group.FormedGroup> buildAndSolveModel(GRBEnv env) throws GRBException
	{
		var model = new GRBModel(env);
		
		try
		{
			AssignmentConstraints assignmentConstraints = AssignmentConstraints.createInModel(model, datasetContext);
			
			objectiveFunction.apply(model, assignmentConstraints);
			model.update();
			
			// Apply all constraints
			for (Constraint constraint : constraints)
			{
				constraint.apply(model, assignmentConstraints);
				model.update();
			}
			
			// Solve model
			model.optimize();
			
			// Check status of solving the model
			var status = model.get(GRB.IntAttr.Status);
			
			if (status == GRB.INFEASIBLE)
			{
				// Debug infeasibility
				//			model.computeIIS();
				//			model.write("model.ilp");
				
				// HACK: Dirty hack to signal the epsilon-constraint soft-grouping approach that the epsilon value is infeasible
				// Idea for improvement: return a monad containing status and corresponding potential result
				throw new InfeasbileMatchingException();
			}
			
			Assert.that(status == GRB.OPTIMAL).orThrowMessage("Model not solved");
			
			// extract x's and map to matching
			var matching = new ChiarandiniGroupToProjectMatching(assignmentConstraints.xVars, datasetContext);
			
			return matching;
		}
		finally
		{
			model.dispose();
		}
	}
	
	public GroupToProjectMatching<Group.FormedGroup> buildAndSolveModelEpsilon(GRBEnv env) throws GRBException
	{
		var x = determineEpsilonConstraints().stream().findFirst().orElseThrow();
		
		/* EPSILON SEARCH */
		enum SolutionStatus { FEASIBLE, INFEASIBLE }
		record EpsilonResult(SolutionStatus outcome, GroupToProjectMatching<Group.FormedGroup> result) {}
		
		interface ThrowingFunction<A,B,Ex extends Exception> { B apply(A a) throws Ex;}
		
		ThrowingFunction<Integer, EpsilonResult, GRBException> findMatchingWithGivenEpsilon = (Integer epsilon) -> {
			try {
				var result = buildAndSolveModel(env);
				return new EpsilonResult(SolutionStatus.FEASIBLE, result);
			}
			catch (ChiarandiniBaseModel.InfeasbileMatchingException ex) {
				Assert.that(epsilon > 0).orThrowMessage("Epsilon 0 is infeasible??");
				return new EpsilonResult(SolutionStatus.INFEASIBLE, null);
			}
		};
		
		var outcomesForEpsilon = new EpsilonResult[x.getEpsilon() + 1];
		ThrowingFunction<Integer, EpsilonResult, GRBException> findMatchingWithGivenEpsilonCached = epsilon -> {
			var result = outcomesForEpsilon[epsilon];
			
			if (result == null) {
				result = findMatchingWithGivenEpsilon.apply(epsilon);
				outcomesForEpsilon[epsilon] = result;
			}
			
			return result;
		};
		
		// Often, there are only a few pregroupings not possible, thus linear search has better best-case performance
		// for binary search, use this:
		//      var epsilonSearch = new EpsilonSearchBinary(x.getEpsilon());
		var epsilonSearch = new EpsilonSearchLinear(x.getEpsilon());
		var epsilon = epsilonSearch.initial();
		
		while (true) {
			x.setEpsilon(epsilon);
			var result = findMatchingWithGivenEpsilonCached.apply(epsilon);
			
			var nextEpsilon = epsilonSearch.next(result.outcome == SolutionStatus.FEASIBLE);
			
			if (nextEpsilon == epsilon)
				break;
			else
				epsilon = nextEpsilon;
		}
		
		var epsilonResult = findMatchingWithGivenEpsilonCached.apply(epsilon);
		Assert.that(epsilonResult.outcome == SolutionStatus.FEASIBLE).orThrowMessage("BUGCHECK: Infeasible matching?");
		
		return epsilonResult.result();
	}
	
	private boolean epsilonConstraintPresent()
	{
		var count = determineEpsilonConstraints().size();
		
		Assert.that(count <= 1).orThrowMessage("Only a single epsilon constraint is supported, found %s".formatted(count));
		
		return count >= 1;
	}
	
	private List<EpsilonConstraint> determineEpsilonConstraints()
	{
		synchronized (epsilonConstraintsCache) {
			return epsilonConstraintsCache.computeIfAbsent(this, instance ->
				Arrays.stream(instance.constraints())
				      .filter(c -> c instanceof EpsilonConstraint)
				      .map(c -> (EpsilonConstraint) c)
				      .toList()
			);
		}
	}
	
	// workaround for not having private fields in records
	private static final WeakHashMap<ChiarandiniBaseModel, List<EpsilonConstraint>> epsilonConstraintsCache = new WeakHashMap<>();
}
