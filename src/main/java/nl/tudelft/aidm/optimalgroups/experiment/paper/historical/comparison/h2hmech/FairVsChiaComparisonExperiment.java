package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.h2hmech;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedTied;
import nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.MatchingByStudentTypes;
import nl.tudelft.aidm.optimalgroups.experiment.viz.FairVsChiaComparisonTable;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FairVsChiaComparisonExperiment
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
	
	public static void main(String[] args)
	{
		var experimentsRunId = Instant.now().getEpochSecond();
		
//		var id  = "TUD-max";
//		var pregroupingType = PregroupingType.maxCliqueSoftGroupedEps();
		
//		var id  = "TUD-except";
//		var pregroupingType = PregroupingType.exceptSubmaxCliqueSoftEpsGrouped();

//		var id  = "TUD-any";
//		var pregroupingType = PregroupingType.anyCliqueSoftGroupedEpsilon();
		
		record ExperimentRunParams(String id, PregroupingType pregroupingType) {}
		
		var datasets = TUDInstances();
		
		for (var params : List.of(
				new ExperimentRunParams("TUD-any", PregroupingType.anyCliqueSoftGroupedEpsilon()),
                new ExperimentRunParams("TUD-except", PregroupingType.exceptSubmaxCliqueSoftEpsGrouped()),
				new ExperimentRunParams("TUD-max", PregroupingType.maxCliqueSoftGroupedEps()) ) )
		{
			runExperiment(experimentsRunId, params.id(), params.pregroupingType(), datasets);
		}
	}
	
	public static void runExperiment(Long experimentsRunId, String id, PregroupingType pregroupingType, List<DatasetContext> datasets)
	{
		var fairnessAlgo = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), pregroupingType);
		var vanillAlgo = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(pregroupingType);
		
//		var datasets = SDUInstances();
		
		var resultsAll = new ArrayList<FairVsChiaComparisonTable.Result>();
		var resultsSingles = new ArrayList<FairVsChiaComparisonTable.Result>();
		var resultsPregrouped = new ArrayList<FairVsChiaComparisonTable.Result>();
		var resultsPregroupingUnsatisfied = new ArrayList<FairVsChiaComparisonTable.Result>();
		
		for (var datasetContext : datasets)
		{
			var pregrouping = pregroupingType.instantiateFor(datasetContext);
			
			var matchingFairness = fairnessAlgo.determineMatching(datasetContext);
			var matchingChia = vanillAlgo.determineMatching(datasetContext);
			
			var resultAll = new FairVsChiaComparisonTable.Result(
					AgentToProjectMatching.from(matchingFairness),
					AgentToProjectMatching.from(matchingChia)
			);
			resultsAll.add(resultAll);
			
			// result into document
			var matchingFairnessByStudentTypes = MatchingByStudentTypes.from(matchingFairness, pregrouping);
			var matchingVanillaByStudentTypes = MatchingByStudentTypes.from(matchingChia, pregrouping);
			
			var resultSingles = new FairVsChiaComparisonTable.Result(matchingFairnessByStudentTypes.singles(), matchingVanillaByStudentTypes.singles());
			resultsSingles.add(resultSingles);
			
			var resultPregrouped = new FairVsChiaComparisonTable.Result(matchingFairnessByStudentTypes.pregrouped(), matchingVanillaByStudentTypes.pregrouped());
			resultsPregrouped.add(resultPregrouped);

			var resultPregroupingUnsat = new FairVsChiaComparisonTable.Result(matchingFairnessByStudentTypes.pregroupingUnsatisfied(), matchingVanillaByStudentTypes.pregroupingUnsatisfied());
			resultsPregroupingUnsatisfied.add(resultPregroupingUnsat);
			
		}
		
		var fileName = String.format("comparison_fair_chia_%s_%s-%s", experimentsRunId, id, pregroupingType.canonicalName());
		
		new FairVsChiaTableReport(pregroupingType, resultsAll, resultsSingles, resultsPregrouped, resultsPregroupingUnsatisfied)
				.writeAsHtmlToFile(new File("reports/thesis/" + fileName + ".html"));
	}
	
}
