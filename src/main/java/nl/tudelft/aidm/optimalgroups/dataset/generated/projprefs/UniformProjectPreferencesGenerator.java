package nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs;

import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.distribution.UniformRealDistribution;

public class UniformProjectPreferencesGenerator extends ProjectPreferencesFromDistributionGenerator
{
	/**
	 * Creates a new Linearly Distributed ProjectPreferences Generator. It generates ProjectPreferences
	 * where in the projects rank is related to the probability of that project.
	 * @param projects The projects for which the preferences are generated
	 */
	public UniformProjectPreferencesGenerator(Projects projects)
	{
		super(projects, new UniformRealDistribution(0, projects.count()));
	}
}
