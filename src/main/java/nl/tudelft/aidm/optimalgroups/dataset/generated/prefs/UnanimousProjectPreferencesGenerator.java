package nl.tudelft.aidm.optimalgroups.dataset.generated.prefs;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;

import java.util.function.Supplier;

/**
 * Identical preference shared by everybody!
 */
public class UnanimousProjectPreferencesGenerator implements ProjectPreferenceGenerator
{
	private final ProjectPreference theOne;

	/**
	 * The given preference is the unanimous preference
	 * @param theOne The unanimous preference
	 */
	public UnanimousProjectPreferencesGenerator(ProjectPreference theOne)
	{
		this.theOne = theOne;
	}
	
	@Override
	public ProjectPreference generateNew()
	{
		return theOne;
	}
}
