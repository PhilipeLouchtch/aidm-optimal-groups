package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.grouping;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.util.ArrayList;

public record HardGroupingConstraint(Groups<?> groups) implements Constraint
{
	@Override
	public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
	{
		groups.forEach(group -> {
			var projPrefs = group.projectPreference();
			var agents = new ArrayList<>(group.members().asCollection());
			
			// transitory constraints, if Xa = Xb and Xb = Xc and Xc = Xd imply Xa = Xd, Xa = Xc etc
			//   so we set contraints that first student in group must have same assignment as the second,
			//   the second same as the third etc. This suffices to ensure that all these students in a group
			//   get the same assignment.
			
			for (int i = 0; i < agents.size() - 1; i++)
			{
				var agent1 = agents.get(i);
				var agent2 = agents.get(i + 1);
				
				// For each acceptible project to the group, link the members' assignment decision variables
				// (note: these preferences are aggregated from the individual members, double check the handling of unacceptible
				// alternatives. If some members have deemed a project unacceptible and others acceptible, is the project acceptible
				// or unacceptible in the aggregated preferences? Can a single student in a group veto a project? Where's the line?)
				// TODO, incomplete preferences - unacceptible alternatives (see comment CliqueGroups#cliquesExtractedFrom)
				projPrefs.forEach(((project, rank, __) -> {
					project.slots().forEach(slot -> {
						
						assignmentConstraints.xVars.of(agent1, slot).ifPresent(Xa1ToSlot -> {
							assignmentConstraints.xVars.of(agent2, slot).ifPresent(Xa2ToSlot -> {
								Try.doing(() ->
								          model.addConstr(Xa1ToSlot.asVar(), '=', Xa2ToSlot.asVar(), "cnstr_grp_" + agent1.sequenceNumber() + "_" + agent2.sequenceNumber() + "_to_" + slot)
								).or(Rethrow.asRuntime());
							});
						});
						
					});
					
				}));
			}
		});
	}
	
	@Override
	public String simpleName()
	{
		return "hardgrp";
	}
}
