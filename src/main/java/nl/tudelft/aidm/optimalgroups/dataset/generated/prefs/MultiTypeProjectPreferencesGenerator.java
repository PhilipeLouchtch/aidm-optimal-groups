package nl.tudelft.aidm.optimalgroups.dataset.generated.prefs;

import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import plouchtch.assertion.Assert;

import java.util.Arrays;
import java.util.List;

public class MultiTypeProjectPreferencesGenerator implements ProjectPreferenceGenerator
{
	private final List<Type> generatorTypes;
	private final EnumeratedDistribution<ProjectPreferenceGenerator> distribution;
	
	public MultiTypeProjectPreferencesGenerator(Type... types)
	{
		var ratioSum = Arrays.stream(types).mapToDouble(value -> value.chance).sum();
		Assert.that(ratioSum == 1).orThrowMessage("Ratio's must sum to 1");
		
		this.generatorTypes = Arrays.asList(types);
		
		var pmf = generatorTypes.stream().map(type -> Pair.create(type.generator(), type.chance())).toList();
		this.distribution = new EnumeratedDistribution<>(pmf);
	}
	
	@Override
	public ProjectPreference generateNew()
	{
		var generator = distribution.sample();
		return generator.generateNew();
	}
	
	/**
	 * @param generator The generator used for the type
	 * @param chance The chance of generating a preference of this type (between 0.0 and 1 inclusive)
	 */
	public record Type(ProjectPreferenceGenerator generator, Double chance)
	{
		public Type
		{
			Assert.that(0 <= chance && chance <= 1)
					.orThrowMessage("Ratio must be between 0 and 1 (inclusive), was: %s".formatted(chance));
		}
	}
}
