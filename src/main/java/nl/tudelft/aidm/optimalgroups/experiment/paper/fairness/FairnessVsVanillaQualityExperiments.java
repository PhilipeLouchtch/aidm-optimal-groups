package nl.tudelft.aidm.optimalgroups.experiment.paper.fairness;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.MultiTypeGeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.experiment.dataset.ResearchProject2021Q4Dataset;
import nl.tudelft.aidm.optimalgroups.experiment.paper.fairness.report.FairnessVsVanillaQualityExperimentReport;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FairnessVsVanillaQualityExperiments
{
	public static void main(String[] args)
	{
		var experimentsRunId = Instant.now().getEpochSecond();
		
		var pregroupingType = PregroupingType.sizedCliqueSoftGrouped(5);
		
		var algorithms = List.of(
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(pregroupingType),
			new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), pregroupingType)
		);
		
		var datasets = List.<DatasetContext>of(
				CourseEditionFromDb.fromLocalBepSysDbSnapshot(4),
				CourseEditionFromDb.fromLocalBepSysDbSnapshot(10),
				ResearchProject2021Q4Dataset.getInstance(),
				SDUDatasetContext.instanceOfYear(2012),
				MultiTypeGeneratedDataContext.makeNewWith40302010Types(50, 250, 1, new GroupSizeConstraint.Manual(4,5))
		);
		
		for (DatasetContext datasetContext : datasets)
		{
			var pregrouping = pregroupingType.instantiateFor(datasetContext);
			
			var results = new ArrayList<GroupProjectAlgorithm.Result>();
			for (GroupProjectAlgorithm algorithm : algorithms)
			{
				var matching = algorithm.determineMatching(datasetContext);
				var result = new GroupProjectAlgorithm.Result(algorithm, matching);
				results.add(result);
			}
			
			var id = datasetContext instanceof CourseEdition ? ((CourseEdition) datasetContext).bepSysId().toString() : datasetContext.identifier();
			
			var fileName = String.format("fairness_%s_CE(%s)-%s", experimentsRunId, id, pregroupingType.canonicalName());
			
			new FairnessVsVanillaQualityExperimentReport(datasetContext, pregrouping, results)
					.writeAsHtmlToFile(new File("reports/thesis/" + fileName + ".html"));
			// result into document
			
		}
	}
}
