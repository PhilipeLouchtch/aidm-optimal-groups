package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.nogroup;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.GeneratedDataExperiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.*;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult.serializeProfile;

public class GroupSizeBoundsExperiment extends GeneratedDataExperiment<GroupSizeBoundsExperiment.DatasetParams>
{
	private static GroupedDatasetParams<DatasetParams> paramsForExperiments()
	{
		List<Integer> nums_students = List.of(50, 200, 600);
		List<PROJ_PRESSURE> project_pressure_levels = List.of(PROJ_PRESSURE.TIGHT, /*PROJ_PRESSURE.MID,*/ PROJ_PRESSURE.LOOSE);
//		List<Integer> nums_projects = List.of(5, 10, 20, 40, 60, 80, 100, 120, 140, 160, 180);
		List<Integer> nums_slots = List.of(1);
		
		List<NamedPrefGenerator> projectPrefGenerators = List.of(
				ProjPrefVariations.singleton(),
				ProjPrefVariations.linearPerturbedSlightly(),
				ProjPrefVariations.linearPerturbedMore(),
				ProjPrefVariations.random(),
				ProjPrefVariations.realistic()
		);
		
		List<GroupSizeConstraint> gscs = new ArrayList<>();
		
		for (int i = 2; i <= 6; i++)
		for (int j = 2; j <= 8; j++)
		{
			if (i < j && j - i <= 4) {
				// Min size must be lower than max size (by definition)
				// and: do not consider min = max sizes as this complicates the experiment
				// parameters - the number of students must then be dividable by the group-size
				// otherwise the "remainder" students cannot be matched which is not allowed (complete matchings)
				var gsc = GroupSizeConstraint.manual(i, j);
				gscs.add(gsc);
			}
		}
		
		var pregroupingGen = new NamedPregroupingGenerator(PregroupingGenerator.none(), "none");
		
		var paramGroups = new ArrayList<GroupedDatasetParams.Group<DatasetParams>>();
		
		for (var num_students : nums_students)
			for (var project_pressure : project_pressure_levels)
			{
				var datasetParamGroup = new ArrayList<DatasetParams>(nums_slots.size() * gscs.size() * projectPrefGenerators.size());
				
				for (var num_slots : nums_slots)
					for (var gsc : gscs)
						for (var projPrefsGen : projectPrefGenerators)
						{
							var minProjectAmount = new MinimumReqProjectAmount(gsc, num_students);
							var num_projects = (int) Math.ceil(minProjectAmount.asInt() * project_pressure.factor / num_slots);
							
							var paramsForDataset = new DatasetParams(num_students, num_projects, num_slots, gsc, projPrefsGen, pregroupingGen, project_pressure);
							
							datasetParamGroup.add(paramsForDataset);
						}
				
				String groupName = "stud[%s]_pressure[%s]".formatted(num_students, project_pressure.name());
				paramGroups.add(new GroupedDatasetParams.Group<>(groupName, datasetParamGroup));
			}
		
		return new GroupedDatasetParams<>(paramGroups);
	}
	
	public GroupSizeBoundsExperiment(String identifier, List<GroupProjectAlgorithm> algos, int numToGenPerParam, int runs)
	{
		super(identifier, paramsForExperiments(), algos, numToGenPerParam, runs);
	}
	
	@Override
	protected ExperimentResultsCollector newExperimentResultsCollector(String filePath)
	{
		return new ExperimentResultsFile(filePath);
	}
	
	@Override
	protected nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult newExperimentSubResult(
			DatasetParams params, DatasetContext datasetContext, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum)
	{
		return new ExperimentSubResult(params, mechanism, matching, runtime, trialRunNum);
	}
	
	
	public record DatasetParams(Integer numStudents, Integer numProjects, Integer numSlotsPerProj, GroupSizeConstraint gsc, NamedPrefGenerator prefGenerator, NamedPregroupingGenerator pregroupingGenerator, PROJ_PRESSURE proj_pressure) implements nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.DatasetParams
	{
		@Override
		public DatasetContext intoNewlyGeneratedDataset()
		{
			var projects = Projects.generated(numProjects, numSlotsPerProj);
			var prefGenerator = this.prefGenerator.makeGeneratorFor(projects);
			
			return new GeneratedDataContext(numStudents, projects, gsc, prefGenerator, pregroupingGenerator.generator());
		}
		
		@Override
		public String toString()
		{
			return "DatasetParams[ st#%s, pr#%s[%s]-[%s], pp[%s], gp[%s], gsc[%s] ]"
					.formatted(
							numStudents,
							numProjects,
							numSlotsPerProj,
							proj_pressure,
							prefGenerator.shortName(),
							pregroupingGenerator().shortName(),
							"%s,%s".formatted(gsc.minSize(), gsc.maxSize())
					);
		}
	}
	
	
	record ExperimentSubResult(DatasetParams params, GroupProjectAlgorithm mechanism, GroupToProjectMatching<?> matching, Duration runtime, Integer trialRunNum) implements nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult
	{
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
					"gsc_min",
					"gsc_max"
			);
		}
		
		@Override
		public List<Object> columnValues()
		{
			return List.of(
					params.numStudents,
					params.numProjects,
					params.numSlotsPerProj,
					
					params.prefGenerator.shortName(),
					params.pregroupingGenerator.shortName(),
					mechanism.name(),
					trialRunNum,
					
					runtime.toMillis(),
					
					serializeProfile(profileAllStudents()),
					serializeProfile(profileSingles()),
					serializeProfile(profilePregrouped()),
					serializeProfile(profileUnsatpregroup()),
					
					params.proj_pressure.name(),
					params.gsc.minSize(),
					params.gsc.maxSize()
			);
		}
		
		public Profile profileAllStudents()
		{
			return Profile.of(
					AgentToProjectMatching.from(this.matching())
			);
		}
		
		public Profile profileSingles()
		{
			return Profile.fromProfileArray();
		}
		
		public Profile profilePregrouped()
		{
			return Profile.fromProfileArray();
		}
		
		public Profile profileUnsatpregroup()
		{
			return Profile.fromProfileArray();
		}
	}
}
