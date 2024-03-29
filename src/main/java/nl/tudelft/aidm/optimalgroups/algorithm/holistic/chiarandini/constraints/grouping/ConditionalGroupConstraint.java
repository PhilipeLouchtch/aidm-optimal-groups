package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping;

import gurobi.*;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.GurobiHelperFns;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import plouchtch.assertion.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The soft-grouping constraint grants the grouping wish or lets the agents go "solo" if grouping is infeasible
 */
public class ConditionalGroupConstraint implements Constraint
{
	public final Collection<GrpLinkedDecisionVar> violateGroupingDecVars;
	private final Groups<?> groups;
	private final int conditionalUpToIncludingRank;
	
	public ConditionalGroupConstraint(Groups<?> groups, int conditionalUpToIncludingRank)
	{
		violateGroupingDecVars = new ArrayList<>();
		
		this.groups = groups;
		this.conditionalUpToIncludingRank = conditionalUpToIncludingRank;
	}
	
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
	{
		groups.forEach(group -> {
			var projPrefs = group.projectPreference();
			var agents = new ArrayList<>(group.members().asCollection());
			var leader = agents.get(0);
			
			// let this be 'g'
			var violateGroupingDecVar = GrpLinkedDecisionVar.make(group, leader.sequenceNumber(), model);
			violateGroupingDecVars.add(violateGroupingDecVar);
			
			// Let 'g' imply that the agents _must_ be assigned one of their top-k choices
			// and implement this by adding constraints only to one of the group's agents, which by transitity will
			// cause all agents in the group to be assigned the same project slot. Because 'g' also implies the agents
			// have the same allocation for their top-k choices.
			var leaderAssignmentsToTopK = new GRBLinExpr();
				
			projPrefs.forEach(((project, rank, __) ->
			{
				// For now, this 'if' the only real difference with soft-group constraint
				if (rank.unacceptable() || rank.asInt() > conditionalUpToIncludingRank)
					return; // continue
				
				project.slots().forEach(slot ->
				{
					// the x's (assignment decision var of agent to project slot) of all agents in group
					var xToSlotVarsAgents = agents.stream().map(agent -> assignmentConstraints.xVars.of(agent, slot))
						.flatMap(Optional::stream)
						.map(AssignmentConstraints.X::asVar)
						.collect(Collectors.toList());
					
					Assert.that(xToSlotVarsAgents.size() == agents.size())
						.orThrowMessage("Could not find all assignment vars for agents in clique to group together...");
					
					try
					{
						var leaderAssVar = xToSlotVarsAgents.get(0);
						leaderAssignmentsToTopK.addTerm(1, leaderAssVar);
						
						for (int i = 1; i < agents.size(); i++)
						{
							var otherAssVar = xToSlotVarsAgents.get(i);
							
						    var lhs = new GRBLinExpr();
						    
						    // Let a desired group be: grp = {s1, s2, s3, s4, s5}
							// Let 'g' be the decision variable for granting the group wish.
							//
							//   Using a binary variable to switch a constraint is not something LP/IP can handle,
							//   luckily, gurobi has a special constraint for this called the indicator constraint that
							//   is switched on/off by an indicator variable. Defined in their docs as "z -> constr".
							
							// To save on the amount of extra constraints added (because the indicator constr is more expensive),
							// the grouping is done as follows:
							//    a "leader" member is quasi-randomly chosen whose project-slot-assignment decision variable (x_leader)
							//    is chosen to be linked to the assignment decision variables of everyone else in the group (x2...xn),
							//    through transitivity this links everyone. Then, these linking constraints are all conditional on the
							//    decision variable (g) of granting the group wish
							
							// Indicator constraint can only handle vars on lhs, so rewrite x_leader = x_2  <==> x_leader - x_2 = 0
							lhs.addTerm(1.0, otherAssVar);
							lhs.addTerm(-1.0, leaderAssVar);
							
							var cnstrName = "cnstr_%s_%d_%s".formatted(violateGroupingDecVar.name, i, slot.id());
							
							// note that g == 0 is indicator for doing the linking, this way we can express not-linking as a big penalty in the objective
							model.addGenConstrIndicator(violateGroupingDecVar.asVar(), 0,
								lhs, '=', 0,
								cnstrName);
						}
					}
					catch (GRBException ex)
					{
						throw new RuntimeException(ex);
					}
				});
				
			}));
			
			try {
				// If the grouping constraint is not violated, then all agents must be assigned together to a top-k choice
				// this is enforced only by ensuring so for only one of the agents, the so called 'leader'. By the other indicator
				// constraint, the other member's assignment variables are linked to those of the leader, forcing whole group
				// to have the same assignment iff (two-way is forced by the penalty to the obj function) the decision variable
				// 'violate grouping' is false (not violated, or alternatively: the decision is made to assigned the group together
				// to one of their top-k choices)
				var constName = violateGroupingDecVar.name() + "_force_topk";
				model.addGenConstrIndicator(violateGroupingDecVar.asVar(), 0,
					leaderAssignmentsToTopK, '=', 1,
					constName);
			}
			catch (GRBException ex)
			{
				throw new RuntimeException(ex);
			}
		});
		
		var objective = new GRBLinExpr((GRBLinExpr) model.getObjective());
		
		var ogSize = objective.size();
		Assert.that(objective.size() > 0).orThrowMessage("Objective function must be set before adding the Soft Grouping constraint");
		
		violateGroupingDecVars.forEach(violateGroupingDecVar -> {
			// Hefty penalty if the group is unlinked (var == 1)
			objective.addTerm(100000, violateGroupingDecVar.asVar());
		});
		
		model.setObjective(objective, GRB.MINIMIZE);
		model.update();
		
		var newSize = ((GRBLinExpr) model.getObjective()).size();
		Assert.that(newSize == ogSize + violateGroupingDecVars.size()).orThrowMessage("Objective did not get updated");
	}
	
	@Override
	public String simpleName()
	{
		return "condgrps";
	}
	
	/**
	 * The grouping decision variable
	 */
	public record GrpLinkedDecisionVar(Group group, GRBVar asVar, String name)
	{
		static GrpLinkedDecisionVar make(Group group, Integer grpIdx, GRBModel model)
		{
			String name = "condlink_g" + grpIdx;
			var softGrpVar = GurobiHelperFns.makeBinaryVariable(model, name);
			return new GrpLinkedDecisionVar(group, softGrpVar, name);
		}
	}
}
