package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group.mechanism;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.MILP_Mechanism_FairPregroupingEpsilon;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

public record Chiarandini_Fairgroups_EpsilonConstraintVersion(ObjectiveFunction objectiveFunction, PregroupingType pregroupingType) implements GroupProjectAlgorithm
{
	@Override
	public String name()
	{
		return "Fair (impr-eps) - " + objectiveFunction.name() + " - " + pregroupingType.simpleName();
	}
	
	@Override
	public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
	{
		var algo = new MILP_Mechanism_FairPregroupingEpsilon(datasetContext, objectiveFunction, pregroupingType);
		var matching = algo.doIt();
		
		return matching;
	}
}
