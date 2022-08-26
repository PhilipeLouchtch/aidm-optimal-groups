package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.setting;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedTied;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.NumberPregroupingStudentsTogether;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.Groups;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AnyExceptMaxCsvReport
{
	/*
		I wish to export the following data per {Edition} x {Mechanism}:
		
		| Edition | Mechanism | true-solo studs profile |
			foreach pregroupClass in {max, small, sub-max}:
				| profile satisfied | profile unsatisfied | together |
	 */
	
	public static List<DatasetContext> TUDInstances()
	{
		return Stream.of(3, 4, 10, 11, 14, 17, 18, 23, 39, 42, 45)
		             .map(CourseEditionFromDb::fromLocalBepSysDbSnapshot)
		             .map(DatasetContext_AugmentedPreferences_AppendedTied::from)
		             .toList();
	}
	
	// determine solo
	// determine pregroup by class
	// implement profile satisfied/unsatisfied/together
	// export to csv
	
	public static void main(String[] args)
	{
		var datasets = TUDInstances();
		
		// For the experiment, we assume the following pregrouping-type ('except', 'any') to be true
		// and compare the results of running a mechanism with this model with the results of the mechanisms
		// if we were to assume so other model. Thus, we can look at the outcomes for pregrouping students
		// that would like to work, e.g. in a pair under a model where we actually know there are these
		// pregroupers and compare that to the outcome of a model where we choose to ignore this information
		// (strict business rule, or if the 'lesser' model is determined to have a better overal outcome)
		var assumedTruePregroupingModel = PregroupingType.anyCliqueSoftGroupedEpsilon();
		
		
		// against these combinations
		List<GroupProjectAlgorithm> mechanisms = List.of(
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), assumedTruePregroupingModel),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(assumedTruePregroupingModel),
			
			// EXCEPT model - comment out if except is a challenger
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.exceptSubmaxCliqueSoftEpsGrouped()),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.exceptSubmaxCliqueSoftEpsGrouped()),
			
			// MAX model
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.maxCliqueSoftGroupedEps()),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.maxCliqueSoftGroupedEps())
		);
		
		var results = mechanisms.stream().flatMap(mechanism -> datasets.stream().map(dataset -> henk(dataset, mechanism)))
				.toList();
		
		var file = new File("reports/" + "anyexcept_tud" + ".csv");
		write(results, file);
	}
	
	public static Result henk(DatasetContext dataset, GroupProjectAlgorithm mechanism)
	{
		var pregrouping = PregroupingType.anyCliqueSoftGrouped().instantiateFor(dataset);
		
		var pregroupingStudents = pregrouping.groups().asAgents();
		var soloStudents = dataset.allAgents().without(pregroupingStudents);
		
		var sizesMax = Set.of(dataset.groupSizeConstraint().maxSize());
		var sizesSubmax = Set.of(dataset.groupSizeConstraint().maxSize() - 1);
		
		var sizesSmall = IntStream.rangeClosed(dataset.groupSizeConstraint().minSize(), dataset.groupSizeConstraint().maxSize()).boxed().collect(Collectors.toSet());
		sizesSmall.removeAll(sizesMax);
		sizesSmall.removeAll(sizesSubmax);
		
		var matching = mechanism.determineMatching(dataset);
		
		var datasetId =  dataset.identifier().replaceAll("^CourseEdition\\[(\\d+)].+$", "CE$1");
		
		return new Result(datasetId, mechanism.name(),
		                  Profile.of(AgentToProjectMatching.from(matching).filteredBy(soloStudents)),
		                  new PregroupClassStats(matching, keepOfClass(pregrouping, sizesMax)),
		                  new PregroupClassStats(matching, keepOfClass(pregrouping, sizesSmall)),
		                  new PregroupClassStats(matching, keepOfClass(pregrouping, sizesSubmax))
		);
	}
	
	record Result(String edition, String mechanismName, Profile solo, PregroupClassStats max, PregroupClassStats small, PregroupClassStats submax) { }
	
	public static void write(List<Result> results, File file)
	{
		var headers =
				List.of("edition", "mechanism", "solo_profile",
				        "max_profile_sat", "max_profile_unsat", "max_together", "max_count",
				        "small_profile_sat", "small_profile_unsat", "small_together", "small_count",
				        "submax_profile_sat", "submax_profile_unsat", "submax_together", "submax_count");
		
		
		try (var writer = new FileWriter(file))
		{
			for (var header : headers)
			{
				writer.write(header);
				writer.write(",");
			}
			writer.write("\n");
			
			for (Result result : results)
			{
				writer.write(result.edition);
				writer.write(",");
				writer.write(result.mechanismName);
				writer.write(",");
				writer.write(ExperimentSubResult.serializeProfile(result.solo));
				writer.write(",");
				
				writer.write(ExperimentSubResult.serializeProfile(result.max.satisfied()));
				writer.write(",");
				writer.write(ExperimentSubResult.serializeProfile(result.max.unsatisfied()));
				writer.write(",");
				writer.write(result.max.together().asInt());
				writer.write(",");
				writer.write(result.max.count());
				writer.write(",");
				
				writer.write(ExperimentSubResult.serializeProfile(result.small.satisfied()));
				writer.write(",");
				writer.write(ExperimentSubResult.serializeProfile(result.small.unsatisfied()));
				writer.write(",");
				writer.write(result.small.together().asInt());
				writer.write(",");
				writer.write(result.small.count());
				writer.write(",");
				
				writer.write(ExperimentSubResult.serializeProfile(result.submax.satisfied()));
				writer.write(",");
				writer.write(ExperimentSubResult.serializeProfile(result.submax.unsatisfied()));
				writer.write(",");
				writer.write(result.submax.together().asInt());
				writer.write(",");
				writer.write(result.submax.count());
				writer.write("\n");
			}
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}
		
	}
	
	static record PregroupClass(Groups<? extends Group> groups) {};
	
	static PregroupClass keepOfClass(Pregrouping pregrouping, Set<Integer> sizes)
	{
		var pregroupingGroupOfClass = pregrouping.groups().ofSizes(sizes.toArray(new Integer[0]));
		return new PregroupClass(pregroupingGroupOfClass);
	}
	
	static class PregroupClassStats
	{
		private GroupToProjectMatching<?> matching;
		private PregroupClass pregroupClass;
		
		public PregroupClassStats(GroupToProjectMatching<?> matching, PregroupClass pregroupClass)
		{
			this.matching = matching;
			this.pregroupClass = pregroupClass;
		}
		
		public NumberPregroupingStudentsTogether together()
		{
			return new NumberPregroupingStudentsTogether(matching, pregroupClass.groups());
		}
		
		public int count()
		{
			return pregroupClass.groups.asAgents().count();
		}
		
		private Agents satisfiedAgents()
		{
			var satisfied = pregroupClass.groups.asCollection().stream()
                                    .filter(group -> group.isTogetherIn(matching))
					                .flatMap(group -> group.members().asCollection().stream())
					                .collect(Agents.collector);
			
			return satisfied;
		}
		
		public Profile satisfied()
		{
			return Profile.of(AgentToProjectMatching.from(matching).filteredBy(satisfiedAgents()));
		}
		
		public Profile unsatisfied()
		{
			var unsatisfiedAgents = pregroupClass.groups().asAgents().without(satisfiedAgents());
			return Profile.of(AgentToProjectMatching.from(matching).filteredBy(unsatisfiedAgents));
		}
	}
}
