package nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs;

import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.distribution.ExponentialDistribution;

public class ExponentiallyDistributedProjectPreferencesGenerator extends ProjectPreferencesFromDistributionGenerator
{
	/**
	 * Creates a new Linearly Distributed ProjectPreferences Generator. It generates ProjectPreferences
	 * where in the projects rank is related to the probability of that project.
	 * @param projects The projects for which the preferences are generated
	 * @param chaos How neatly linear the resulting preference avg-rank profile will look. With 1 being a very clean f(x) = y linear
	 *              line, and greater values resulting in larger std deviation. 4 looks real-ish for smaller datasets but for larger #p
	 *              more like a line with some std dev
	 */
	public ExponentiallyDistributedProjectPreferencesGenerator(Projects projects, double chaos)
	{
		super(projects, new ExponentialDistribution(chaos));
	}
}
