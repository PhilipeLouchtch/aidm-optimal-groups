package nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs;

import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  This type of generator generates ProjectPreferences
 * 	where in the projects rank is related to the probability of that project.
 * 	The probability is determined by the given distribution. More precisely, the projects correspond to [0,mean]
 * 	line of the PDF of the distribution. Mean = #number of projects.
 */
public class ProjectPreferencesFromDistributionGenerator extends ProjectPreferencesFromPmfGenerator
{
	/**
	 * Base constructor, given a RealDistribution the PMF of projects is constructed.
	 * @param projects The projects for which the preferences are generated
	 * @param distribution The distribution according to which the projects are drawn from
	 */
	public ProjectPreferencesFromDistributionGenerator(Projects projects, RealDistribution distribution)
	{
		super(projects, toPmf(projects, distribution));
	}

	private static List<Pair<Project, Double>> toPmf(Projects projects, RealDistribution distribution)
	{
		// Assign each project a prob from the distrib
		//     - map projects from 0 to mean along the x-axis, but in an anonymous way
		// Make EnumeratedDistribution with the <Project,probability[0,1]> pairs

		var data = new ArrayList<>(projects.asCollection());
		Collections.shuffle(data);

		var pmf = new ArrayList<Pair<Project, Double>>(data.size());
		for (int i = 0; i < data.size(); i++)
		{
			var project = data.get(i);
			pmf.add(new Pair<>(project, distribution.density(i)));
		}

		return pmf;
	}
}
