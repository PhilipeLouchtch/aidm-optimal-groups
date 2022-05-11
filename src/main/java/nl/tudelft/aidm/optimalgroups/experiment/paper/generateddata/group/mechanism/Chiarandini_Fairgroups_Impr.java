package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.group.mechanism;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.MILP_Mechanism_FairPregroupingImpr;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;

public class Chiarandini_Fairgroups_Impr implements GroupProjectAlgorithm
{
	private final ObjectiveFunction objectiveFunction;
	private final PregroupingType pregroupingType;
	
	public Chiarandini_Fairgroups_Impr(ObjectiveFunction objectiveFunction, PregroupingType pregroupingType)
	{
		this.objectiveFunction = objectiveFunction;
		this.pregroupingType = pregroupingType;
	}
	
	@Override
	public String name()
	{
		return "Fair (impr) - " + objectiveFunction.name() + " - " + pregroupingType.simpleName();
	}
	
	@Override
	public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
	{
		var algo = new MILP_Mechanism_FairPregroupingImpr(datasetContext, objectiveFunction, pregroupingType);
		var matching = algo.doIt();
		
		return matching;
	}
}
