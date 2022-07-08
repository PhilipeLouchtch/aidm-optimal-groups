package nl.tudelft.aidm.optimalgroups.experiment.agp;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.NormallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.UniformProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.ExperimentReportInHtml;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SomeSimulations040520
{
	public static void main(String[] args)
	{
		var experimentsForInReport = new ArrayList<Experiment>();
		
		var pregroupingType = PregroupingType.anyCliqueHardGrouped();

		var algorithms = List.of(
			new GroupProjectAlgorithm.BepSys(pregroupingType),
			new GroupProjectAlgorithm.CombinedPrefs(),
			new GroupProjectAlgorithm.BEPSys_RSD(pregroupingType)
			/*new ILPPP_TGAlgorithm()*/); // will not succeed on CE10

		var groupSize = GroupSizeConstraint.manual(4, 5);

		/* CE 10 */
		DatasetContext dataContext = CourseEditionFromDb.fromLocalBepSysDbSnapshot(10);

		int numSlots = 5;
		int numProjects = dataContext.allProjects().count();
		int numAgents = dataContext.allAgents().count();

		var projects = dataContext.allProjects();

		var experiment = new Experiment(dataContext, algorithms);
		experimentsForInReport.add(experiment);


//		algorithms = List.of(
//			new BepSys_TGAlgorithm(),
//			new CombinedPrefs_TGAlgorithm(),
//			new ILPPP_TGAlgorithm());

		/* GENERATED DATA  */
		numSlots = 1;
		numProjects = 40;
		numAgents = numProjects * groupSize.maxSize();

		projects = Projects.generated(40, numSlots);
		ProjectPreferenceGenerator prefGenerator = new NormallyDistributedProjectPreferencesGenerator(projects, 4);
		dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		experiment = new Experiment(dataContext, algorithms);
		experimentsForInReport.add(experiment);

		/* */
		numSlots = 3;
		numProjects = 40;
		numAgents = numProjects * groupSize.maxSize();

		projects = Projects.generated(40, numSlots);
		prefGenerator = new NormallyDistributedProjectPreferencesGenerator(projects, 4);
		dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		experiment = new Experiment(dataContext, algorithms);
		experimentsForInReport.add(experiment);

		/* */
		numSlots = 3;
		numProjects = 40;
		numAgents = numProjects * groupSize.maxSize();

		projects = Projects.generated(40, numSlots);
		prefGenerator = new NormallyDistributedProjectPreferencesGenerator(projects, 16);
		dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		experiment = new Experiment(dataContext, algorithms);
		experimentsForInReport.add(experiment);

		/* */
		numSlots = 3;
		numProjects = 40;
		numAgents = numProjects * groupSize.maxSize();

		projects = Projects.generated(40, numSlots);
		prefGenerator = new UniformProjectPreferencesGenerator(projects);
		dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		experiment = new Experiment(dataContext, algorithms);
		experimentsForInReport.add(experiment);

//		/* */
//		numSlots = 3;
//		numProjects = 40;
//		numAgents = numProjects * groupSize.maxSize();
//
//		projects = Projects.generated(40, numSlots);
//		prefGenerator = new UniformProjectPreferencesGenerator(projects, 1);
//		generatedDataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator);
//
//		experiment = new Experiment(generatedDataContext, algorithms);
//
//		doc += Markdown.heading("Experiment - " + generatedDataContext.identifier()).toString() + "\n";
//		doc += datasetInfo(numAgents, numProjects, numSlots, groupSize);
//		doc += algoResultsInMarkdown(experiment.result().results);
//		doc += popularityInMarkdown(experiment.result().popularityMatrix);
//		doc += Markdown.rule();
//
//
//		/* */
//		numSlots = 3;
//		numProjects = 40;
//		numAgents = numProjects * groupSize.maxSize();
//
//		projects = Projects.generated(40, numSlots);
//		prefGenerator = new UniformProjectPreferencesGenerator(projects, 2);
//		generatedDataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator);
//
//		experiment = new Experiment(generatedDataContext, algorithms);
//
//		doc += Markdown.heading("Experiment - " + generatedDataContext.identifier()).toString() + "\n";
//		doc += datasetInfo(numAgents, numProjects, numSlots, groupSize);
//		doc += algoResultsInMarkdown(experiment.result().results);
//		doc += popularityInMarkdown(experiment.result().popularityMatrix);
//		doc += Markdown.rule();

//		var markdownAsString = markdown.toString();

		new ExperimentReportInHtml(experimentsForInReport)
			.writeHtmlSourceToFile(new File("reports/Simulations040520.html"));

		return;
	}


}
