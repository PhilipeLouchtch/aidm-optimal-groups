package nl.tudelft.aidm.optimalgroups.algorithm.holistic.pessimism;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.util.*;
import java.util.stream.Collectors;

record KProjectAgentsPairing(Collection<ProjectAgentsPairing>pairingsAtK, int k)
{
	public record Edge(Agent from, Project to, int rank){}

	public static KProjectAgentsPairing from(Agents agents, Projects projects, GroupSizeConstraint groupSizeConstraint)
	{
//		if (groupSizeConstraint.maxSize() / groupSizeConstraint.minSize() < 2)
//			if (agents.count() <= groupSizeConstraint.maxSize()) {
//				return new KProjectAgentsPairing();
//			}

		List<Edge> edges = new ArrayList<>(agents.count() * projects.count());
		Map<Agent, List<Edge>> edgesFrom = new IdentityHashMap<>();
		Map<Project, List<Edge>> edgesTo = new IdentityHashMap<>();

		agents.asCollection().forEach(agent -> {
			agent.projectPreference().forEach((Project project, int rank) -> {
				var edge = new Edge(agent, project, rank);

				edges.add(edge);
				edgesFrom.computeIfAbsent(agent, __ -> new ArrayList<>()).add(edge);
				edgesTo.computeIfAbsent(project, __ -> new ArrayList<>()).add(edge);
			});
		});

		Map<Project, ProjectAgentsPairing>[] agentsWithK = new IdentityHashMap[projects.count() + 1];
		for (int i = 0; i < agentsWithK.length; i++)
		{
			agentsWithK[i] = new IdentityHashMap<>();
		}

		//		int[] cum260 = new int[43];
		//		var proj260 = projects.findWithId(260).orElseThrow();
		//		agents.forEach(agent -> {
		//			agent.projectPreference().rankOf(proj260)
		//				.ifPresent(rank -> cum260[rank] += 1);
		//		});

		int k = 0;
		for (var thisAgent : agents.asCollection())
		{
			int l = agents.datsetContext.allProjects().count() + 1;
			Project lProj = null;
			Set<Agent> lPossibleGroupmates = null;

			// Note: the current assumption is that prefs have no ties
			// but rankOf() can return same rank for different projects (indifferences)
			var prefs = thisAgent.projectPreference().asListOfProjects();

			for (int i = prefs.size(); i >= 1; i--)
			{
				final var rankThisAgent = i;

				// opt: skip if rank being examined is better (<) than the 'k' found so far
//				if (rankThisAgent <= k)
//					continue;

				// Prefs is a List representation of ProjPrefs and is 0-based
				var proj = prefs.get(rankThisAgent - 1);

				var possibleGroupmates = edgesTo.get(proj).stream()
					// Do not include 'this' agent as a possible group mate
					.filter(edge -> edge.from() != thisAgent)
					// Consider only agents who rank 'proj' better or eq
					.filter(edge -> edge.rank() <= rankThisAgent)
					.map(Edge::from)
					.collect(Collectors.toSet());

				// Possible group size = |possible group mates| + 1 (the agent himself)
				int possibleGroupSize = possibleGroupmates.size() + 1;
				if (possibleGroupSize >= groupSizeConstraint.minSize() && rankThisAgent < l)
				{
					l = rankThisAgent;
					lProj = proj;
					lPossibleGroupmates = possibleGroupmates;
				}
			}

			if (l >= agentsWithK.length) {
				// Didn't find anything so try another agent
				continue;
			}

			var existingResultForProj = agentsWithK[l].get(lProj);
			if (existingResultForProj == null) {
				if (lProj == null) {
					System.out.printf("DBG: woops");
				}

				HashSet<Agent> agentsInclude = new HashSet<>(Set.of(thisAgent));
				ProjectAgentsPairing pairing = new ProjectAgentsPairing(lProj, agentsInclude, lPossibleGroupmates);
				agentsWithK[l].put(lProj, pairing);
			}
			else {
				existingResultForProj.agents().add(thisAgent);
				existingResultForProj.possibleGroupmates().remove(thisAgent);
			}

			k = Math.max(l, k);
		}

		// Hacky fix: k = 0 implies nothing found - no grouping possible
		if (k == 0) {
			return new KProjectAgentsPairing(Collections.emptyList(), 0);
		}

		return new KProjectAgentsPairing(agentsWithK[k].values(), k);
	}

}
