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
import java.io.Writer;
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
		
		var sizesSmall = IntStream.rangeClosed(2, dataset.groupSizeConstraint().maxSize()).boxed().collect(Collectors.toSet());
		
		sizesSmall.removeAll(sizesMax);
		sizesSmall.removeAll(sizesSubmax);
		
		var datasetId =  dataset.identifier().replaceAll("^CourseEdition\\[(\\d+)].+$", "CE$1");
		System.out.printf("Running dataset %s on mechanism %s...", datasetId, mechanism.name());
		
		var matching = mechanism.determineMatching(dataset);
		
		var result = new Result(datasetId, mechanism.name(),
		                  Profile.of(AgentToProjectMatching.from(matching).filteredBy(soloStudents)),
		                  new PregroupClassStats(matching, keepOfClass(pregrouping, sizesMax)),
		                  new PregroupClassStats(matching, keepOfClass(pregrouping, sizesSmall)),
		                  new PregroupClassStats(matching, keepOfClass(pregrouping, sizesSubmax))
		);
		
		System.out.println("done");
		
		return result;
	}
	
	record Result(String edition, String mechanismName, Profile solo, PregroupClassStats max, PregroupClassStats small, PregroupClassStats submax)
	{
		public String mechanism_name_simple()
		{
			var canonical = mechanismName.toLowerCase();
			
			if (canonical.contains("fair"))
			{
				return "FAIR";
			}
			
			if (canonical.contains("chiarandini") || canonical.contains("chiaranini"))
			{
				return "CHIA";
			}
			
			throw new RuntimeException("Cant determine short name for '%s'".formatted(canonical));
		}
		
		public String pregroup_scenario()
		{
			var canonical = mechanismName.toLowerCase();
			
			if (canonical.contains("any"))
				return "ANY";
				
			if (canonical.contains("except"))
				return "EXCEPT";
			
			if (canonical.contains("max"))
				return "MAX";
				
			throw new RuntimeException("Cant determine pregrouping scenario in '%s'".formatted(canonical));
		}
	}
	
	public static void write(List<Result> results, File file)
	{
		var headers =
				List.of("edition", "mechanism", "scenario", "profile_solo_sat",
				        "profile_max_sat", "profile_max_unsat", "max_together", "max_count",
				        "profile_small_sat", "profile_small_unsat", "small_together", "small_count",
				        "profile_submax_sat", "profile_submax_unsat", "submax_together", "submax_count");
		
		
		try (var writer = new FileWriter(file))
		{
			var headersAsLine = headers.stream().collect(Collectors.joining(",", "","\n"));
			writer.write(headersAsLine);
			
			for (Result result : results)
			{
				writeUsing(writer,
						   
				           result.edition,
				           result.mechanism_name_simple(),
				           result.pregroup_scenario(),
						   
				           ExperimentSubResult.serializeProfile(result.solo),
							
				           ExperimentSubResult.serializeProfile(result.max.satisfied()),
				           ExperimentSubResult.serializeProfile(result.max.unsatisfied()),
				           result.max.together().toString(),
				           result.max.count().toString(),
							
				           ExperimentSubResult.serializeProfile(result.small.satisfied()),
				           ExperimentSubResult.serializeProfile(result.small.unsatisfied()),
				           result.small.together().toString(),
				           result.small.count().toString(),
							
				           ExperimentSubResult.serializeProfile(result.submax.satisfied()),
				           ExperimentSubResult.serializeProfile(result.submax.unsatisfied()),
				           result.submax.together().toString(),
				           result.submax.count().toString()
				);
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
		
		public Integer count()
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
	
	public static void writeUsing(Writer writer, String... fields) throws IOException
	{
		for (int i = 0; i < fields.length; i++)
		{
			var field = fields[i];
			writer.write(field);
			
			if (i < fields.length-1)
				writer.write(",");
			else
				writer.write("\n");
		}
	}
}
