package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model;

import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints.Constraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public interface Pregrouping
{
	PregroupGroups<Group.TentativeGroup> groups();
	
	Constraint constraint();
	
	class anyClique implements Pregrouping
	{
		private final PregroupGroups<Group.TentativeGroup> groups;
		private final Constraint constraint;
		
		public anyClique(Agents agents, Function<Groups<?>, Constraint> groupingConstraintProvider)
		{
			this.groups = CliqueGroups.from(agents);
			this.constraint = groupingConstraintProvider.apply(this.groups);
		}
		
		@Override
		public PregroupGroups<Group.TentativeGroup> groups()
		{
			return groups;
		}
		
		@Override
		public Constraint constraint()
		{
			return constraint;
		}
	}
	
	class sizedClique implements Pregrouping
	{
		private final PregroupGroups<Group.TentativeGroup> groups;
		private final Constraint constraint;
		
		public sizedClique(Agents agents, Function<Groups<?>, Constraint> groupingConstraintProvider, Integer... sizes)
		{
			this.groups = (PregroupGroups<Group.TentativeGroup>) CliqueGroups.from(agents).ofSizes(sizes);
			this.constraint = groupingConstraintProvider.apply(groups);
		}
		
		@Override
		public PregroupGroups<Group.TentativeGroup> groups()
		{
			return this.groups;
		}
		
		@Override
		public Constraint constraint()
		{
			return this.constraint;
		}
	}
	
	class None implements Pregrouping
	{
		@Override
		public PregroupGroups<Group.TentativeGroup> groups()
		{
			return new PregroupGroups<>(List.of());
		}
		
		@Override
		public Constraint constraint()
		{
			return new Constraint()
			{
				@Override
				public void apply(GRBModel model, AssignmentConstraints assignmentConstraints) throws GRBException
				{
				}
				
				@Override
				public String simpleName()
				{
					return "Empty constraint";
				}
			};
		}
	}
}
