package nl.tudelft.aidm.optimalgroups.model.pref.base;

import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.Collections;
import java.util.List;

// NOTE: Prefs do not link to DatasetContext?

/**
 * A simple implementation of the abstract list-based project preferences, that can be simply instantiated with
 * a list of projects which becomes the backing list.
 */
public class ListBasedProjectPreferences extends AbstractListBasedProjectPreferences
{
	private final List<Project> preferencesAsList;

	public ListBasedProjectPreferences(List<Project> preferencesAsList)
	{
		this.preferencesAsList = Collections.unmodifiableList(preferencesAsList);
	}

	@Override
	public List<Project> asList()
	{
		return preferencesAsList;
	}
}