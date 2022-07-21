package nl.tudelft.aidm.optimalgroups.experiment.paper.fairness;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.experiment.paper.fairness.report.FairnessVsVanillaQualityExperimentReport;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FairnessVsVanillaQualityExperiments_SDU
{
	public static void main(String[] args)
	{
		var experimentsRunId = Instant.now().getEpochSecond();
		
		var pregroupingType = PregroupingType.sizedCliqueHardGrouped(10);
		
		var algorithms = List.of(
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(pregroupingType),
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), pregroupingType)
		);
		
		var datasets = List.of(
				SDUDatasetContext.instanceOfYear(2008)/*,
				SDUDatasetContext.instanceOfYear(2009),
				SDUDatasetContext.instanceOfYear(2010),
				SDUDatasetContext.instanceOfYear(2011),
				SDUDatasetContext.instanceOfYear(2012),
				SDUDatasetContext.instanceOfYear(2013),
				SDUDatasetContext.instanceOfYear(2014),
				SDUDatasetContext.instanceOfYear(2015),
				SDUDatasetContext.instanceOfYear(2016)*/
		);
		
		for (var datasetContext : datasets)
		{
			var pregrouping = pregroupingType.instantiateFor(datasetContext);
			
			var results = new ArrayList<GroupProjectAlgorithm.Result>();
			for (GroupProjectAlgorithm algorithm : algorithms)
			{
				var matching = algorithm.determineMatching(datasetContext);
				var result = new GroupProjectAlgorithm.Result(algorithm, matching);
				results.add(result);
			}
			
			var id  = "SDU_" + datasetContext.year;
			
			var fileName = String.format("fairness_%s_%s-%s", experimentsRunId, id, pregroupingType.canonicalName());
			
			new FairnessVsVanillaQualityExperimentReport(datasetContext, pregrouping, results)
					.writeAsHtmlToFile(new File("reports/thesis/" + fileName + ".html"));
			// result into document
			
		}
	}
}
