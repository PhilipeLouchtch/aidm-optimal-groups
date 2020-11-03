package nl.tudelft.aidm.optimalgroups.model.pref;

import nl.tudelft.aidm.optimalgroups.metric.rank.RankInArray;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public interface ProjectPreference
{
	// TODO: determine representation (let algo guide this choice)
	Integer[] asArray();

	List<Project> asListOfProjects();

	default void forEach(ProjectPreferenceObjectRankConsumer iter)
	{
		List<Project> projectList = asListOfProjects();
		for (Project proj : projectList) {
			var rank = rankOf(proj);
			iter.apply(proj, rank);
		}
	}

	/**
	 * Checks if the project preferences indicate complete indifference, that is an absence of preference.
	 * In case of BepSys: the agent has no preferences available. In other scenarios this might mean that the
	 * the available choices have equal rank to the agent
	 * @return
	 */
	default boolean isCompletelyIndifferent()
	{
		return asArray().length == 0;
	}

	default OptionalInt rankOf(Project project)
	{
		if (isCompletelyIndifferent()) {
			return OptionalInt.empty();
		}

		return new RankInArray().determineRank(project.id(), this.asArray());
	}

	/**
	 * Return the preferences as a map, where the keys represent the project
	 * and the value represents the rank of the project.
	 *
	 * The highest rank is 1 and represents the most preferable project.
	 *
	 * @return Map
	 */
	default Map<Integer, Integer> asMap()
	{
		Integer[] preferences = this.asArray();
		var preferencesMap = new HashMap<Integer, Integer>(preferences.length);

		for (int rank = 1; rank <= preferences.length; rank++) {
			int project = preferences[rank - 1];
			preferencesMap.put(project, rank);
		}

		return preferencesMap;
	}
	default int differenceTo(ProjectPreference otherPreference)
	{
		Map<Integer, Integer> own = asMap();
		Map<Integer, Integer> other = otherPreference.asMap();

		// If the other does not have any preferences, return maximum difference to
		// avoid picking this matchings over people that do have preferences
		if (other.size() == 0 || own.size() == 0) {
			return Integer.MAX_VALUE;
		}

		int difference = 0;
		for (Map.Entry<Integer, Integer> entry : own.entrySet()) {
			difference += Math.abs(entry.getValue() - other.get(entry.getKey()));
		}

		return difference;
	}

	interface ProjectPreferenceObjectRankConsumer
	{
		/**
		 * @param project The project with the given rank
		 * @param rank Rank in preference, 1 being highest - empty if indifferent
		 */
		void apply(Project project, OptionalInt rank);
	}

}
