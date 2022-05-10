package nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs;

import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.base.AbstractListBasedProjectPreferences;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.*;

/**
 *  This type of generator generates ProjectPreferences
 * 	where in the projects rank is related to the probability of that project.
 * 	The probability is determined by the given distribution. More precisely, the projects correspond to [0,mean]
 * 	line of the PDF of the distribution. Mean = #number of projects.
 */
public class ProjectPreferencesFromPmfGenerator implements ProjectPreferenceGenerator
{
	protected final Projects projects;
	protected final List<Pair<Project, Double>> pmf;

	/**
	 * Base constructor, given a RealDistribution the PMF of projects is constructed.
	 * @param projects The projects for which the preferences are generated
	 * @param pmf The PMF according to which the projects are drawn from - will be normalized to sum 100%
	 */
	public ProjectPreferencesFromPmfGenerator(Projects projects, List<Pair<Project, Double>> pmf)
	{
		this.projects = projects;
		this.pmf = pmf;
	}

	@Override
	public ProjectPreference generateNew()
	{
		// Would be nicer to have NormallyDistributedProjectPreference type...
		/// TODO: move PMF into own class
		return new PmfGeneratedProjectPreference(pmf);
	}

	class PmfGeneratedProjectPreference extends AbstractListBasedProjectPreferences
	{
		// The PMF used to generate this preference profile
		private final List<Pair<Project, Double>> completePmf;

		private final List<Project> prefProfile;
		private Integer[] asArray = null;

		public PmfGeneratedProjectPreference(List<Pair<Project, Double>> completePmf)
		{
			this.completePmf = completePmf;
			// We're building this list by drawing projects with probabilities defined in the PMF
			var pref = new ArrayList<Project>(pmf.size());

			// Copy the complete PMF
			var pmf = new LinkedList<>(completePmf);

			// While we have projects to add to our list
			while (!pmf.isEmpty()) {
				EnumeratedDistribution<Project> projectPrefDistrib = new EnumeratedDistribution<>(pmf);
				var sample = projectPrefDistrib.sample();
				pref.add(sample);

				// Remove the project from PMF, the pmf will be updated with the remaining elements
				pmf.removeIf(pair -> pair.getKey().equals(sample));
			}

			prefProfile = pref;
		}
		
		@Override
		public List<Project> asList()
		{
			return Collections.unmodifiableList(prefProfile);
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof ProjectPreference)) return false;
			if (!(o instanceof PmfGeneratedProjectPreference)) throw new RuntimeException("Hmm PrefProfPmf is being compared with some other type. Check if use-case is alright.");
			PmfGeneratedProjectPreference that = (PmfGeneratedProjectPreference) o;
			return prefProfile.equals(that.prefProfile);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(prefProfile);
		}
	}
}
