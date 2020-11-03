package nl.tudelft.aidm.optimalgroups.model.pref.base;

import nl.tudelft.aidm.optimalgroups.metric.rank.RankInArray;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A base implementation of ProjectPreferences where an agent has ordinal preference
 * over the projects. The preference can be incomplete, any missing projects are assumed
 * to be tied and given the ordinal rank after those present. So [A > B > C], then rank of
 * D, E, F etc would be 4.
 */
public abstract class AbstractListBasedProjectPreferences implements ProjectPreference
{
	private Integer[] asArray = null;
	private Map<Integer, Integer> asMap;

	private final IdentityHashMap<Project, OptionalInt> rankOfProject = new IdentityHashMap<>();

	@Override
	public Integer[] asArray()
	{
		if (asArray == null) {
			asArray = asListOfProjects().stream()
				.map(Project::id)
				.toArray(Integer[]::new);
		}

		return asArray;
	}

	@Override
	public OptionalInt rankOf(Project project)
	{
		// Cache results - pessimism makes heavy use of this fn
		return rankOfProject.computeIfAbsent(project, p ->
		{
			if (isCompletelyIndifferent()) {
				return OptionalInt.empty();
			}

			var rankInArray = new RankInArray().determineRank(p.id(), asArray());

			if (rankInArray.isEmpty()){
				// Not present, so rank is assumed to be tied between all missing projects after
				// those that are present in the preferences - note: this impl excludes the possibility
				// of "do no want" preferences
				rankInArray = OptionalInt.of(asListOfProjects().size());
			}

			return rankInArray;
		});
	}

	@Override
	public boolean isCompletelyIndifferent()
	{
		if (asListOfProjects().isEmpty()) {
			return true;
		}

		return false;
	}

	@Override
	public Map<Integer, Integer> asMap()
	{
		if (asMap == null) {
			asMap = ProjectPreference.super.asMap();
		}

		return asMap;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;

		// If not proj-pref -> not equal
		if (!(o instanceof ProjectPreference)) return false;

		// For now only allow comparison between this base-types (to be safe)
		// if/when exception is triggered, re-evaluate use-case
		if (!(o instanceof AbstractListBasedProjectPreferences)) throw new RuntimeException("Hmm AbsLBProjPref is being compared with some other type. Check if use-case is alright.");

		var that = (AbstractListBasedProjectPreferences) o;
		return this.asListOfProjects().equals(that.asListOfProjects());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.asListOfProjects());
	}
}
