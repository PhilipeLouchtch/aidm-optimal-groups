package nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.ListBasedProjects;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.util.Pair;
import plouchtch.assertion.Assert;

import java.util.*;

import static java.lang.Math.max;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class ActiveProjects extends ListBasedProjects implements Projects
{
	private final AgentToProjectMatching partialMatching;

	private final GroupFactorization groupFactorization;

	private Projects allProjects;
	private final Agents remainingAgents;
	private final GroupSizeConstraint groupSizeConstraint;

	private Set<Project> asSet;
	private List<Project> asList;

	public ActiveProjects(AgentToProjectMatching partialMatching, Projects allProjects, Agents remainingAgents, GroupSizeConstraint groupSizeConstraint)
	{
		this.partialMatching = partialMatching;

		this.groupFactorization = GroupFactorization.cachedInstanceFor(groupSizeConstraint);

		this.allProjects = allProjects;
		this.remainingAgents = remainingAgents;
		this.groupSizeConstraint = groupSizeConstraint;
	}

	public boolean contains(Project project)
	{
		if (asSet == null) {
			asSet = new HashSet<>(projectList());
		}

		return asSet.contains(project);

	}

	@Override
	protected List<Project> projectList()
	{
		if (asList != null) {
			return asList;
		}

		var partialMatchingByProject = partialMatching.groupedByProject();

		// Definition 4 (i)
		var projectsNotFull = allProjects.asCollection().stream()
			.map(project -> Pair.create(project, partialMatchingByProject.getOrDefault(project, List.of())))
			.filter(projectAndMatchedAgents -> {
				var project = projectAndMatchedAgents.getKey();
				var agentsMatchedToProject = projectAndMatchedAgents.getValue();

				return agentsMatchedToProject.size() < (project.slots().size() * groupSizeConstraint.maxSize());
			})
			.collect(toMap(Pair::getKey, Pair::getValue));


//			var breakpoint = partialMatchingByProject.entrySet().stream()
//				.filter(proj -> partialMatchingByProject.containsKey(proj.getKey()))
//				.filter(proj -> partialMatchingByProject.get(proj.getKey()).size() >= (groupSizeConstraint.maxSize() * proj.getKey().slots().size()))
//				.filter(proj -> projectsNotFull.containsKey(proj.getKey()))
//				.findFirst();

		// Put in all project that have no one assigned yet
//			for (var project : projects.asCollection()) {
//				if (partialMatchingByProject.getOrDefault(project, List.of()).size() < 5)
//					projectsNotFull.computeIfAbsent(project, __ -> List.of());
//			}

//			int sum = projectsNotFull.values().stream().mapToInt(value -> groupSizeConstraint.maxSize() - value.size()).sum();
//			Assert.that(sum == (agents.count() - (currentStep - 1)))
//				.orThrowMessage("Boe!");

		// Defintion 4 (ii)
		var activeAsList = projectsNotFull.entrySet().stream().
			filter(projectWithCapacityAndMatchedStudents -> {
				// p \in P
				var project = projectWithCapacityAndMatchedStudents.getKey();
				var agentsMatchedToProject = projectWithCapacityAndMatchedStudents.getValue();

				// Intuitively, the true question we're asking here, is that "if another student is matched to this
				// project, can these students be partitioned into valid groups? Or if not, then how many must be
				// matched to it still, such that valid groups _can_ be created?" Then we also ask the same question
				// regarding projects that have students assigned in the previous steps (that the student at 'current
				// step' did not pick). * We're not really in a step here, but assume per project that it is picked
				// and see if this would result in a feasible matching. If it is determined not feasible, the project
				// is considered "closed" (not selectable in this round).
				var projectQuorumIfNewAssigned = computeQuorum(project, agentsMatchedToProject.size() + 1);
				var studentsProjectStillNeeds = Math.max(projectQuorumIfNewAssigned - (agentsMatchedToProject.size() + 1), 0);

				var neededForOtherProjects =
					//     SUM            [
					// p' \in P_t-1 / {p} [
					projectsNotFull.entrySet().stream()
						.filter(entry -> !entry.getKey().equals(project)) // other project
						.filter(entry -> entry.getValue().size() > 0) // is opened
						.mapToInt(otherProjectWithMatchedStudents -> {
							int numMatchedToOther = otherProjectWithMatchedStudents.getValue().size();
							var otherQuorum = computeQuorum(project, numMatchedToOther);
							// max{q_p' - sigma_p'(t - 1), 0}
							return max(otherQuorum - numMatchedToOther, 0);
						})
						.sum();
				// ]
				// ]

				return (studentsProjectStillNeeds + neededForOtherProjects) <= (remainingAgents.count());
			})
			.map(Map.Entry::getKey)
			.collect(toList());

		this.asList = activeAsList;
		return asList;
	}

	private int computeQuorum(Project project, int numStudents)
	{
		Assert.that(numStudents <= groupSizeConstraint.maxSize() * project.slots().size())
			.orThrowMessage(
				String.format("No quorum possible for project %s, numStudents %s exceeds max total capacity...", project, numStudents));

		// We're searching for the smallest number of students that can be partitioned into groups
		// of valid sizes. Where this smallest number is >= numStudents.
		// In the paper of Monte and Tumennsasan this is a simple number, in our case, projects have
		// capacities for multiple groups. So a quorum in our case is dynamic as it shifts
		// Example, with groupSizeConstraint = [4,5], when |mu(p)| (num agents assigned to project p) is less than 5,
		// the quorum is 4. When the |mu(p)| = 6, the quorum for p becomes 8 (two groups of size 4) and remains that until
		// |mu(p)| becomes larger than 10, then it is 12 and so on...
		int quorum = numStudents;
		while (!groupFactorization.isFactorableIntoValidGroups(quorum))
		{
			quorum++;
		}

		// (next) number of students that can be partitioned into valid group(s)
		// quorum != numStudents if numStudents could not be partitioned
		return quorum;
	}
}
