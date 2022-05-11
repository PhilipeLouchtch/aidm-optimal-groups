package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.nogroup;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.experiment.paper.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.WarmupExperiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group.mechanism.Chiarandini_Fairgroups_EpsilonConstraintVersion;

import java.util.List;

public class NoPregroupExperimentsRunner implements Experiment
{
	static final int runsPerDataset = 3;
	static final int numDatasetsToGen = 5;
	
	private static List<GroupProjectAlgorithm> mechanisms()
	{
		var sdpc = new GroupProjectAlgorithm.SDPCWithSlots();
		var bepsys = new GroupProjectAlgorithm.BepSys_reworked();
		var chiarandini_none = new GroupProjectAlgorithm.Chiarandini_MiniMax_OWA(PregroupingType.none());
		var fairness_soft_eps = new Chiarandini_Fairgroups_EpsilonConstraintVersion(new OWAObjective(), PregroupingType.anyCliqueSoftGroupedEpsilon());
		
		return List.of(sdpc, bepsys, chiarandini_none, fairness_soft_eps);
	}
	
	
	@Override
	public void run()
	{
		var postfix = "final1";
		var mechanisms = mechanisms();
		
		new WarmupExperiment(mechanisms)
				.run();
		
		new SizeExperiment("size_exp_" + postfix, mechanisms, numDatasetsToGen, runsPerDataset)
				.run();
		
		new SlotsScalingExperiment("slots_exp_" + postfix, mechanisms, numDatasetsToGen, runsPerDataset)
				.run();
		
		new GroupSizeBoundsExperiment("gsc_exp_" + postfix, mechanisms, numDatasetsToGen, runsPerDataset)
				.run();
	}
	
	
	public static void main(String[] args)
	{
		new NoPregroupExperimentsRunner().run();
	}
}
