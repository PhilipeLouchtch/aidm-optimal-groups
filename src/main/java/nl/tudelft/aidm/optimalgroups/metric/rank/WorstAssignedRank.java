package nl.tudelft.aidm.optimalgroups.metric.rank;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

public interface WorstAssignedRank extends Comparable<WorstAssignedRank>
{
	Integer asInt();

	@Override
	default int compareTo(@NotNull WorstAssignedRank o)
	{
		return asInt().compareTo(o.asInt());
	}

	class ProjectToGroup implements WorstAssignedRank
	{
		private final Matching<? extends Group, Project> matching;
		private Integer asInt = null;

		public ProjectToGroup(Matching<? extends Group, Project> matching)
		{
			this.matching = matching;
		}

		public Integer asInt()
		{
			if (asInt == null) {
				asInt = calculate();
			}

			return asInt;
		}

		private Integer calculate()
		{
			var worst = AssignedRank.ProjectToGroup.groupRanks(matching)
				.map(AssignedRank.ProjectToGroup::asInt)
				.filter(OptionalInt::isPresent)
				.mapToInt(OptionalInt::getAsInt)
				.max().orElseThrow();

			return worst;
		}
		
		@Override
		public String toString()
		{
			return String.valueOf(asInt());
		}
	}

	class ProjectToStudents implements WorstAssignedRank
	{
		private final Matching<Agent, Project> matching;
		private Integer asInt = null;

		public ProjectToStudents(Matching<Agent, Project> matching)
		{
			this.matching = matching;
		}
		public static WorstAssignedRank in(Matching<Agent, Project> matching)
		{
			return new ProjectToStudents(matching);
		}

		public Integer asInt()
		{
			if (asInt == null) {
				asInt = calculate();
			}

			return asInt;
		}

		private Integer calculate()
		{
			var worst = AssignedRank.ProjectToStudent.inStudentMatching(matching)
				.map(AssignedRank.ProjectToStudent::asInt)
				.filter(OptionalInt::isPresent)
				.mapToInt(OptionalInt::getAsInt)
				.max()
				.orElse(0); // all are indifferent

			return worst;
		}
		
		@Override
		public String toString()
		{
			return String.valueOf(asInt());
		}
	}
}
