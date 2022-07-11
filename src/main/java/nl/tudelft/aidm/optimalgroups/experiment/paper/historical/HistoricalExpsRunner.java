package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedTied;
import nl.tudelft.aidm.optimalgroups.experiment.paper.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.WarmupExperiment;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.List;
import java.util.stream.Stream;

public class HistoricalExpsRunner implements Experiment
{
	public static List<GroupProjectAlgorithm> groupingMechanisms_any()
	{
//		var bepsys = new GroupProjectAlgorithm.BepSys_reworked(PregroupingType.anyCliqueSoftGrouped());
		var chia = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.anyCliqueSoftGroupedEpsilon());
		var fairness_soft_eps = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.anyCliqueSoftGroupedEpsilon());
		
		var mechanisms = List.<GroupProjectAlgorithm>of(/*bepsys,*/ chia, fairness_soft_eps);
		return mechanisms;
	}
	
	public static List<GroupProjectAlgorithm> groupingMechanisms_max()
	{
		var bepsys = new GroupProjectAlgorithm.BepSys_reworked(PregroupingType.maxCliqueSoftGrouped());
		var chia = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.maxCliqueSoftGroupedEps());
		var fairness_soft_eps = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.maxCliqueSoftGroupedEps());
		
		var mechanisms = List.<GroupProjectAlgorithm>of(bepsys, chia, fairness_soft_eps);
		return mechanisms;
	}
	
	public static List<GroupProjectAlgorithm> groupingMechanisms_except()
	{
//		var bepsys = new GroupProjectAlgorithm.BepSys_reworked(PregroupingType.exceptSubmaxCliqueSoftGrouped());
		var chia = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.exceptSubmaxCliqueSoftEpsGrouped());
		var fairness_soft_eps = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.exceptSubmaxCliqueSoftEpsGrouped());
		
		var mechanisms = List.<GroupProjectAlgorithm>of(/*bepsys,*/ chia, fairness_soft_eps);
		return mechanisms;
	}
	
	public List<DatasetContext> TUDInstances()
	{
		return Stream.of(3, 4, 10, 11, 14, 17, 18, 23, 39, 42, 45)
		             .map(CourseEditionFromDb::fromLocalBepSysDbSnapshot)
			         .map(DatasetContext_AugmentedPreferences_AppendedTied::from)
		             .toList();
	}
	
	public List<DatasetContext> SDUInstances()
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
	
	static final int runsPerDataset = 3;
	
	@Override
	public void run()
	{
		// ez warmup experiment first
		new WarmupExperiment(groupingMechanisms_max()).run();
		
		new HistoricalDatasetExperiment("Historical_TUD_2_any", TUDInstances(), groupingMechanisms_any(), runsPerDataset).run();
		new HistoricalDatasetExperiment("Historical_TUD_2_max", TUDInstances(), groupingMechanisms_max(), runsPerDataset).run();
		new HistoricalDatasetExperiment("Historical_TUD_2_except", TUDInstances(), groupingMechanisms_except(), runsPerDataset).run();
		
		// can't support these due to per-project group-size-constraints that SDU instances have
//		new HistoricalDatasetExperiment("Historical_SDU_max", SDUInstances(), groupingMechanisms_max(), runsPerDataset).run();
//		new HistoricalDatasetExperiment("Historical_SDU_except", SDUInstances(), groupingMechanisms_except(), runsPerDataset).run();
		new HistoricalDatasetExperiment("Historical_SDU_any", SDUInstances(), groupingMechanisms_any(), runsPerDataset).run();
	}
	
	public static void main(String[] args)
	{
		new HistoricalExpsRunner().run();
	}
}
