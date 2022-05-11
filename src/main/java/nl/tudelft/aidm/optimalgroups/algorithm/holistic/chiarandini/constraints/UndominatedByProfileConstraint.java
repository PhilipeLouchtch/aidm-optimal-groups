package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import plouchtch.assertion.Assert;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Constraint that the solution for (a subset of) students must not be dominated by the given profile
 */
public class UndominatedByProfileConstraint implements Constraint
{
	private final Profile profile;
	private final Agents undominatedAgents;
	
	public UndominatedByProfileConstraint(Profile profile, Agents undominatedAgents)
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
		
		var cumsumInProfileUpToRankH = new AtomicInteger(0); // needs to be effectively final for lambda's
		
		var cumsumStudentsUpToRankH = new GRBLinExpr();
		
		// TODO: try improve performance by introducing intermediary constraints
		
		for (var i = new AtomicInteger(1); i.get() <= profile.maxRank(); i.incrementAndGet())
		{
			int h = i.getPlain();
			
			undominatedAgents.forEach(agent -> {
				agent.projectPreference().forEach((project, rank, __) -> {
					project.slots().forEach(slot ->
					{
						// Agent is not indiff and finds project acceptable
						// AND ranks project at h
						if (!rank.isCompletelyIndifferent() && !rank.unacceptable() && rank.asInt() == h) {
							var x = assignmentConstraints.xVars.of(agent, slot).orElseThrow();
							cumsumStudentsUpToRankH.addTerm(1, x.asVar());
						}
					});
					
				});
			});
			
			// cumsumInProfileUpToRankH += profile.numInRank(h)
			var with_h_in_profile = profile.numAgentsWithRank(h);
			cumsumInProfileUpToRankH.addAndGet(with_h_in_profile);
			
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
