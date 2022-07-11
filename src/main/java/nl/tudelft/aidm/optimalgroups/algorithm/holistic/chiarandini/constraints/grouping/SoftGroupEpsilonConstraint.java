package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping;

import gurobi.*;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.EpsilonConstraint;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.GurobiHelperFns;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import plouchtch.assertion.Assert;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The soft-grouping constraint grants the grouping wish or lets the agents go "solo" if grouping is infeasible
 */
public class SoftGroupEpsilonConstraint implements EpsilonConstraint
{
	public final Collection<GrpLinkedDecisionVar> violateGroupingDecVars;
	private final Groups<?> groups;
	private int epsionValue;
	
	public SoftGroupEpsilonConstraint(Groups<?> groups)
	{
		this.groups = groups;
		this.violateGroupingDecVars = new ArrayList<>();
		
		// Default value: all pregroups must be together together
		this.epsionValue = groups.count();
	}
	
	/**
	 * For SoftPregrouping, epsilon = the least number of groups that _must_ be together.
	 * Or in other words, epsilon is how many pregroupings may be disbanded. <b>Except for
	 * the 0-case, then we skip pregrouping altogether!
	 * @param epsilon
	 */
	@Override
	public void setEpsilon(int epsilon)
	{
		this.epsionValue = epsilon;
	}
	
	@Override
	public int getEpsilon()
	{
		return epsionValue;
	}
	
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
	{
		// Workaround: if epsilon got to 0, the model could still remain 'infeasible' which is odd.
		// so if we reach epsilon = 0, don't apply any grouping constraints, they'd be useless
		// anyway with epslon set to 0
		if (epsionValue == 0)
			return;
		
		groups.forEach(group -> {
			var projPrefs = group.projectPreference();
			var agents = new ArrayList<>(group.members().asCollection());
			var leaderId = agents.get(0).sequenceNumber();
			
			// let this be 'g'
			var violateGroupingDecVar = GrpLinkedDecisionVar.make(group, leaderId, model);
			violateGroupingDecVars.add(violateGroupingDecVar);
			
			
			// For each acceptible project to the group, link the members' assignment decision variables
			// (note: these preferences are aggregated from the individual members, double check the handling of unacceptible
			// alternatives. If some members have deemed a project unacceptible and others acceptible, is the project acceptible
			// or unacceptible in the aggregated preferences? Can a single student in a group veto a project? Where's the line?)
			// TODO, incomplete preferences - unacceptible alternatives (see comment CliqueGroups#cliquesExtractedFrom)
			projPrefs.forEach(((project, rank, __) -> {
				project.slots().forEach(slot -> {
					
					// the x's (assignment decision var) of all agents in group for assignment to project slot 'slot'
					var xToSlotVarsAgents = agents.stream().map(agent -> assignmentConstraints.xVars.of(agent, slot))
						.flatMap(Optional::stream)
						.map(AssignmentConstraints.X::asVar)
						.collect(Collectors.toList());
					
					Assert.that(xToSlotVarsAgents.size() == agents.size())
						.orThrowMessage("Could not find all assignment vars for agents in clique to group together...");
					
					var leaderAssVar = xToSlotVarsAgents.get(0);
					for (int i = 1; i < agents.size(); i++)
					{
					    
					    // Let a desired group be: grp = {s1, s2, s3, s4, s5}
						// Let 'g' be the decision variable for granting the group wish.
						//
						//   Using a binary variable to switch a constraint is not something LP/IP can handle,
						//   luckily, gurobi has a special constraint for this called the indicator constraint that
						//   is switched on/off by an indicator variable. Defined in their docs as "z -> constr".
						
						// To save on the amount of extra constraints added (because the indicator constr is more expensive),
						// the grouping is done as follows:
						//    a "leader" member is randomly chosen whose project-slot-assignment decision variable (x_leader)
						//    is chosen to be linked to the assignment decision variables of everyone else in the group (x2...xn),
						//    through transitivity this links everyone. Then, these linking constraints are all conditional on the
						//    decision variable (g) of granting the group wish
						
						var lhs = new GRBLinExpr();
						var otherAssVar = xToSlotVarsAgents.get(i);
						
						// Indicator constraint can only handle vars on lhs, so rewrite x_leader = x_2  <==> x_leader - x_2 = 0
						lhs.addTerm(1.0, otherAssVar);
						lhs.addTerm(-1.0, leaderAssVar);
						
						var constName = "cnstr_%s_%d_%s".formatted(violateGroupingDecVar.name, i, slot.id());
						
						// note that g == 0 is indicator for doing the linking, this way we can express not-linking as a big penalty in the objective
						Try.doing(() -> model.addGenConstrIndicator(violateGroupingDecVar.asVar(), 0,
						                                            lhs, '=', 0,
						                                            constName)
						).or(Rethrow.asRuntime());
					}
				});
				
			}));
		});
		
		// add epsilon constraint
		
		var total_num_pregroupings = groups.count();
		var sumViolations = new GRBLinExpr();
		violateGroupingDecVars.forEach(violation -> sumViolations.addTerm(1, violation.asVar()));
		
		model.addConstr(sumViolations, GRB.LESS_EQUAL, total_num_pregroupings - epsionValue, "cnst_softpregroup_epsilon");
		
		model.update();
	}
	
	@Override
	public String simpleName()
	{
		return "softgrps_e";
	}
	
	public record GrpLinkedDecisionVar(Group group, GRBVar asVar, String name)
	{
		static GrpLinkedDecisionVar make(Group group, Integer grpIdx, GRBModel model)
		{
			String name = "link_g" + grpIdx;
			var softGrpVar = GurobiHelperFns.makeBinaryVariable(model, name);
			return new GrpLinkedDecisionVar(group, softGrpVar, name);
		}
	}
}
