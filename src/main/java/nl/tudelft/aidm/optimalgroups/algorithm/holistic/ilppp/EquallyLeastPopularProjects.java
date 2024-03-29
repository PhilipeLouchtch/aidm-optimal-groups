package nl.tudelft.aidm.optimalgroups.algorithm.holistic.ilppp;

import nl.tudelft.aidm.optimalgroups.algorithm.project.AgentProjectMaxFlowMatching;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCR;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCRStudent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EquallyLeastPopularProjects implements Projects
{
	private final DatasetContext datasetContext;
	private final Map<Project, List<Agent>> grouping;
	private Projects equallyLeastPopularProjects = null;

	public EquallyLeastPopularProjects(DatasetContext datasetContext, Map<Project, List<Agent>> grouping)
	{
		this.datasetContext = datasetContext;
		this.grouping = grouping;
	}

	// most naive implementation: just maxflow it and
	private List<Project> determine()
	{
		// Collect all students
		var students = Agents.from(grouping.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

		// All projects that have students assigned (the filter is probably redundant but it costs nothing to do)
		var projects = Projects.from(
			grouping.keySet().stream()
				.filter(project -> grouping.get(project).size() > 0)
				.collect(Collectors.toList())
		);

		// mapping: AUPCR scores when Project is exluded
		var results = new ConcurrentHashMap<Project, AUPCR>(projects.count());

		projects.asCollection()/*.parallelStream()*/.forEach(project -> {
			var projectsWithoutOne = projects.without(project);

			// TODO: include this maxflow result?
			var maxflowResultWithoutCurrentProject = new AgentProjectMaxFlowMatching(datasetContext, students, projectsWithoutOne);

//			SingleGroupPerProjectMatching matching = new SingleGroupPerProjectMatching(maxflowResultWithoutCurrentProject);

			// calc effect
			var metric = new AUPCRStudent(maxflowResultWithoutCurrentProject, projectsWithoutOne, students);

			results.put(project, metric);
		});

		// The "Least Popular" project is the one whose removal results in highest metric (AUPCR) relative to any other
		// now we just need to determine which project that is, to do so we need to sort the tuples computed above by the value (AUPCR result)
		// by sorting descending, the least popular project (key) is the top most (0th) element
		ArrayList<Map.Entry<Project, AUPCR>> entries = new ArrayList<>(results.entrySet());
		entries.sort(Comparator.comparing((Map.Entry<Project, AUPCR> entry) -> entry.getValue().asDouble()).reversed());

		// some dbg / progess report
//		for (Map.Entry<Project, AUPCR> entry : entries)
//		{
//			System.out.printf("\tExcluding project '%s' with %s students has effect: %s\n", entry.getKey(), grouping.get(entry.getKey()).size(), entry.getValue().result());
//		}

		// We want to get all Projects that share same, highest AUPCR-after-removal. Use streams to collect into a map with the AUPCR-as-float as key
		// note: not very efficient implementation, but as easy to write and should read easier
		Map<Double, List<Map.Entry<Project, AUPCR>>> groupedByAUPCR = entries.stream()
			.collect(Collectors.groupingBy(entry -> entry.getValue().asDouble()));

		var leastPopular = entries.get(0);

		var equallyLeastPopularAsList = groupedByAUPCR.get(leastPopular.getValue().asDouble());
//		equallyLeastPopularAsList.forEach(entry -> {
//			System.out.printf("Removing project '%s' has least effect on AUPCR (resulting in: %s)\n", entry.getKey(), entry.getValue().result());
//		});

		// Need to map the list of entries to just the projects
		return equallyLeastPopularAsList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
	}

	@Override
	public int count()
	{
		return equallyLeastPopularProjects().count();
	}

	@Override
	public int countAllSlots()
	{
		return  equallyLeastPopularProjects().countAllSlots();
	}
	
	@Override
	public void forEach(Consumer<Project> fn)
	{
		equallyLeastPopularProjects().forEach(fn);
	}

	@Override
	public Projects without(Project project)
	{
		return equallyLeastPopularProjects().without(project);
	}

	@Override
	public Collection<Project> asCollection()
	{
		return equallyLeastPopularProjects().asCollection();
	}

	private Projects equallyLeastPopularProjects()
	{
		if (equallyLeastPopularProjects == null)
			equallyLeastPopularProjects = Projects.from(determine());

		return equallyLeastPopularProjects;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof EquallyLeastPopularProjects)
		{
			EquallyLeastPopularProjects other = (EquallyLeastPopularProjects) obj;

			obj = ((EquallyLeastPopularProjects) obj).equallyLeastPopularProjects();
		}

		return equallyLeastPopularProjects().equals(obj);
	}

}
