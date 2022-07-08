package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResultsCollector;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResultsFile;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;
import nl.tudelft.aidm.optimalgroups.experiment.paper.historical.model.HistoricalDataExperiment;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.NumberProposedGroupsTogether;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.time.Duration;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult.serializeProfile;

public class BepSys_PF_SDU_HistoricalInstancesExperiment extends HistoricalDataExperiment
{
	public BepSys_PF_SDU_HistoricalInstancesExperiment(String identifier, List<DatasetContext> problemInstances, List<GroupProjectAlgorithm> algos, int runs)
	{
		super(identifier, problemInstances, algos, runs);
	}
	
	@Override
	protected ExperimentResultsCollector newExperimentResultsCollector(String filePath)
	{
		return new ExperimentResultsFile(filePath);
	}
	
	@Override
	protected ExperimentSubResult newExperimentSubResult(DatasetContext datasetContext, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum)
	{
		// determine / translate params here
		
		// id
		
		// num_students
		// num_projects
		// num_slots
		
		// mechanism
		// run_id
		// runtime
		
		// profile_all
		// profile_solo
		// profile_preg
		// profile_unsat
		
		// pregroup_proportion (pregroup / all)
		// pressure
		// PREGROUP_PREF_DIST_TYPE --- unknown!
		// PREGROUP_SIZES_DISTRIBUTION --- cannot name, only dist |||
		
		return new HistoricalInstanceResult(datasetContext, mechanism, trialRunNum, matching, runtime);
	}
	
	private record HistoricalInstanceResult(DatasetContext datasetContext, GroupProjectAlgorithm mechanism, Integer trialRunNum, GroupToProjectMatching<?> matching, Duration runtime) implements ExperimentSubResult
	{
		@Override
		public List<Object> columnHeaders()
		{
			return List.of(
					"id",
					
					"num_students",
					"num_projects",
					"num_slots",
					
					"mechanism",
					"trial",
					
					"duration_ms",
					
					"profile_all",
					"profile_singles",
					"profile_pregrouped",
					"profile_unsatpregroup",
					
					"project_pressure",
					"pregroup_proportion",
					"pregroup_sizes_distribution",
					
					"num_pregroups_fully_together",
					"num_pregroups_max"
			);
		}
		
		@Override
		public List<Object> columnValues()
		{
			// Calc project pressure
			var minReqProjectAmount = new MinimumReqProjectAmount(datasetContext.groupSizeConstraint(), datasetContext.allAgents().count());
			var projectPressure = 1d * datasetContext.allProjects().count() * datasetContext.numMaxSlots() / minReqProjectAmount.asInt();
			
			// Calc proportion of pregrouping students
			var pregroupings = pregroupingFor(datasetContext);
			var pregroupProportion = 1d * pregroupings.asAgents().count() / datasetContext.allAgents().count();
			
			// Calc distribution of pregrouping sizes
			var pregroupDist = IntStream.rangeClosed(2, datasetContext.groupSizeConstraint().maxSize())
			                            .mapToObj(i -> Integer.toString(pregroupings.ofSize(i).count() * i))
			                            .collect(joining("|"));
			
			// Calc # pregroupings together
			var numPregroupingsTogether = new NumberProposedGroupsTogether(matching, currentPregrouping());
			
			return List.of(
					datasetContext.identifier(),
					
					datasetContext.allAgents().count(),
					datasetContext.allProjects().count(),
					datasetContext.numMaxSlots(),
					
					mechanism.name(),
					trialRunNum,
					
					runtime.toMillis(),
					
					serializeProfile(profileAllStudents()),
					serializeProfile(profileSingles()),
					serializeProfile(profilePregrouped()),
					serializeProfile(profileUnsatpregroup()),
					
					projectPressure,
					pregroupProportion,
					pregroupDist,
					
					numPregroupingsTogether.asInt(),
					pregroupings.count()
			);
		}
		
		private final static WeakHashMap<DatasetContext, Groups<?>> pregroupingsCache = new WeakHashMap<>();
		
		private static synchronized Groups<?> pregroupingFor(DatasetContext datasetContext)
		{
			return pregroupingsCache.computeIfAbsent(datasetContext, d -> new CliqueGroups(d.allAgents()));
		}
		
		private Groups<?> currentPregrouping()
		{
			return pregroupingFor(datasetContext);
		}
		
		private int numPregroupsMax()
		{
			return currentPregrouping().count();
		}
		
		private Profile profileAllStudents()
		{
			return Profile.of(
					AgentToProjectMatching.from(matching)
			);
		}
		
		private Profile profileSingles()
		{
			var preformedGroups = currentPregrouping();
			
			var soloStudents = datasetContext.allAgents()
			                                 .without(preformedGroups.asAgents());
			var matchingSubsetOfSoloStudents = AgentToProjectMatching.from(matching)
			                                                         .filteredBy(soloStudents);
			
			return Profile.of(matchingSubsetOfSoloStudents);
		}
		
		private Profile profilePregrouped()
		{
			var preformedGroups = currentPregrouping();
			var agentsPregrouping = preformedGroups.asAgents();
			
			var matchingPregroupedSatisfied = AgentToProjectMatching.from(matching.filteredBySubsets(preformedGroups))
			                                                        .filteredBy(agentsPregrouping);
			
			var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
			
			return Profile.of(matchingPregroupedSatisfied);
		}
		
		private Profile profileUnsatpregroup()
		{
			var preformedGroups = currentPregrouping();
			var agentsPregrouping = preformedGroups.asAgents();
			
			var matchingIndividuals = AgentToProjectMatching.from(matching);
			var matchingPregroupedSatisfied = AgentToProjectMatching.from(matching.filteredBySubsets(preformedGroups))
			                                                        .filteredBy(agentsPregrouping);
			
			var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
			var pregroupingStudentsUnsatisfied = agentsPregrouping.without(pregroupingStudentsSatisfied);
			var matchingPregroupedUnsatisfied = matchingIndividuals.filteredBy(pregroupingStudentsUnsatisfied);
			
			return Profile.of(matchingPregroupedUnsatisfied);
		}
	}
}
