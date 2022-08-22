package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResultsCollector;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentResultsFile;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;
import nl.tudelft.aidm.optimalgroups.experiment.paper.historical.model.HistoricalDataExperimentBase;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.NumberProposedGroupsTogether;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult.serializeProfile;

public class HistoricalDatasetExperiment extends HistoricalDataExperimentBase
{
	public HistoricalDatasetExperiment(String identifier, List<DatasetContext> problemInstances, List<GroupProjectAlgorithm> algos, int runs)
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
					
					"pregroup_proportion",
					"pregroup_sizes_distribution",
					
					"num_pregroups_fully_together",
					"num_pregroups_max"
			);
		}
		
		@Override
		public List<Object> columnValues()
		{
			// Calc proportion of pregrouping students
			var pregroupings = currentPregrouping();
			var pregroupProportion = 1d * pregroupings.asAgents().count() / datasetContext.allAgents().count();
			
			// workaround for the SDU datasets because they have a per-project group size bounds
			var maxAllowedGroupSize = datasetContext instanceof SDUDatasetContext sduDatasetContext ?
				sduDatasetContext.allProjects().asCollection().stream()
				                 .map(sduDatasetContext::groupSizeBoundsOf)
				                 .mapToInt(GroupSizeConstraint::maxSize)
				                 .max().getAsInt()
                : datasetContext.groupSizeConstraint().maxSize();
			
	         // Calc distribution of pregrouping sizes
	         var pregroupDist = groupSizesDist(pregroupings, maxAllowedGroupSize);
			
			// Calc # pregroupings together
			var numPregroupingsTogether = new NumberProposedGroupsTogether(matching, pregroupings);
			
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
					
					pregroupProportion,
					pregroupDist,
					
					numPregroupingsTogether.asInt(),
					pregroupings.count()
			);
		}
		
		private static final WeakHashMap<HistoricalInstanceResult, Groups<?>> currentPregrouping = new WeakHashMap<>();
		private Groups<?> currentPregrouping()
		{
			synchronized (currentPregrouping) {
				var cached = currentPregrouping.get(this);
				if (cached == null) {
					cached = mechanism.pregroupingType().instantiateFor(datasetContext).groups();
					currentPregrouping.put(this, cached);
				}
				
				return cached;
			}
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
	
	@NotNull
	private static String groupSizesDist(Groups<?> pregroupings, int maxAllowedGroupSize)
	{
		return IntStream.rangeClosed(2, maxAllowedGroupSize)
		                .mapToObj(i -> Integer.toString(pregroupings.ofSize(i)
		                                                            .count() * i))
		                .collect(joining("|"));
	}
}