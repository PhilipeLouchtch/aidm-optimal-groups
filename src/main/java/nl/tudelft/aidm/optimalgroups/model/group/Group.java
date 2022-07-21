package nl.tudelft.aidm.optimalgroups.model.group;

import nl.tudelft.aidm.optimalgroups.model.HasProjectPrefs;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;

public interface Group extends HasProjectPrefs
{
	Agents members();

	@Override
	ProjectPreference projectPreference();
	
	default boolean contains(Group other)
	{
		return members().containsAll(other.members());
	}

	abstract class AbstractGroup implements Group
	{
		protected Agents members;
		protected ProjectPreference projectPreference;

		AbstractGroup(Agents members, ProjectPreference projectPreference)
		{
			this.members = members;
			this.projectPreference = projectPreference;
		}

		public Agents members()
		{
			return members;
		}

		public ProjectPreference projectPreference()
		{
			return projectPreference;
		}
	}

	class FormedGroup extends AbstractGroup
	{
		private final int id;

		public FormedGroup(Agents members, ProjectPreference projectPreference, int id)
		{
			super(members, projectPreference);
			this.id = id;
		}

		public int groupId()
		{
			return id;
		}
	}

	class TentativeGroup extends AbstractGroup
	{
		public TentativeGroup(Agents members, ProjectPreference projectPreference)
		{
			super(members, projectPreference);
		}

		public TentativeGroup(Group group)
		{
			super(group.members(), group.projectPreference());
		}

		public TentativeGroup combined(TentativeGroup other)
		{
			Agents agents = members.with(other.members);
			AggregatedProjectPreference pref = AggregatedProjectPreference.usingGloballyConfiguredMethod(agents);

			return new TentativeGroup(agents, pref);
		}
	}
}
