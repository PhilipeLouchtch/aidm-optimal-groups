package nl.tudelft.aidm.optimalgroups.model.pref.base;

import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.RankInPref;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.lang.reflect.Array;
import java.util.*;

/**
 * A project preference implementation that supports (in)complete, total, partial etc
 * preferences by maintaining a mapping between projects and their ranking ({@link MapBasedProjectPreferences#asMap})
 */
public class MapBasedProjectPreferences implements ProjectPreference
{
	private final Map<Project, RankInPref> asMap;
	private final Object owner;
	
	private Project[] asArray;
	private List<Project> asList;
	
	public MapBasedProjectPreferences(Object owner, Map<Project, RankInPref> asMap)
	{
		this.owner = owner;
		this.asMap = asMap;
	}
	
	@Override
	public Project[] asArray()
	{
		if (this.asArray == null)
		{
			this.asArray = asMap.entrySet().stream()
					.filter(entry -> entry.getValue().isPresent()) // missing projects = unacceptible, thus array and list contain only acceptible
					.sorted(Map.Entry.comparingByValue())
					.map(Map.Entry::getKey)
					.toArray(Project[]::new);
		}
		
		return Arrays.copyOf(this.asArray, this.asArray.length);
	}
	
	@Override
	public List<Project> asList()
	{
		if (this.asList == null)
		{
			this.asList = Arrays.asList(asArray());
		}
		
		return Collections.unmodifiableList(this.asList);
	}
	
	@Override
	public Map<Project, RankInPref> asMap()
	{
		return Collections.unmodifiableMap(asMap);
	}
	
	@Override
	public RankInPref rankOf(Project project)
	{
		return asMap().get(project);
	}
}
