package nl.tudelft.aidm.optimalgroups.model.pref.base;

import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedCommonly;
import nl.tudelft.aidm.optimalgroups.dataset.transforms.DatasetContext_AugmentedPreferences_AppendedRandomly;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.RankInPref;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.UnacceptableAlternativeRank;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A list-backed implementation of ProjectPreferences where an agent has ordinal preferences
 * over the projects. The position of the project in the list corresponds with the rank (index 0 -> rank 1).
 * The preference can be incomplete, any missing projects are assumed to be "unacceptible" (the default
 * way of handling missing projects in preferences, as defined by the default method in {@link ProjectPreference}.
 *
 * <p>
 * <b>Example:</b> given the list L = [C, B, A] as an example preference of a problem instance with the set of
 * projects P = {A, B, ..., F}, then according to the preference list L, the projects are ranked
 * as follows:
 * <ul>
 *      <li>C as first,</li>
 *      <li>B as second,</li>
 *      <li>A as third,</lu>
 *      <li>D, E and F are "unacceptible" ({@link UnacceptableAlternativeRank}).</li>
 * </ul>
 *
 * <p>
 * Alternative behaviour can be achieved through using the following implementation:
 * {@link nl.tudelft.aidm.optimalgroups.model.pref.complete.ProjectPreferenceAugmentedWithMissingTiedLast} or alternatively,
 * if a whole datasetcontext needs to be changed such that missing projects in prefs are set to ties at the end:
 * the following are dataset-transformations: {@link DatasetContext_AugmentedPreferences_AppendedRandomly} and
 * {@link DatasetContext_AugmentedPreferences_AppendedCommonly}.
 * </p>
 */
public abstract class AbstractListBasedProjectPreferences implements ProjectPreference
{
	private Project[] asArray = null;
	private Map<Project, RankInPref> asMap;

	private final IdentityHashMap<Project, RankInPref> rankOfProject = new IdentityHashMap<>();

	@Override
	public Project[] asArray()
	{
		if (asArray == null) {
			asArray = asList().toArray(Project[]::new);
		}

		return asArray;
	}

	@Override
	public RankInPref rankOf(Project project)
	{
		// Cache results - optimization for pessimism algorithm
		return rankOfProject.computeIfAbsent(project,
				ProjectPreference.super::rankOf);
	}

	@Override
	public boolean isCompletelyIndifferent()
	{
		if (asList().isEmpty()) {
			return true;
		}

		return false;
	}

	@Override
	public Map<Project, RankInPref> asMap()
	{
		if (asMap == null) {
			// use default method and cache result
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

		// For now only allow comparison between this base-type (to be safe)
		// if/when exception is triggered, re-evaluate use-case
		if (!(o instanceof AbstractListBasedProjectPreferences)) throw new RuntimeException("Hmm AbsLBProjPref is being compared with some other type. Check if use-case is alright.");

		var that = (AbstractListBasedProjectPreferences) o;
		return this.asList().equals(that.asList());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.asList());
	}
}
