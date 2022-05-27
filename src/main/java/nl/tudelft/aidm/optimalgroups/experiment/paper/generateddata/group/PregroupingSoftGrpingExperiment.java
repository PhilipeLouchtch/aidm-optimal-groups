package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.agents.*;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.MultiTypeProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.MultiTypeProjectPreferencesGenerator.Type;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.GeneratedDataExperiment;
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
import plouchtch.assertion.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import static nl.tudelft.aidm.optimalgroups.dataset.generated.agents.ProportionalAgentGenerator.*;
import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group.PregroupingSoftGrpingExperiment.PREGROUP_SIZES_DISTRIBUTION.*;
import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult.serializeProfile;

public class PregroupingSoftGrpingExperiment extends GeneratedDataExperiment<PregroupingSoftGrpingExperiment.MaxPregroupingsDatasetParams>
{
	enum PREGROUP_PREF_DIST_TYPE
	{
		IDENTICAL,
		DIFFERENT,
		MIX
	}
	
	// Question: proportion groups or agents in these groups?
	// in other words, given a 100 pregrouping students,
	// does 0.5, 0.5 dist (2, 5) result in:
	//    - 14 grps of 2 and 14 grps of 5 - 28, 70
	//    - 25 grps of 2 and 10 grps of 5
	// the latter
	enum PREGROUP_SIZES_DISTRIBUTION
	{
		MAX_ONLY(0, 0, 0, 1),
		// max size dominant, tapers towards pairs
		MAX_TAPERED(0.1, 0.2, 0, 0.7),
		// most people are full team or pair
		REALISTIC(0.4, 0.2, 0, 0.4),
		// pair size dominant, tapers towards max-size pref,
		PAIR_TAPERED(0.6, 0.2, 0, 0.2);
		
		private final double[] dist;
		
		PREGROUP_SIZES_DISTRIBUTION(double... dist)
		{
			Assert.that(Arrays.stream(dist).sum() == 1.0).orThrowMessage("Pregroup size dist must sum to 1");
			this.dist = dist;
		}
		
		public AgentGenerator asGenerator(ProjectPreferenceGenerator projPrefGen)
		{
			var subGenerators = new ArrayList<>();
			
			for (int i = 0; i < this.dist.length; i++)
			{
				var pregroupSize = i + 2; // index 0: size 2, index 3: size 5
				var subGen = new PregroupingAgentsGenerator(pregroupSize, projPrefGen);
				subGenerators.add(subGen);
			}
			
			var arr = subGenerators.toArray(SubGen[]::new);
			return new ProportionalAgentGenerator(arr);
		}
	}
	
	record Proportion(double asDouble)
	{
		public String asString()
		{
			return ((int) asDouble * 100) + " %";
		}
	}
	
	private static GroupedDatasetParams<MaxPregroupingsDatasetParams> paramsForExperiments()
	{
		var nums_slots = List.of(1);
		
		var nums_students = List.of(600, 200, 50);
		var project_pressure_levels = List.of(PROJ_PRESSURE.TIGHT, PROJ_PRESSURE.MID, PROJ_PRESSURE.LOOSE);
		
		var pregrouping_proportions = Stream.of(0.1, 0.3, 0.6, 0.9).map(Proportion::new).toList();
		var pregrouping_proj_pref_types = List.of(PREGROUP_PREF_DIST_TYPE.IDENTICAL, PREGROUP_PREF_DIST_TYPE.DIFFERENT, PREGROUP_PREF_DIST_TYPE.MIX);
		
		var pregrouping_size_distributions = List.of(MAX_ONLY, MAX_TAPERED, REALISTIC, PAIR_TAPERED);
		
		var gsc = GroupSizeConstraint.manual(4,5);
		
		List<NamedPrefGenerator> proj_pref_gens = List.of(
//				ProjPrefVariations.singleton(),
//				ProjPrefVariations.linearPerturbedSlightly(),
				ProjPrefVariations.linearPerturbedMore(),
				ProjPrefVariations.random(),
				ProjPrefVariations.realistic()
		);
		
		var paramGroups = new ArrayList<GroupedDatasetParams.Group<MaxPregroupingsDatasetParams>>();
		
		for (var num_students : nums_students)
		for (var proj_pref_gen : proj_pref_gens)
		for (var pregrouping_proj_pref_type : pregrouping_proj_pref_types)
		{
			
			for (var pregrouping_sizes_dist : pregrouping_size_distributions)
			{
				// todo
				String groupName = "stud[%s]_pp[%s]_gpp[%s]_gd[%s]".formatted(num_students, proj_pref_gen.shortName(), pregrouping_proj_pref_type, pregrouping_sizes_dist);
				var datasetParamGroup = new GroupedDatasetParams.Group<MaxPregroupingsDatasetParams>(groupName, new ArrayList<>(nums_slots.size() * proj_pref_gens.size()));
				
				for (var pregrouping_proportion : pregrouping_proportions)
				for (var num_slots : nums_slots)
				for (var project_pressure : project_pressure_levels)
				{
					var minProjectAmount = new MinimumReqProjectAmount(gsc, num_students);
					var num_projects = (int) Math.ceil(minProjectAmount.asInt() * project_pressure.factor / num_slots);
					
					var paramsForDataset = new MaxPregroupingsDatasetParams(num_students, num_projects, num_slots, gsc,
					                                                        proj_pref_gen,
					                                                        pregrouping_proportion, project_pressure, pregrouping_proj_pref_type,
					                                                        pregrouping_sizes_dist
					);
					
					datasetParamGroup.add(paramsForDataset);
				}
				
				paramGroups.add(datasetParamGroup);
			}
		}
		
		return new GroupedDatasetParams<>(paramGroups);
	}
	
	public PregroupingSoftGrpingExperiment(String identifier, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
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
			NamedPrefGenerator prefGenerator,
			Proportion pregrouping_proportion,
			PROJ_PRESSURE proj_pressure,
			PREGROUP_PREF_DIST_TYPE pregroup_pref_dist_type,
			PREGROUP_SIZES_DISTRIBUTION pregroup_sizes_distribution
	) implements DatasetParams
	{
		@Override
		public DatasetContext intoNewlyGeneratedDataset()
		{
			var projects = Projects.generated(numProjects, numSlotsPerProj);
			
			var soloPrefGenerator = this.prefGenerator.makeGeneratorFor(projects);
			// Pick the project preferences for the pregrouping students
			var pregroupProjPrefGenerator = switch (pregroup_pref_dist_type) {
				case IDENTICAL -> soloPrefGenerator; // same distribution instance
				case DIFFERENT -> this.prefGenerator.makeGeneratorFor(projects); // same distribution TYPE, but newly drawn
				// 50-50 mix of exactly the same as solo students and newly drawn
				case MIX -> new MultiTypeProjectPreferencesGenerator(
						new Type(soloPrefGenerator, 0.5),
						new Type(this.prefGenerator.makeGeneratorFor(projects), 0.5)
				);
			};
			
			var agentGenerator = new SoloAndPregroupingAgentsGenerator(
					new SoloAgentGenerator(soloPrefGenerator),
					pregroup_sizes_distribution.asGenerator(pregroupProjPrefGenerator),
					pregrouping_proportion.asDouble()
			);
			
			return new GeneratedDataContext(numStudents, projects, gsc, agentGenerator);
		}
		
		@Override
		public String toString()
		{
			return "DatasetParams[ st#%s, pr[%s], pp[%s], gpp[%s], gp[%s], gd[%s] ]"
					.formatted(
							numStudents,
							proj_pressure,
							prefGenerator.shortName(),
							pregroup_pref_dist_type,
							pregrouping_proportion.asString(),
							pregroup_sizes_distribution.toString()
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
					params().pregrouping_proportion().asString(),
					mechanism().name(),
					trialRunNum(),
					
					runtime().toMillis(),
					
					serializeProfile(profileAllStudents()),
					serializeProfile(profileSingles()),
					serializeProfile(profilePregrouped()),
					serializeProfile(profileUnsatpregroup()),
					
					params().proj_pressure.name(),
					params().pregroup_pref_dist_type().name(),
					
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
