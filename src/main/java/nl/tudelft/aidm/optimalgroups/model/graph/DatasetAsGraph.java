package nl.tudelft.aidm.optimalgroups.model.graph;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.*;

public class DatasetAsGraph implements BipartitieAgentsProjectGraph
{
	private final DatasetContext datasetContext;

	private final Edges edges;
	private final Vertices vertices;

//	private final IdentityHashMap<Agent, Vertex<Agent>> agentVertices;
//	private final IdentityHashMap<Project, Vertex<Project>> projectVertices;

	public DatasetAsGraph(DatasetContext datasetContext)
	{
		Objects.requireNonNull(datasetContext);
		this.datasetContext = datasetContext;

//		this.agentVertices = new IdentityHashMap<>();
//		this.projectVertices = new IdentityHashMap<>();

		int numAgents = datasetContext.allAgents().count();
		int numProjects = datasetContext.allProjects().count();

		this.vertices = new Vertices(numAgents + numProjects);
		this.edges = new DatasetEdges();

//		datasetContext.allProjects().forEach(project -> projectVertices.put(project, vertices.vertexOf(project)));
	}

	@Override
	public Edges edges()
	{
		return edges;
	}

	@Override
	public Set<Vertex> vertices()
	{
		return vertices.asSet();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DatasetAsGraph that = (DatasetAsGraph) o;
		return datasetContext.equals(that.datasetContext);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(datasetContext);
	}

	private static class Vertices
	{
		private int idOfNext;
		private final Vertex[] vertices;
		private final Set<Vertex> asSet;
		private final Map<Object, Object> existing = new IdentityHashMap<>();

		public Vertices(int capacity)
		{
			idOfNext = 0;
			vertices = new Vertex[capacity];
			asSet = new HashSet<>(capacity);
		}

		public <T> Vertex<T> vertexOf(int id) {
			// Caller should know the type
			return (Vertex<T>) vertices[id];
		}

		public <T> Vertex<T> vertexOf(T obj)
		{
			//noinspection unchecked -- is safe
			return (Vertex<T>) existing.computeIfAbsent(obj, o -> {
				var vert = new Vertex<>(idOfNext++, o);
				vertices[vert.id()] = vert;
				asSet.add(vert);
				return vert;
			});
		}

		public Set<Vertex> asSet()
		{
			return Collections.unmodifiableSet(asSet);
		}
	}

	public class DatasetEdges implements Edges
	{
		private Set<WeightedEdge> edges;

		private Map<Agent, Set<WeightedEdge>> edgesFromAgent;
		private Map<Project, Set<WeightedEdge>> edgesToProject;

		public DatasetEdges()
		{
			edges = new HashSet<>(datasetContext.allAgents().count() * datasetContext.allProjects().count());
			edgesFromAgent = new IdentityHashMap<>(datasetContext.allProjects().count());
			edgesToProject = new IdentityHashMap<>(datasetContext.allAgents().count());

			datasetContext.allAgents().forEach(agent ->
			{
				Vertex<Agent> agentVertex = vertices.vertexOf(agent);

				agent.projectPreference().forEach((Project project, int rank) -> {
					Vertex<Project> projectVertex = vertices.vertexOf(project);

					WeightedEdge edge = new WeightedEdge(agentVertex, projectVertex, rank);

					edges.add(edge);

					edgesFromAgent.computeIfAbsent(agent, __ -> new HashSet<>()).add(edge);
					edgesToProject.computeIfAbsent(project, __ -> new HashSet<>()).add(edge);
				});
			});
		}

		@Override
		public Set<WeightedEdge> all()
		{
			return Collections.unmodifiableSet(edges);
		}

		@Override
		public Set<WeightedEdge> from(Agent agent)
		{
			return edgesFromAgent.getOrDefault(agent, Collections.emptySet());
		}

		@Override
		public Set<WeightedEdge> to(Project project)
		{
			return edgesToProject.getOrDefault(project, Collections.emptySet());
		}
	}
}