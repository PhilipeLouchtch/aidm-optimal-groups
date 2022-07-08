package nl.tudelft.aidm.optimalgroups.experiment.paper.old;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.MILP_Mechanism_FairPregroupingEpsilon;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.MILP_Mechanism_FairPregroupingImpr;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.objectives.OWAObjective;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.WarmupExperiment;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group.PregroupingSoftGrpingExperiment;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

import java.util.List;

public class FairnessImprovExperimentsRunner
{
	record Chiarandini_FairgroupsNEW(ObjectiveFunction objectiveFunction, PregroupingType pregroupingType) implements GroupProjectAlgorithm
	{
		public Chiarandini_FairgroupsNEW(ObjectiveFunction objectiveFunction, PregroupingType pregroupingType)
		{
			this.objectiveFunction = objectiveFunction;
			this.pregroupingType = pregroupingType;
		}

		@Override
		public String name()
		{
			return "Chiarandini w Fair pregrouping IMPR " + objectiveFunction.name() + " - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new MILP_Mechanism_FairPregroupingImpr(datasetContext, objectiveFunction, pregroupingType);
			var matching = algo.doIt();
			
			return matching;
		}
	}
	
	record Chiarandini_FairgroupsEps(ObjectiveFunction objectiveFunction, PregroupingType pregroupingType) implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return "Chiarandini w Fair pregrouping IMPR " + objectiveFunction.name() + " - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new MILP_Mechanism_FairPregroupingEpsilon(datasetContext, objectiveFunction, pregroupingType);
			var matching = algo.doIt();
			
			return matching;
		}
	}
	
	public static List<GroupProjectAlgorithm> groupingMechanisms()
	{
		var fairness_soft = new Chiarandini_FairgroupsNEW(new OWAObjective(), PregroupingType.anyCliqueSoftGrouped());
		var fairness_soft_eps = new Chiarandini_FairgroupsEps(new OWAObjective(), PregroupingType.anyCliqueSoftGroupedEpsilon());
		
		var mechanisms = List.<GroupProjectAlgorithm>of(fairness_soft_eps, fairness_soft);
		return mechanisms;
	}
	
	
	static final int runsPerDataset = 3;
	static final int numDatasetsToGen = 3;
	
	public static void main(String[] args)
	{
		// ez warmup experiment first
		new WarmupExperiment(groupingMechanisms()).run();
		
//		new SizeExperiment("size_exp_att1", mechanisms(), numDatasetsToGen, runsPerDataset)
//				.run();
		
//		new SlotsScalingExperiment("slots_exp_att2", mechanisms(), numDatasetsToGen, runsPerDataset)
//				.run();
		
//		new GroupSizeBoundsExperiment("gsc_exp_att1", mechanisms(), numDatasetsToGen, runsPerDataset)
//				.run();
		
		new PregroupingSoftGrpingExperiment("eps_grouping_maxsize_att2", groupingMechanisms(), numDatasetsToGen, runsPerDataset)
				.run();
	}
}
