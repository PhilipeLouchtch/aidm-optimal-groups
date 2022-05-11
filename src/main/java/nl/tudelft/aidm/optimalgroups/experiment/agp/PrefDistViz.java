package nl.tudelft.aidm.optimalgroups.experiment.agp;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.ExperimentReportInHtml;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations.*;

public class PrefDistViz
{
	public static void main(String[] args)
	{
		var experimentsForInReport = new ArrayList<Experiment>();

		var algorithms = List.of(
			new GroupProjectAlgorithm.BepSys(),
			new GroupProjectAlgorithm.BEPSys_RSD()
			/*new ILPPP_TGAlgorithm()*/); // will not succeed on CE10

		var groupSize = GroupSizeConstraint.manual(4, 5);
		
		
		for (var pref_type : List.of(singleton(), linearPerturbedSlightly(), linearPerturbedMore(), random(), realistic()))
		{
			for (Integer num_students : List.of(50, 200, 600))
			{
				int numProjects = new MinimumReqProjectAmount(groupSize, num_students).asInt();
				var projects = Projects.generated(numProjects, 1);
				
				var experiment = new Experiment(new GeneratedDataContext(num_students, projects, groupSize, pref_type.makeGeneratorFor(projects)), algorithms);
				experimentsForInReport.add(experiment);
			}
		}

		new ExperimentReportInHtml(experimentsForInReport)
			.writeHtmlSourceToFile(new File("reports/prefs_viz.html"));

		return;
	}


}
