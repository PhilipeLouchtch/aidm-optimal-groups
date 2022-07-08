package nl.tudelft.aidm.optimalgroups.algorithm.holistic.solver.minizinc;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.solver.minizinc.model.SequentualDatasetContext;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.solver.minizinc.model.SequentualProjects;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedRandomly;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.metric.profile.StudentRankProfile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatch;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GroupedProjectMinizincAllocation
{
	private final int groupsPerTopic;
	DatasetContext datasetContext;

	private final static URL model = MiniZinc.class.getClassLoader().getResource("Grouped_Project_Student_Allocation.mzn");

	public GroupedProjectMinizincAllocation(DatasetContext datasetContext)
	{
		var homogeneousSlotCounts = datasetContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).distinct().count() == 1;
		Assert.that(homogeneousSlotCounts).orThrowMessage("Heterogenous number of slots across projects is not (yet) supported for MiniZinc model");
		
		groupsPerTopic = datasetContext.numMaxSlots();
		
		Assert.that(datasetContext.groupSizeConstraint().minSize() == 4).orThrowMessage("(Yet) unsupported param value: GSC-min must be 4");
		Assert.that(datasetContext.groupSizeConstraint().maxSize() == 5).orThrowMessage("(Yet) unsupported param value: GSC-min must be 5");

		Assert.that(groupsPerTopic > 0).orThrowMessage("Unsatisfiable: Topics must have capacity for at least 1 group");

		this.datasetContext = datasetContext;
	}

	public AgentToProjectMatching matching()
	{
		var minizinc = new MiniZinc(model);

		SequentualDatasetContext seqDataset = SequentualDatasetContext.from(datasetContext);

		var slotsPerProject = datasetContext.allProjects().asCollection().stream().map(Project::slots).map(List::size).distinct().collect(Collectors.toList());
		Assert.that(slotsPerProject.size() == 1)
			.orThrowMessage("I know this is probably a bad time, but you need to fix MiniZinc model to support heterogenuous slot amounts");

		var instanceData = new StudentGroupProjectMatchingInstanceData(seqDataset, slotsPerProject.get(0));

		try {
			long timelimit = Duration.ofMinutes(5).toMillis();
			var solutions = minizinc.run(instanceData, "gurobi", timelimit);

			var bestRawSolution = solutions.bestRaw();
			var bestSolution = new MinizincSolution(seqDataset, bestRawSolution);


			return bestSolution.asMatching();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static class MinizincSolution
	{
		private final static Pattern pattern = Pattern.compile("[0-9]+");

		private final SequentualDatasetContext datasetContext;
		private final List<SequentualProjects.SequentualProject> studentTopicCorrespondance;

		private AgentToProjectMatching matching = null;

		public MinizincSolution(SequentualDatasetContext datasetContext, String rawSolution)
		{
			this.datasetContext = datasetContext;

			// raw solution should consist of json-like array: "[value1, value2]"
			// with index being the agent's id and value the project id assigned
			// I don't expect it to be a problem, for now, to parse it manually
			var matchingResult = pattern.matcher(rawSolution).results();

			this.studentTopicCorrespondance = matchingResult.map(matchResult -> matchResult.group(0))
				.map(Integer::parseInt)
				.map(id -> datasetContext.allProjects().findWithSeqNum(id))
				.flatMap(Optional::stream)
				.map(proj -> (SequentualProjects.SequentualProject) proj)
				.collect(Collectors.toList());

			this.studentTopicCorrespondance.add(0, null); // Minizinc is 1-based!!!
		}

		public SequentualProjects.SequentualProject projectAssignedTo(Agent student)
		{
			return studentTopicCorrespondance.get(student.sequenceNumber());
		}

		public AgentToProjectMatching asMatching()
		{
			if (matching != null) {
				return matching;
			}

			var matches = new AgentToProjectMatching.Simple(datasetContext.originalContext());

			for (int i = 1; i < studentTopicCorrespondance.size(); i++)
			{
				var agent = datasetContext.allAgents().findBySequenceNumber(i).orElseThrow();
				var project = studentTopicCorrespondance.get(i);

				var originalAgent = datasetContext.mapToOriginal(agent);
				var originalProject = datasetContext.mapToOriginal(project);

				matches.add(new AgentToProjectMatch(originalAgent, originalProject));
			}

			this.matching = matches;
			return matches;
		}
	}


	public static void main(String[] args) throws Exception
	{
//		var ce = CourseEdition.fromLocalBepSysDbSnapshot(10);
//		var ce = ThesisDatasets.CE10Like(500);
		var ce = DatasetContext_AugmentedPreferences_AppendedRandomly.from(CourseEditionFromDb.fromLocalBepSysDbSnapshot(10));

		var seqDataset = SequentualDatasetContext.from(ce);
		var data = new StudentGroupProjectMatchingInstanceData(seqDataset, 1);

		var henk = new GroupedProjectMinizincAllocation(ce).matching();
		MatchingMetrics.StudentProject metrics = new MatchingMetrics.StudentProject(henk);

		new StudentRankProfile(henk).displayChart("MiniZinc - MinSum");

		return;
	}
}
