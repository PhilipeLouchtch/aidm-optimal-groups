package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.MultiTypeProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.MultiTypeProjectPreferencesGenerator.Type;
import nl.tudelft.aidm.optimalgroups.dataset.generated.prefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.*;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.NumberProposedGroupsTogether;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult.serializeProfile;

public class PregroupingMaxSoftExperiment extends GeneratedDataExperiment<PregroupingMaxSoftExperiment.MaxPregroupingsDatasetParams>
{
	enum PREGROUP_PREF_DIST
	{
		IDENTICAL,
		DIFFERENT,
		MIX
	}
	
	private static GroupedDatasetParams<MaxPregroupingsDatasetParams> paramsForExperiments()
	{
		List<Integer> nums_students = List.of(600, 200, 50);
		List<PROJ_PRESSURE> project_pressure_levels = List.of(PROJ_PRESSURE.TIGHT, PROJ_PRESSURE.MID, PROJ_PRESSURE.LOOSE);
//		List<Integer> nums_projects = List.of(5, 10, 20, 40, 60, 80, 100, 120, 140, 160, 180);
		List<Integer> nums_slots = List.of(1);
		
		List<Integer> pregrouping_proportions = List.of(/*0, */ 10, /*20,*/ 30,/* 40, 50,*/ 60, /*70, 80,*/ 90);//, 100);
		List<PREGROUP_PREF_DIST> pregrouping_proj_pref_types = List.of(PREGROUP_PREF_DIST.IDENTICAL, PREGROUP_PREF_DIST.DIFFERENT, PREGROUP_PREF_DIST.MIX);
		
		var gsc = GroupSizeConstraint.manual(4,5);
		
		List<NamedPrefGenerator> proj_pref_gens = List.of(
//				ProjPrefVariations.singleton(),
				ProjPrefVariations.linearPerturbedSlightly(),
				ProjPrefVariations.linearPerturbedMore(),
//				ProjPrefVariations.random(),
				ProjPrefVariations.realistic()
		);
		
		var paramGroups = new ArrayList<GroupedDatasetParams.Group<MaxPregroupingsDatasetParams>>();
		
		for (var num_students : nums_students)
		for (var proj_pref_gen : proj_pref_gens)
		for (var pregrouping_proj_pref_type : pregrouping_proj_pref_types)
		{
			var datasetParamGroup = new ArrayList<MaxPregroupingsDatasetParams>(nums_slots.size() * proj_pref_gens.size());
			
			for (var pregrouping_proportion : pregrouping_proportions)
			for (var num_slots : nums_slots)
			for (var project_pressure : project_pressure_levels)
			{
				var minProjectAmount = new MinimumReqProjectAmount(gsc, num_students);
				var num_projects = (int) Math.ceil(minProjectAmount.asInt() * project_pressure.factor / num_slots);
				
				var grp_size = 5;
				var exp_pregroups = num_students * (pregrouping_proportion / 100.0) / grp_size;
				var chance = exp_pregroups / (num_students - (grp_size - 1) * exp_pregroups);
				
				var pregroupingGen = new NamedPregroupingGenerator(
						PregroupingGenerator.singlePregroupingSizeOnly(grp_size, chance),
						pregrouping_proportion + " %"
				);
				
				var paramsForDataset = new MaxPregroupingsDatasetParams(num_students, num_projects, num_slots, gsc, proj_pref_gen, pregroupingGen, project_pressure, pregrouping_proj_pref_type);
				datasetParamGroup.add(paramsForDataset);
			}
			
			String groupName = "stud[%s]_pp[%s]_gpp[%s]".formatted(num_students, proj_pref_gen.shortName(), pregrouping_proj_pref_type);
			paramGroups.add(new GroupedDatasetParams.Group<>(groupName, datasetParamGroup));
		}
		
		return new GroupedDatasetParams<>(paramGroups);
	}
	
	public PregroupingMaxSoftExperiment(String identifier, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		super(identifier, paramsForExperiments(), algos, numToGenPerParam, runs);
	}
	
	@Override
	protected ExperimentResultsCollector newExperimentResultsCollector(String filePath)
	{
		return new ExperimentResultsFile(filePath);
	}
	
	@Override
	protected ExperimentSubResult newExperimentSubResult(MaxPregroupingsDatasetParams params, DatasetContext datasetContext, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum)
	{
		return new MaxPregroupingsExperimentSubResult(params, datasetContext, mechanism, matching, runtime, trialRunNum);
	}
	
	
	public record MaxPregroupingsDatasetParams(
			Integer numStudents, Integer numProjects, Integer numSlotsPerProj, GroupSizeConstraint gsc,
			NamedPrefGenerator prefGenerator, NamedPregroupingGenerator pregroupingGenerator,
			PROJ_PRESSURE proj_pressure, PREGROUP_PREF_DIST pregroup_pref_dist
	) implements DatasetParams
	{
		@Override
		public DatasetContext intoNewlyGeneratedDataset()
		{
			var projects = Projects.generated(numProjects, numSlotsPerProj);
			var prefGenerator = this.prefGenerator.makeGeneratorFor(projects);
			
			var pregroupProjPrefGenerator = switch (pregroup_pref_dist) {
				case IDENTICAL -> prefGenerator; // same distribution
				case DIFFERENT -> this.prefGenerator.makeGeneratorFor(projects); // same distribution TYPE, but newly drawn
				// 50-50 mix of exactly the same as solo students and newly drawn
				case MIX -> new MultiTypeProjectPreferencesGenerator(new Type(prefGenerator, 0.5), new Type(this.prefGenerator.makeGeneratorFor(projects), 0.5));
			};
			
			// do the pregrouping pref dist selection here...
			
			return new GeneratedDataContext(numStudents, projects, gsc, prefGenerator, pregroupProjPrefGenerator, pregroupingGenerator.generator());
		}
		
		@Override
		public String toString()
		{
			return "DatasetParams[ st#%s, pr#%s[%s]-[%s], pp[%s], gpp[%s], gp[%s] ]"
					.formatted(
							numStudents,
							numProjects,
							numSlotsPerProj,
							proj_pressure,
							prefGenerator.shortName(),
							pregroup_pref_dist,
							pregroupingGenerator().shortName()
					);
		}
	}
	
	
	record MaxPregroupingsExperimentSubResult(
			MaxPregroupingsDatasetParams params, DatasetContext datasetContext, GroupProjectAlgorithm mechanism,
			GroupToProjectMatching<?> matching,
			Duration runtime, Integer trialRunNum
	) implements ExperimentSubResult
	{
		private final static WeakHashMap<DatasetContext, Groups<?>> pregroupingsCache = new WeakHashMap<>();
		
		private static synchronized Groups<?> pregroupingFor(DatasetContext datasetContext)
		{
			return pregroupingsCache.computeIfAbsent(datasetContext, d -> new CliqueGroups(d.allAgents()));
		}
		
		private Groups<?> currentPregrouping()
		{
			return pregroupingFor(datasetContext);
		}
		
		@Override
		public List<Object> columnHeaders()
		{
			return List.of(
					"num_students",
					"num_projects",
					"num_slots",
					
					"proj_pref_type",
					"pregroup_type",
					"mechanism",
					"trial",
					
					"duration_ms",
					
					"profile_all",
					"profile_singles",
					"profile_pregrouped",
					"profile_unsatpregroup",
					
					"project_pressure",
					"pregroup_proj_pref_type",
					
					"num_pregroups_fully_together",
					"num_pregroups_max"
			);
		}
		
		@Override
		public List<Object> columnValues()
		{
			return List.of(
					params().numStudents(),
					params().numProjects(),
					params().numSlotsPerProj(),
					
					params().prefGenerator().shortName(),
					params().pregroupingGenerator().shortName(),
					mechanism().name(),
					trialRunNum(),
					
					runtime().toMillis(),
					
					serializeProfile(profileAllStudents()),
					serializeProfile(profileSingles()),
					serializeProfile(profilePregrouped()),
					serializeProfile(profileUnsatpregroup()),
					
					params().proj_pressure.name(),
					params().pregroup_pref_dist().name(),
					
					numPregroupsFullyTogether(),
					numPregroupsMax()
			);
		}
		
		public int numPregroupsFullyTogether()
		{
			var numPreformedGroupsTogether = new NumberProposedGroupsTogether(matching, currentPregrouping());
			return numPreformedGroupsTogether.asInt();
		}
		
		public int numPregroupsMax()
		{
			return currentPregrouping().count();
		}
		
		public Profile profileAllStudents()
		{
			return Profile.of(
					AgentToProjectMatching.from(this.matching())
			);
		}
		
		public Profile profileSingles()
		{
			var preformedGroups = currentPregrouping();
			
			var soloStudents = datasetContext.allAgents().without( preformedGroups.asAgents() );
			var matchingSubsetOfSoloStudents = AgentToProjectMatching.from( matching() )
			                                                         .filteredBy(soloStudents);
			
			return Profile.of(matchingSubsetOfSoloStudents);
		}
		
		public Profile profilePregrouped()
		{
			var preformedGroups = currentPregrouping();
			var agentsPregrouping = preformedGroups.asAgents();
			
			var matchingPregroupedSatisfied = AgentToProjectMatching.from( matching.filteredBySubsets(preformedGroups) )
			                                                        .filteredBy(agentsPregrouping);
		
			var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
			
			return Profile.of(matchingPregroupedSatisfied);
		}
		
		public Profile profileUnsatpregroup()
		{
			var preformedGroups = currentPregrouping();
			var agentsPregrouping = preformedGroups.asAgents();
			
			var matchingIndividuals = AgentToProjectMatching.from(matching());
			var matchingPregroupedSatisfied = AgentToProjectMatching.from( matching.filteredBySubsets(preformedGroups) )
			                                                        .filteredBy(agentsPregrouping);
			
			var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
			var pregroupingStudentsUnsatisfied = agentsPregrouping.without(pregroupingStudentsSatisfied);
			var matchingPregroupedUnsatisfied = matchingIndividuals.filteredBy(pregroupingStudentsUnsatisfied);
			
			return Profile.of(matchingPregroupedUnsatisfied);
		}
	}
}
