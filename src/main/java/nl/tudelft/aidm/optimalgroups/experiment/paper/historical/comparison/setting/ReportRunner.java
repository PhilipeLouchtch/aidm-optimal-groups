package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.setting;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedTied;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class ReportRunner
{
	public static List<DatasetContext> TUDInstances()
	{
		return Stream.of(3, 4, 10, 11, 14, 17, 18, 23, 39, 42, 45)
		             .map(CourseEditionFromDb::fromLocalBepSysDbSnapshot)
		             .map(DatasetContext_AugmentedPreferences_AppendedTied::from)
		             .toList();
	}
	
	public static List<DatasetContext> SDUInstances()
	{
		return 	List.<DatasetContext>of(
				SDUDatasetContext.instanceOfYear(2008),
				SDUDatasetContext.instanceOfYear(2009),
				SDUDatasetContext.instanceOfYear(2010),
				SDUDatasetContext.instanceOfYear(2011),
				SDUDatasetContext.instanceOfYear(2012),
				SDUDatasetContext.instanceOfYear(2013),
				SDUDatasetContext.instanceOfYear(2014),
				SDUDatasetContext.instanceOfYear(2015),
				SDUDatasetContext.instanceOfYear(2016)
		);
	}
	
	public static String simple_name(String name)
	{
		var nameL = name.toLowerCase();
		
		if (nameL.contains("fair"))
			return "Fair";
		
		if (nameL.contains("chiarandini"))
			return "Chiarandini";
		if (nameL.contains("chiaranini"))
			return "Chiarandini";
		
		
		else throw new RuntimeException("Don't know short name for %s".formatted(name));
	}
	
	public static void main(String[] args)
	{
		runSingleTable(args);
	}
	
	public static void runSingleTable(String[] args)
	{
		var experimentsRunId = Instant.now().getEpochSecond();
		
		var datasets = TUDInstances();
		
		// For the experiment, we assume the following pregrouping-type ('except', 'any') to be true
		// and compare the results of running a mechanism with this model with the results of the mechanisms
		// if we were to assume so other model. Thus, we can look at the outcomes for pregrouping students
		// that would like to work, e.g. in a pair under a model where we actually know there are these
		// pregroupers and compare that to the outcome of a model where we choose to ignore this information
		// (strict business rule, or if the 'lesser' model is determined to have a better overal outcome)
//		var assumedTruePregroupingModel = PregroupingType.anyCliqueSoftGroupedEpsilon();
		
		enum Scenario {
			ANY(PregroupingType.anyCliqueSoftGroupedEpsilon()),
			EXCEPT(PregroupingType.exceptSubmaxCliqueSoftEpsGrouped()),
			MAX(PregroupingType.maxCliqueSoftGroupedEps());
			
			public final PregroupingType pregroupingType;
			
			Scenario(PregroupingType pregroupingType) {
				this.pregroupingType = pregroupingType;
			}
		}
	
		
		// The combinations mechanisms x pregrouping setting we would like to evaluate
		var scenarios = List.of(
				Scenario.ANY,
				Scenario.EXCEPT,
				Scenario.MAX
		);
		
		for (var scenario : scenarios)
		{
			List<GroupProjectAlgorithm> algos = List.of(
				new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), scenario.pregroupingType),
				new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(scenario.pregroupingType)
			);
			
			var fileName = String.format("pregroupscenario_%s_report_tud_%s", scenario.name(), experimentsRunId);
			var report = new AnyVsExceptVsMaxPregroupSingleTableReport(datasets, algos);
			report.writeAsHtmlToFile(new File("reports/thesis/" + fileName + ".html"));
		}
		
	}
	
	public static void runComparison(String[] args)
	{
		var experimentsRunId = Instant.now().getEpochSecond();
		
		var datasets = TUDInstances();
		
		// For the experiment, we assume the following pregrouping-type ('except', 'any') to be true
		// and compare the results of running a mechanism with this model with the results of the mechanisms
		// if we were to assume so other model. Thus, we can look at the outcomes for pregrouping students
		// that would like to work, e.g. in a pair under a model where we actually know there are these
		// pregroupers and compare that to the outcome of a model where we choose to ignore this information
		// (strict business rule, or if the 'lesser' model is determined to have a better overal outcome)
		var assumedTruePregroupingModel = PregroupingType.anyCliqueSoftGroupedEpsilon();
		
		// The combinations mechanisms x pregrouping setting we would like to evaluate
		List<GroupProjectAlgorithm> challengers = List.of(
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), assumedTruePregroupingModel),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(assumedTruePregroupingModel)
		);
		
		// against these combinations
		List<GroupProjectAlgorithm> oppos = List.of(
			// EXCEPT model - comment out if except is a challenger
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.exceptSubmaxCliqueSoftEpsGrouped()),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.exceptSubmaxCliqueSoftEpsGrouped()),
			
			// MAX model
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.maxCliqueSoftGroupedEps()),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.maxCliqueSoftGroupedEps())
		);
		
		for (GroupProjectAlgorithm challenger : challengers)
		{
			for (var datasetContext : datasets)
			{
				var datasetId =  datasetContext.identifier().replaceAll("^CourseEdition\\[(\\d+)].+$", "CE$1");
				var fileName = String.format("challenger_oppo_%s_%s_%s-%s", challenger.pregroupingType().canonicalName(), simple_name(challenger.name()), experimentsRunId,
				                            datasetId);
				
				new AnyVsExceptVsMaxPregroupReport(datasetContext, challenger, oppos, datasetId)
						.writeAsHtmlToFile(new File("reports/thesis/" + fileName + ".html"));
			}
		}
		
	}
}
