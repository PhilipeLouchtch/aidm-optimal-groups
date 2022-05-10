package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.GurobiHelperFns;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import plouchtch.assertion.Assert;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Constraint that the solution for (a subset of) students must not be dominated by the given profile
 *
 * Improved version (slightly): introduce additional variable/constraints that represents number of 'solo' students assinged to a rank
 * and use it in the cumsum constraints rather than summing decision variables of all 'solo' students for the necessary ranks. Experiements
 * have shown this version to have better outlier runtimes compared to the not-improved version.
 */
public class UndominatedByProfileConstraintImproved implements Constraint
{
	private final Profile profile;
	private final Agents undominatedAgents;
	
	public UndominatedByProfileConstraintImproved(Profile profile, Agents undominatedAgents)
	{
		Assert.that(profile.numAgents() == undominatedAgents.count())
			.orThrowMessage("Given profile is not fit to use with given set of agents (different sizes)");
		
		this.profile = profile;
		this.undominatedAgents = undominatedAgents;
	}
	
	
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
	{
		// Copied and modified from DistributiveWeightsObjective
		
		// Variables need to be "effectively final" for lambda functions
		// So this is an integer wrapped in an effectively final object
		var cumsumInProfileUpToRankH = new AtomicInteger(0);
		
		var cumsumStudentsUpToRankH = new GRBLinExpr();
		
		
		for (var i = 1; i <= profile.maxRank(); i++)
		{
			int h = i;
			
			// Here = h = i
			var numAgentsWithRankH = GurobiHelperFns.makeIntegerVariable(model, 0, profile.numAgents(), "num_withrank_" + i);
			var numAgentsWithRankHExpr = new GRBLinExpr();
			
			undominatedAgents.forEach(agent -> {
				agent.projectPreference().forEach((project, rank, __) -> {
					project.slots().forEach(slot ->
					{
						// Agent is not indiff, finds project acceptable
						// AND ranks project at h
						if (!rank.isCompletelyIndifferent() && !rank.unacceptable() && rank.asInt() == h) {
							var x = assignmentConstraints.xVars.of(agent, slot).orElseThrow();
							numAgentsWithRankHExpr.addTerm(1, x.asVar());
						}
					});
					
				});
			});
			
			model.addConstr(numAgentsWithRankHExpr, '=', numAgentsWithRankH, "num_withrank_%s_cnstr".formatted(i));
			
			cumsumStudentsUpToRankH.addTerm(1, numAgentsWithRankH);
			
			// cumsumInProfileUpToRankH += profile.numInRank(h)
			var with_h_in_profile = profile.numAgentsWithRank(h);
			cumsumInProfileUpToRankH.addAndGet(with_h_in_profile);
			
			// Add the part up-to i to the model
			// Gurobi manual: Once you add a constraint to your model, subsequent changes to the expression object you used to build the constraint will not change the constraint
			Try.doing(() ->
					model.addConstr(cumsumStudentsUpToRankH, GRB.GREATER_EQUAL, cumsumInProfileUpToRankH.getPlain(), "const_leximin_" + h)
			).or(Rethrow.asRuntime());
			
		}
	}
	
	@Override
	public String simpleName()
	{
		return "undom";
	}
}
