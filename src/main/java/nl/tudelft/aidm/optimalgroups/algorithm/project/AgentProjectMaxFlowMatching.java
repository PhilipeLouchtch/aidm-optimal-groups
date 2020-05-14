package nl.tudelft.aidm.optimalgroups.algorithm.project;

import com.google.ortools.graph.MinCostFlow;
import louchtch.graphmatch.model.*;
import nl.tudelft.aidm.optimalgroups.algorithm.PreferencesToCostFn;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatch;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"Duplicates"})
public class AgentProjectMaxFlowMatching implements AgentToProjectMatching
{
	static {
		System.loadLibrary("jniortools");
	}

	private static Map<Collection<Project>, AgentProjectMaxFlowMatching> existingResultsCache = new ConcurrentHashMap<>();

	// source and sink vertices
	private static Vertex<Object> source = new Vertex<>(null);
	private static Vertex<Object> sink = new Vertex<>(null);


	private final DatasetContext datasetContext;
	public final Agents students;
	public final Projects projects;
	private final PreferencesToCostFn preferencesToCostFunction;

	//	private Map<Project.ProjectSlot, List<Agent>> groupedBySlot = null;
	private Map<Project, List<Agent>> groupedByProject = null;
	private List<Match<Agent, Project>> asList = null;
	private Boolean allStudentsAreMatched = null;

	public static void flushCache()
	{
		existingResultsCache = new ConcurrentHashMap<>();
	}

	public static AgentProjectMaxFlowMatching of(DatasetContext datasetContext, Agents students, Projects projects)
	{
		if (existingResultsCache.containsKey(projects.asCollection()) == false) {
			AgentProjectMaxFlowMatching maxflow = new AgentProjectMaxFlowMatching(datasetContext, students, projects);
			existingResultsCache.put(projects.asCollection(), maxflow);

			return maxflow;
		}

		AgentProjectMaxFlowMatching existing = existingResultsCache.get(projects.asCollection());
		if (existing.students.equals(students)) {
			return existing;
		}
		else {
			throw new RuntimeException("Requested a cached StudentsProjectsMaxFlow for previously computed projects, but different student set." +
				"Cache implementation only works on projects and assumes identical studens. Decide how to handle this case first (support proj + studs or simply compute this case without caching).");
		}
	}

	/**
	 * When not all agents or projects must be used in the matching, but the context is still one given by DatasetContext
	 * @param datasetContext
	 * @param students A (subset) of students in datasetContext
	 * @param projects A (subset) of projects in datasetContext
	 */
	public AgentProjectMaxFlowMatching(DatasetContext datasetContext, Agents students, Projects projects)
	{
		this(datasetContext, students, projects, ProjectPreference::rankOf);
	}

	/**
	 * When not all agents or projects must be used in the matching, but the context is still one given by DatasetContext.
	 * Furthermore, allows overriding of assignment cost through the {@link PreferencesToCostFn} parameter.
	 * @param datasetContext
	 * @param students A (subset) of students in datasetContext
	 * @param projects A (subset) of projects in datasetContext
	 */
	public AgentProjectMaxFlowMatching(DatasetContext datasetContext, Agents students, Projects projects, PreferencesToCostFn preferencesToCostFunction)
	{
		this.datasetContext = datasetContext;
		this.students = students;
		this.projects = projects;
		this.preferencesToCostFunction = preferencesToCostFunction;
	}

	/**
	 * Returns a MaxFlow-matching on the full given datasetcontext with assignment costs being the ranks
	 * @param datasetContext The datacontext
	 */
	public AgentProjectMaxFlowMatching(DatasetContext datasetContext)
	{
		this(datasetContext, datasetContext.allAgents(), datasetContext.allProjects());
	}

	/**
	 * Returns a MaxFlow-matching on the full given datasetcontext but with a custom cost assigning function
	 * @param datasetContext The datacontext
	 * @param preferencesToCostFunction Custom assignment-cost function
	 */
	public AgentProjectMaxFlowMatching(DatasetContext datasetContext, PreferencesToCostFn preferencesToCostFunction)
	{
		this(datasetContext, datasetContext.allAgents(), datasetContext.allProjects(), preferencesToCostFunction);
	}

	@Override
	public DatasetContext datasetContext()
	{
		// FIXME later
//		throw new ImplementMe();
		return this.datasetContext;
	}

	@Override
	public Map<Project, List<Agent>> groupedByProject()
	{
		init();

		return groupedByProject;
	}

	@Override
	public List<Match<Agent, Project>> asList()
	{
		if (asList != null)
			return asList;

//		ListBasedMatchings listBasedMatching = new ListBasedMatchings<Agent, Project>();

		this.asList = new ArrayList<>();

		groupedByProject().forEach((project, agents) -> {
			agents.forEach(agent -> {
				this.asList.add(new AgentToProjectMatch(agent, project));
			});
		});


		return this.asList;
	}

	public boolean allStudentsAreMatched()
	{
		if (allStudentsAreMatched == null) {
			long studentsMatched = this.groupedByProject.values().stream().mapToLong(Collection::size).sum();
			allStudentsAreMatched = studentsMatched == this.students.count();
		}

		return allStudentsAreMatched;
	}


	// quick and dirty: want access to groupedByProject but also keep the asList() method functioning AND without unnecessary recomputing values each time
	private void init()
	{
		// Very simple check: init only once, subsequent calls return directly
		if (this.groupedByProject != null) {
			return;
		}

		var groupedByProject = new IdentityHashMap<Project, List<Agent>>(projects.count());

		StudentVertices studentVertices = new StudentVertices(students);
		ProjectVertices projectVertices = new ProjectVertices(projects);
		var edges = new StudentToProjectEdges(studentVertices, projectVertices);

		MinCostFlow minCostFlow = new MinCostFlow();

		int source = AgentProjectMaxFlowMatching.source.id;
		int sink = AgentProjectMaxFlowMatching.sink.id;

		// Source and Sink do not need to supply/consume more than we have students
		minCostFlow.setNodeSupply(source, studentVertices.count());
		minCostFlow.setNodeSupply(sink, -studentVertices.count());

		studentVertices.forEach(studentVertex -> {
			minCostFlow.addArcWithCapacityAndUnitCost(source, studentVertex.id, 1, 1);
		});

		projectVertices.forEach(projectVertex -> {
			Project project = projectVertex.content().theProject;
			int capacity = project.slots().size() * datasetContext.groupSizeConstraint().maxSize();
			minCostFlow.addArcWithCapacityAndUnitCost(projectVertex.id, sink, capacity, 1);
		});


		List<Integer> arcs = new ArrayList<>();

		edges.forEach(edge -> {
			// Convert the edge to an OrTools MaxFlow arc with capacity 1 and cost set to the weight of the edge (the rank of the preference this edge represents)
			int arc = minCostFlow.addArcWithCapacityAndUnitCost(edge.from.id, edge.to.id, 1, edge.weight);

			// We need to record the id's of the arcs created by OrTools MaxFlow implementation
			// as we need to query them after the problem instance was solved.
			arcs.add(arc);
		});

		// TODO: check if status (return value) is always "OPTIMAL"?
		minCostFlow.solveMaxFlowWithMinCost();

		for (var arc : arcs)
		{
			// Not all arcs will have a flow assigned, we only care about those that do (non-zero flow: vertices are matched)
			if (minCostFlow.getFlow(arc) == 0) continue;

			int from = minCostFlow.getTail(arc);
			int to = minCostFlow.getHead(arc);

			// Not very efficient: getting the agent/project instance for the corresponding ID
			Agent student = studentVertices.asReadonlyList().stream().filter(v -> v.id == from).findAny().get().content().theStudent;
			Project project = projectVertices.asReadonlyList().stream().filter(v -> v.id == to).findAny().get().content().theProject;

			// Put in the grouping
			groupedByProject.computeIfAbsent(project, __ -> new ArrayList<>()).add(student);
		}

		this.groupedByProject = groupedByProject;
	}

	///////////
	/* EDGES */
	///////////
	private class StudentToProjectEdges extends DirectedWeightedEdges<StudentProjectMatchingVertexContent> // no generic, we'll cast
	{
		public StudentToProjectEdges(StudentVertices studentVertices, ProjectVertices projectVertices)
		{
			// for each student
			studentVertices.forEach(studentVertex -> {

				// for each student's preference, create the edges to projects according to preferences
				Agent student = studentVertex.content().theStudent;

				if (student.projectPreference.isCompletelyIndifferent()) {
					// Indifferent -> no projects in pref profile at all
					projectVertices.forEach(projectVertex -> {
						// This student is indifferent, therefore prioritize everyone else by assigning lowest rank
						// the "combined preferences" algorithm does the same. Another approach: exclude these studens from
						// the maxflow matchings and only add them at the very end as "wildcard" students
						// TODO: investigate effects
						int rank = projectVertices.count() - 1;

						var edge = DirectedWeightedEdge.between(studentVertex, projectVertex, rank);
						add(edge);
					});
				}
				else {
					// Note: if student is missing projects from the profile, no edge will be created
					// therefore projects that are missing from the pref profile are counted as "do not want"
					student.projectPreference.forEach((Project project, int rank) -> {
						projectVertices.findForProject(project.id())
							.ifPresent(projectVertex -> {
								var costOfProjectToStudent = preferencesToCostFunction.costOfGettingAssigned(student.projectPreference, project);
								var edge = DirectedWeightedEdge.between(studentVertex, projectVertex, costOfProjectToStudent);
								add(edge);
							});
					});

				}
			});
		}
	}

	//////////////
	/* VERTICES */
	//////////////
	private static class ProjectVertices extends Vertices<ProjectVC>
	{
		private Map<Integer, Vertex<ProjectVC>> projectIdToProjectVert;

		public ProjectVertices(Projects projects)
		{
			projectIdToProjectVert = new HashMap<>();

			projects.asCollection().forEach(project -> {
				var vert = new Vertex<>(new ProjectVC(project));

				this.listOfVertices.add(vert);
				this.projectIdToProjectVert.put(project.id(), vert);
			});
		}

		public Optional<Vertex<ProjectVC>> findForProject(int projectId)
		{
			Vertex<ProjectVC> value = projectIdToProjectVert.get(projectId);
			return Optional.ofNullable(value);
		}
	}

	private static class StudentVertices extends Vertices<StudentVC>
	{
		public StudentVertices(Agents students)
		{
			students.forEach(student -> {
				var vert = new Vertex<>(new StudentVC(student));
				listOfVertices.add(vert);
			});
		}
	}

	/////////////////////
	/* VERTEX CONTENTS */
	/////////////////////
	private interface StudentProjectMatchingVertexContent {}

	private static class ProjectVC implements StudentProjectMatchingVertexContent
	{
		final Project theProject;

		public ProjectVC(Project theProject)
		{
			this.theProject = theProject;
		}
	}

	private static class StudentVC implements StudentProjectMatchingVertexContent
	{
		final Agent theStudent;

		public StudentVC(Agent theStudent)
		{
			this.theStudent = theStudent;
		}
	}
}

