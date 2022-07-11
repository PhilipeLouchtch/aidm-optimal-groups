package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.experiment.paper.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.WarmupExperiment;

import java.util.List;

public class PregroupingExperimentsRunner implements Experiment
{
	public static List<GroupProjectAlgorithm> groupingMechanisms()
	{
		var bepsys = new GroupProjectAlgorithm.BepSys_reworked(PregroupingType.anyCliqueSoftGrouped());
		var normal_hard = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.anyCliqueHardGrouped());
		var fairness_soft_eps = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.anyCliqueSoftGroupedEpsilon());
//		var fairness_none = new Chiarandini_FairgroupsNEW(new OWAObjective(), PregroupingType.anyCliqueSoftGrouped());
		
		var mechanisms = List.<GroupProjectAlgorithm>of(bepsys, normal_hard, fairness_soft_eps);
		return mechanisms;
	}
	
	static final int runsPerDataset = 3;
	static final int numDatasetsToGen = 5;
	
	@Override
	public void run()
	{
		// ez warmup experiment first
		new WarmupExperiment(groupingMechanisms()).run();
		
		new PregroupingSoftGrpingExperiment("grouping_soft_att2_eps", groupingMechanisms(), numDatasetsToGen, runsPerDataset)
				.run();
	}
	
	public static void main(String[] args)
	{
		new PregroupingExperimentsRunner().run();
	}
}
