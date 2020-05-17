package nl.tudelft.aidm.optimalgroups.metric.rank;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.OptionalInt;

public interface WorstAssignedRank
{
	Integer asInt();

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
	}

	class ProjectToStudents implements WorstAssignedRank
	{
		private final Matching<Agent, Project> matching;
		private Integer asInt = null;

		public ProjectToStudents(Matching<Agent, Project> matching)
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
			var worst = AssignedRank.ProjectToStudent.inStudentMatching(matching)
				.map(AssignedRank.ProjectToStudent::asInt)
				.filter(OptionalInt::isPresent)
				.mapToInt(OptionalInt::getAsInt)
				.max().orElseThrow();

			return worst;
		}
	}
}