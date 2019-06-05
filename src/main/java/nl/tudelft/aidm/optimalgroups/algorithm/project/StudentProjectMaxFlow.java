package nl.tudelft.aidm.optimalgroups.algorithm.project;

import louchtch.graphmatch.matching.MaxFlowMatching;
import louchtch.graphmatch.model.*;
import nl.tudelft.aidm.optimalgroups.model.entity.*;
import nl.tudelft.aidm.optimalgroups.model.pref.AverageProjectPreferenceOfAgents;

import java.util.*;

public class StudentProjectMaxFlow //implements ProjectMatchingAlgorithm
{
	Agents students;
	Projects projects;

	public StudentProjectMaxFlow(Agents students, Projects projects)
	{
		this.students = students;
		this.projects = projects;
	}

	public Matching.FormedGroupToProjectMatchings result()
	{
		StudentVertices studentVertices = new StudentVertices(students);
		ProjectSlotVertices projectSlotVertices = new ProjectSlotVertices(projects);
		var edges = new StudentToProjectSlotsEdges(studentVertices, projectSlotVertices);


		// Sick cast https://stackoverflow.com/questions/3246137/java-generics-cannot-cast-listsubclass-to-listsuperclass
		// warning: not very safe, but not catastrophic if lists are not modified
		var left = (Vertices<StudentProjectMatchingVertexContent>) (Vertices<? extends StudentProjectMatchingVertexContent>) studentVertices;
		var right = (Vertices<StudentProjectMatchingVertexContent>) (Vertices<? extends StudentProjectMatchingVertexContent>) projectSlotVertices;

		var graph = new MaxFlowGraph<StudentProjectMatchingVertexContent>(left, right, edges);

		var matching = new MaxFlowMatching<>(graph, SearchType.MinCost);

		var groupedBySlot = new HashMap<Project.ProjectSlot, List<Agent>>(projects.countAllSlots());
		var groupedByProject = new HashMap<Project, List<Agent>>(projects.count());

		matching.asListOfEdges().forEach(edge -> {
			Project.ProjectSlot projectSlot = ((ProjectSlotStudentSlotContent) edge.to.content()).theProjectSlot;
			Agent student = ((StudentVertexContent) edge.from.content()).theStudent;

			groupedBySlot.computeIfAbsent(projectSlot, __ -> new ArrayList<>()).add(student);
			groupedByProject.computeIfAbsent(projectSlot.belongingToProject(), __ -> new ArrayList<>()).add(student);
		});

		FormedGroups formedGroups = new FormedGroups();

		var resultingMatching = new Matching.FormedGroupToProjectMatchings();
		for (var x : groupedBySlot.entrySet())
		{
			Agents agents = Agents.from(x.getValue());
			Group.TentativeGroup tentativeGroup = new Group.TentativeGroup(agents, new AverageProjectPreferenceOfAgents(agents));
			Group.FormedGroup formedGroup = formedGroups.addAsFormed(tentativeGroup);


			var match = new Matching.FormedGroupToProjectSlotMatch(formedGroup, x.getKey());
			resultingMatching.add(match);
		}

		return resultingMatching;
	}

//
//
//	private static class ProjectSlotMatching implements Matching.Match<Group.FormedGroup, Project.ProjectSlot>
//	{
//		private Agents from;
//		private Project.ProjectSlot to;
//
//		public ProjectSlotMatching(Agents from, Project.ProjectSlot to)
//		{
//			this.from = from;
//			this.to = to;
//		}
//
//		@Override
//		public  from()
//		{
//			return from;
//		}
//
//		@Override
//		public Project.ProjectSlot to()
//		{
//			return to;
//		}
//	}

	private static class StudentToProjectSlotsEdges extends DirectedWeightedEdges // no generic, we'll cast
	{
		public StudentToProjectSlotsEdges(StudentVertices studentVertices, ProjectSlotVertices projectSlotVertices)
		{
			// for each student
			studentVertices.forEach(studentVertex -> {

				// for each student's preference
				studentVertex.content().theStudent.projectPreference.forEach((projectId, rank) -> {

					// Find the corresponding project-slot vertices
					projectSlotVertices.findAllForProject(projectId).forEach(projSlotVert -> {

						// and create/add and edge between them
						var edge = DirectedWeightedEdge.between(studentVertex, projSlotVert, rank);
						add(edge);
					});
				});

			});
		}
	}

	private static class ProjectSlotVertices extends Vertices<ProjectSlotStudentSlotContent>
	{
		private Map<Integer, List<Vertex<ProjectSlotStudentSlotContent>>> projectToSlots;

		public ProjectSlotVertices(Projects projects)
		{
			projectToSlots = new HashMap<>();

			projects.asCollection().forEach(project -> {
				project.slots().forEach(projectSlot -> {

					for (int i = 0; i < 5; i++)
					{
						var vert = new Vertex<>(new ProjectSlotStudentSlotContent(projectSlot));
						this.listOfVertices.add(vert);

						projectToSlots
							.computeIfAbsent(project.id(), __ -> new ArrayList<>(project.slots().size()))
							.add(vert);
					}
				});
			});
		}

		public Collection<Vertex<ProjectSlotStudentSlotContent>> findAllForProject(int projectIdToFind)
		{
			if (projectToSlots == null) {
				// compute if mapping not created yet

				this.listOfVertices.forEach(vert -> {
					Project proj = vert.content().theProjectSlot.belongingToProject();

				});
			}

			return projectToSlots.getOrDefault(projectIdToFind, Collections.emptyList());
		}
	}

	private static class StudentVertices extends Vertices<StudentVertexContent>
	{
		public StudentVertices(Agents students)
		{
			students.forEach(student -> {
				var vert = new Vertex<>(new StudentVertexContent(student));
				listOfVertices.add(vert);
			});
		}
	}

	private interface StudentProjectMatchingVertexContent {}

	private static class ProjectSlotStudentSlotContent implements StudentProjectMatchingVertexContent
	{
		final Project.ProjectSlot theProjectSlot;

		public ProjectSlotStudentSlotContent(Project.ProjectSlot theProjectSlot)
		{
			this.theProjectSlot = theProjectSlot;
		}
	}

	private static class StudentVertexContent implements StudentProjectMatchingVertexContent
	{
		final Agent theStudent;

		public StudentVertexContent(Agent theStudent)
		{
			this.theStudent = theStudent;
		}
	}
}
