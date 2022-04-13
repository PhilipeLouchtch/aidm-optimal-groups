package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;

import java.util.List;

public class ThesisExperimentsRunner
{
	public static List<GroupProjectAlgorithm> mechanisms()
	{
		var bepsys = new GroupProjectAlgorithm.BepSys_reworked();
		var normal_none = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.none());
		var fairness_none = new GroupProjectAlgorithm.Chiarandini_Fairgroups(new OWAObjective(), PregroupingType.none());
		
		var mechanisms = List.of(bepsys, normal_none, fairness_none);
		
		return mechanisms;
	}
	
	static final int runsPerDataset = 3;
	static final int numDatasetsToGen = 5;
	
	public static void main(String[] args)
	{
		// ez warmup experiment first
		new WarmupExperiment(mechanisms()).run();
		
		var exp = new SizeExperiment("size_exp_att1", mechanisms(), numDatasetsToGen, runsPerDataset);
		exp.run();
		
		var exp2 = new SlotsScalingExperiment("slots_exp_tst", mechanisms(), numDatasetsToGen, runsPerDataset);
		exp2.run();
	}
}
