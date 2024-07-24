package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints;

import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import plouchtch.assertion.Assert;

public class DistributeIndiffStudentsProperlyConstraint implements Constraint
{
	private final Agents indiffAgents;
	private final Projects projects;
	private int k;
	
	public static DistributeIndiffStudentsProperlyConstraint fromDatasetContext(DatasetContext datasetContext)
	{
		var indiffAgents = datasetContext.allAgents().asCollection().stream()
		                                 .filter(agent -> agent.projectPreference().isCompletelyIndifferent())
			                             .collect(Agents.collector);
		
		int k = calc_k(datasetContext, indiffAgents);
		
		return new DistributeIndiffStudentsProperlyConstraint(indiffAgents, datasetContext.allProjects(), k);
	}
	
	public DistributeIndiffStudentsProperlyConstraint(Agents indiffAgents, Projects projects, int k)
	{
		Assert.that(k > 0).orThrowMessage("Assert failed: k > 0, was: %s".formatted(k));
		
		this.indiffAgents = indiffAgents;
		this.projects = projects;
		this.k = k;
	}
	
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
	{
		if (indiffAgents.count() == 0)
			return;
		
		for (var project : projects.asCollection())
        {
			for (var slot : project.slots())
            {
				var assignmentDecisionVarsProjectSlot = indiffAgents.asCollection().stream()
				                                                 .map(agent -> assignmentConstraints.xVars.of(agent, slot).orElseThrow())
				                                                 .toList();
				
				// We're constructing a constraint: x_a1s1 + x_a2s1 + ... x_ais1 < k
				// where x_aisj is the decision variable of assigning student i to projectslot j
				
				var expr = new GRBLinExpr();
				
				// adding all the decision variables of assigning the indiff student to the current project-slot together
				// becoming: `x_s1 + x_s2 + ... x_si`
				assignmentDecisionVarsProjectSlot.forEach( xVar -> expr.addTerm(1, xVar.asVar()) );
				
				// add the final constraint for limiting num indifferent students per project-slot to k
				model.addConstr(expr, '<', k, "distrib_indiff_pslot_"+slot.id());
			}
		}
	}
	
	@Override
	public String simpleName()
	{
		return "distrib_indiff_studs_properly_ctrs";
	}
	
	private static int calc_k(DatasetContext datasetContext, Agents indiffAgents)
	{
		var n = datasetContext.allAgents().count() * 1.0;
		var n_indiff = indiffAgents.count() * 1.0;
		var g_max = datasetContext.groupSizeConstraint().maxSize() * 1.0;
		
		var min_groups = Math.ceil(n / g_max);
		
		int k = (int) Math.ceil( (n-n_indiff) / min_groups );
		return k;
	}
}
