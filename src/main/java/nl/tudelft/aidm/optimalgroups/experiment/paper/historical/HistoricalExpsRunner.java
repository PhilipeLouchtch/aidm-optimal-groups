package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedTied;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.experiment.paper.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.WarmupExperiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group.mechanism.Chiarandini_Fairgroups_EpsilonConstraintVersion;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.List;
import java.util.stream.Stream;

public class HistoricalExpsRunner implements Experiment
{
	public static List<GroupProjectAlgorithm> groupingMechanisms()
	{
		var bepsys = new GroupProjectAlgorithm.BepSys_reworked(PregroupingType.anyCliqueSoftGrouped());
		var normal_hard = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.anyCliqueSoftGrouped());
		var fairness_soft_eps = new Chiarandini_Fairgroups_EpsilonConstraintVersion(new OWAObjective(), PregroupingType.anyCliqueSoftGroupedEpsilon());
//		var fairness_none = new Chiarandini_FairgroupsNEW(new OWAObjective(), PregroupingType.anyCliqueSoftGrouped());
		
		var mechanisms = List.<GroupProjectAlgorithm>of(bepsys, normal_hard, fairness_soft_eps);
		return mechanisms;
	}
	
	static final int runsPerDataset = 3;
	
	@Override
	public void run()
	{
		// ez warmup experiment first
//		new WarmupExperiment(groupingMechanisms()).run();
		
		new BepSys_PF_SDU_HistoricalInstancesExperiment("Historical_TUD", allTUDInstances(), groupingMechanisms(), runsPerDataset)
				.run();
	}
	
	public List<DatasetContext> allTUDInstances()
	{
		return Stream.of(3, 4, 10, 11, 14, 17, 18, 23, 39, 42, 45)
		             .map(CourseEditionFromDb::fromLocalBepSysDbSnapshot)
			         .map(DatasetContext_AugmentedPreferences_AppendedTied::from)
		             .toList();
	}
	
	public static void main(String[] args)
	{
		new HistoricalExpsRunner().run();
	}
}
