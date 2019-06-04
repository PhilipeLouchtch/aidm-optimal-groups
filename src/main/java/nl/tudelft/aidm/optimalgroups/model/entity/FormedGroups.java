package nl.tudelft.aidm.optimalgroups.model.entity;

import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class FormedGroups implements Groups
{
	private List<Group> groups = new ArrayList<>();

	public Collection<Group> asCollection()
	{
		return this.groups;
	}

	// make new
	public Group addAsFormed(Group tentativeGroup)
	{
		var group = new Group.FormedGroup(tentativeGroup.members(), tentativeGroup.projectPreference(), groups.size());
		groups.add(group);

		return group;
	}

	public int count()
	{
		return groups.size();
	}

//	public Group getByIndex(int idx)
//	{
//		return groups.get(idx);
//	}

	public void forEach(Consumer<Group> fn)
	{
		this.groups.forEach(fn);
	}
}