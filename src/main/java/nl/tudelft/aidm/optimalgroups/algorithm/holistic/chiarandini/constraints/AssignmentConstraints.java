package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints;

import gurobi.*;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.*;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.util.Optional;
import java.util.function.Function;

public class AssignmentConstraints
{
	public final DatasetContext datasetContext;
	
	public final XVars xVars;
	public final YVars yVars;

	private AssignmentConstraints(DatasetContext datasetContext, XVars x, YVars y)
	{
		this.datasetContext = datasetContext;
		this.xVars = x;
		this.yVars = y;
	}

	public static AssignmentConstraints createInModel(GRBModel model, DatasetContext datasetContext) throws GRBException
	{
		// Hack to support SDU/Chirandini datasets
		Function<Project, GroupSizeConstraint> gscProvider =
				switch (datasetContext)
				{
                     case SDUDatasetContext sdu -> sdu::groupSizeBoundsOf;
                     default -> p -> datasetContext.groupSizeConstraint();
                };
		
		var agents = datasetContext.allAgents();
		var projects = datasetContext.allProjects();

		XVars xVars = XVars.createInModel(model, datasetContext);
		YVars yVars = YVars.createInModel(model, datasetContext);

		// CONSTRAINTS

		// - student is assigned to exactly one project
		agents.forEach(agent ->
		{
			var numSlotsAssignedToAgentExpr = new GRBLinExpr();
			var s = agent.sequenceNumber();

			projects.forEach(project ->
			{
				project.slots().forEach(slot ->
				{
					// note: agent-project pair might be unacceptible -> x is not present
					xVars.of(agent, slot).ifPresent(x ->
						numSlotsAssignedToAgentExpr.addTerm(1, x.asVar())
					);
				});
			});

			String name = "s_" + agent + "assignedToOneProjCnstr";
			Try.doing(() -> model.addConstr(numSlotsAssignedToAgentExpr, GRB.EQUAL, 1, name))
				.or(Rethrow.asRuntime());
		});

		// - a project-slot is either unassigned or meets its group-size requirements:
		projects.forEach(project ->
		{
			var groupSizeBound = gscProvider.apply(project);
			var p = project.sequenceNum();

			project.slots().forEach(slot ->
			{
				var sl = slot.index();
				var numStudentsAssignedToSlot = new GRBLinExpr();

				agents.forEach(agent ->
				{
					// note: agent-project pair might be unacceptible -> x is not present
					xVars.of(agent, slot).ifPresent(x ->
							numStudentsAssignedToSlot.addTerm(1, x.asVar())
					);
				});

				var y = yVars.of(slot).orElseThrow();
				
				var constraintNameUb = String.format("%s_doesnt_exceed_ub", slot);
				var ub = new GRBLinExpr();
				ub.addTerm(groupSizeBound.maxSize(), y.asVar());
				// numAssignedToSlot <= UB * y
				Try.doing(() -> model.addConstr(numStudentsAssignedToSlot, GRB.LESS_EQUAL, ub, constraintNameUb))
					.or(Rethrow.asRuntime());
				
				
				var constraintNameLb = String.format("%s_meets_lb", slot);
				// numAssignedToSlot >= LB * y
				var lb = new GRBLinExpr();
				lb.addTerm(groupSizeBound.minSize(), y.asVar());
				Try.doing(() -> model.addConstr(numStudentsAssignedToSlot, GRB.GREATER_EQUAL, lb, constraintNameLb))
					.or(Rethrow.asRuntime());
			});
		});

		return new AssignmentConstraints(datasetContext, xVars, yVars);
	}
	
	public static record X(Agent student, Project project, Project.ProjectSlot slot, String name, GRBVar asVar)
	{
		public static X createInModel(Agent student, Project project, Project.ProjectSlot slot, GRBModel model)
		{
			var name = String.format("x[%s, %s]", student, slot);
			var grbVar = GurobiHelperFns.makeBinaryVariable(model, name);
	
			return new X(student, project, slot, name, grbVar);
		}
	
		public double getValueOrThrow()
		{
			return Try.getting(() -> this.asVar().get(GRB.DoubleAttr.X))
				.or(Rethrow.asRuntime());
		}
	
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public static class XVars
	{
		private final X[][][] x;
		private final DatasetContext datasetContext;
	
		public static XVars createInModel(GRBModel model, DatasetContext datasetContext)
		{
			var maxAgentSeqNum = datasetContext.allAgents().asCollection().stream().mapToInt(value -> value.sequenceNumber()).max().orElseThrow();
			var maxProjectSeqNum = datasetContext.allProjects().asCollection().stream().mapToInt(Project::sequenceNum).max().orElseThrow();
			var maxSlots = datasetContext.numMaxSlots();
	
			var x = new X[maxAgentSeqNum+1][maxProjectSeqNum+1][maxSlots];
	
			// Create the X variables (student assigned to slot)
			datasetContext.allAgents().forEach(student ->
			{
				var s = student.sequenceNumber();
	
				// hacky: seqentualization does properly handles the varios pref profile types, so workaround
				if (student.projectPreference().isCompletelyIndifferent())
				{
					datasetContext.allProjects().forEach(project ->
					{
						var p = project.sequenceNum();
						project.slots().forEach(slot ->
						{
							int sl = slot.index();
							x[s][p][sl] = X.createInModel(student, project, slot, model);
						});
					});
				}
				else student.projectPreference().forEach((project, rank, __) ->
				{
					// If the project is not acceptible to agent, then it cannot be assigned to them.
					// Therefore, no assignment decision variable must be created for this pair.
					if (rank.unacceptable())
						return;
					
					var p = project.sequenceNum();
					project.slots().forEach(slot ->
					{
						int sl = slot.index();
						x[s][p][sl] = X.createInModel(student, project, slot, model);
					});
				});
			});
	
			return new XVars(x, datasetContext);
		}
	
		private XVars(X[][][] x, DatasetContext datasetContext)
		{
			this.x = x;
			this.datasetContext = datasetContext;
		}
	
		public Optional<X> of(Agent agent, Project.ProjectSlot slot)
		{
			return Optional.ofNullable(
				x[agent.sequenceNumber()][slot.belongingToProject().sequenceNum()][slot.index()]
			);
		}
	}
	
	public static record Y(Project.ProjectSlot project, String name, GRBVar asVar)
	{
		public static Y createInModel(Project.ProjectSlot slot, GRBModel model)
		{
			var name = String.format("y[%s]", slot.toString());
			var grbVar = GurobiHelperFns.makeBinaryVariable(model, name);
	
			return new Y(slot, name, grbVar);
		}
	}
	
	public static class YVars
	{
		private final Y[][] y;
		private final DatasetContext datasetContext;
	
		public static YVars createInModel(GRBModel model, DatasetContext datasetContext)
		{
			var numProjects = datasetContext.allProjects().count();
			var maxSlots = datasetContext.numMaxSlots();
	
			var y = new Y[numProjects+1][maxSlots];
	
			// Create Y variables (project-slot is open)
			datasetContext.allProjects().forEach(project ->
			{
				int p = project.sequenceNum();
				project.slots().forEach(slot -> {
					var sl = slot.index();
					y[p][sl] = Y.createInModel(slot, model);
				});
			});
	
			return new YVars(y, datasetContext);
		}
	
		public YVars(Y[][] y, DatasetContext datasetContext)
		{
			this.y = y;
			this.datasetContext = datasetContext;
		}
		public Optional<Y> of(Project.ProjectSlot slot)
		{
			// Todo: check if within bounds
	
			return Optional.ofNullable(
				y[slot.belongingToProject().sequenceNum()][slot.index()]
			);
		}
	}
}
