package nl.tudelft.aidm.optimalgroups.experiment.agp;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedRandomly;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.NormallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.UniformProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.agp.datasets.ThesisDatasets;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.ExperimentReportInHtml;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Experiment_Chiarandini_etc_180221
{
	public static void main(String[] args)
	{
		var experimentsForInReport = new ArrayList<Experiment>();

		List<GroupProjectAlgorithm> algorithms = List.of(
			new GroupProjectAlgorithm.BepSys_reworked(),
//			new GroupProjectAlgorithm.BepSys_reworkedGroups_minimizeIndividualDisutility(),
//			new GroupProjectAlgorithm.CombinedPrefs(),
//			new GroupProjectAlgorithm.RSD(),
//			new GroupProjectAlgorithm.ILPPP(),
//			new GroupProjectAlgorithm.BB_SDPC(),
			new GroupProjectAlgorithm.SDPCWithSlots(),
			new GroupProjectAlgorithm.Pessimism(),
			new GroupProjectAlgorithm.MinizincMIP(),
			new GroupProjectAlgorithm.Chiarandini_Utilitarian_MinSum_IdentityScheme(PregroupingType.anyCliqueHardGrouped()),
			new GroupProjectAlgorithm.Chiarandini_Stable_Utilitarian_MinSum_IdentityScheme(PregroupingType.anyCliqueHardGrouped()),
			new GroupProjectAlgorithm.Chiarandini_Utilitarian_MinSum_ExpScheme(PregroupingType.anyCliqueHardGrouped()),
			new GroupProjectAlgorithm.Chiarandini_Stable_Utilitarian_MinSum_ExpScheme(PregroupingType.anyCliqueHardGrouped()),
			new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.anyCliqueHardGrouped()),
			new GroupProjectAlgorithm.Chiaranini_Stable_MiniMax_OWA(PregroupingType.anyCliqueHardGrouped())
//			new GroupProjectAlgorithm.SDPCWithSlots_potential_numgroupmates_ordered()
		);

			/*new ILPPP_TGAlgorithm()*/ // will not succeed on CE10

		var groupSize = GroupSizeConstraint.manual(4, 5);

		/* CE 10 */
		experimentsForInReport.add(experimentCE10(algorithms));

		/* GENERATED DATA  */
//		experimentsForInReport.add(experimentSingleSlotTightMatchingCE10Like(algorithms, groupSize));

		/* */
//		experimentsForInReport.add(experimentThreeSlotsCE10Like(algorithms, groupSize));

		/* */
//		experimentsForInReport.add(experimentThreeSlotsUniformPrefs40p(algorithms, groupSize));

		/* */
//		experimentsForInReport.add(experimentCE10Like500Slots5(algorithms, groupSize));

		/* */
//		numSlots = 3;
//		numProjects = 40;
//		numAgents = numProjects * groupSize.maxSize();
//
//		projects = Projects.generated(40, numSlots);
//		prefGenerator = new NormallyDistributedProjectPreferencesGenerator(projects, 16);
//		dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator);
//
//		experiment = new Experiment(dataContext, algorithms);
//		experimentsForInReport.add(experiment);

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
			.writeHtmlSourceToFile(new File("reports/Experiment_Chiarandini_etc_180221.html"));

		return;
	}

	private static Experiment experimentCE10Like500Slots5(List<GroupProjectAlgorithm> algorithms, GroupSizeConstraint groupSize)
	{
		var dataContext = ThesisDatasets.CE10Like(500);

		var experiment = new Experiment(dataContext, algorithms);
		return experiment;
	}

	private static Experiment experimentThreeSlotsUniformPrefs40p(List<GroupProjectAlgorithm> algorithms, GroupSizeConstraint groupSize)
	{
		var numSlots = 3;
		var numProjects = 40;
		var numAgents = numProjects * groupSize.maxSize();

		var projects = Projects.generated(40, numSlots);
		var prefGenerator = new UniformProjectPreferencesGenerator(projects);
		var dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		var experiment = new Experiment(dataContext, algorithms);
		return experiment;
	}

	private static Experiment experimentThreeSlotsCE10Like(List<GroupProjectAlgorithm> algorithms, GroupSizeConstraint groupSize)
	{
		var numSlots = 3;
		var numProjects = 40;
		var numAgents = numProjects * groupSize.maxSize();

		var projects = Projects.generated(40, numSlots);
		var prefGenerator = new NormallyDistributedProjectPreferencesGenerator(projects, 4);
		var dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		var experiment = new Experiment(dataContext, algorithms);
		return experiment;
	}

	private static Experiment experimentSingleSlotTightMatchingCE10Like(List<GroupProjectAlgorithm> algorithms, GroupSizeConstraint groupSize)
	{
		var numSlots = 1;
		var numProjects = 40;
		var numAgents = numProjects * groupSize.maxSize();

		var projects = Projects.generated(40, numSlots);
		var prefGenerator = new NormallyDistributedProjectPreferencesGenerator(projects, 4);
		var dataContext = new GeneratedDataContext(numAgents, projects, groupSize, prefGenerator, PregroupingGenerator.none());

		var experiment = new Experiment(dataContext, algorithms);
		return experiment;
	}

	private static Experiment experimentCE10(List<GroupProjectAlgorithm> algorithms)
	{
		DatasetContext dataContext = DatasetContext_AugmentedPreferences_AppendedRandomly.from(CourseEditionFromDb.fromLocalBepSysDbSnapshot(10));

		var numSlots = 5;
		var numProjects = dataContext.allProjects().count();
		var numAgents = dataContext.allAgents().count();

		var projects = dataContext.allProjects();

		return new Experiment(dataContext, algorithms);
	}


}
