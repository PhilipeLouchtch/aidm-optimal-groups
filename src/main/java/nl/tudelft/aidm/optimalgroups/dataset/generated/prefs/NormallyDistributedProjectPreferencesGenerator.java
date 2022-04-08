package nl.tudelft.aidm.optimalgroups.dataset.generated.prefs;

import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.distribution.NormalDistribution;

public class NormallyDistributedProjectPreferencesGenerator extends ProjectPreferencesFromDistributionGenerator
{
	/**
	 * Creates a new Normally Distributed ProjectPreferences Generator.
	 * @param projects The projects for which the preferences are generated
	 * @param curveSteepness Used to calculate the stdDev, larger values result in steeper and narrower spikes.
	 *                       In other words, this value sets how strongly the top projects are desired relative to
	 *                       the others. Very hight values result in a few projects being extremely desired with
	 *                       the rest relatively equally desired. Lower values result in distributions where
	 *                       Most projects are realtively desired, some somewhat more than others.
	 */
	public NormallyDistributedProjectPreferencesGenerator(Projects projects, double curveSteepness)
	{
		super(projects, new NormalDistribution(projects.count(), projects.count() / curveSteepness));
	}
}
