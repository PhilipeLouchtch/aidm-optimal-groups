package nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import plouchtch.assertion.Assert;

import java.util.Arrays;

import static nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator.ChanceBased.*;

public interface PregroupingGenerator
{
	/**
	 * Draws a pregrouping size
	 * @return A pregrouping size
	 */
	Integer draw();
	
	record ChanceBased(Item... items) implements PregroupingGenerator
	{
		@Override
		public Integer draw()
		{
			var values = Arrays.stream(items).mapToInt(Item::groupSize).toArray();
			var probabilies = Arrays.stream(items).mapToDouble(Item::chance).toArray();
			var distribution = new EnumeratedIntegerDistribution(values, probabilies);
			
			return distribution.sample();
		}
		
		public record Item(Integer groupSize, Double chance) {
			public Item {
				Assert.that(0 <= chance && chance <= 1).orThrowMessage("Invalid value for chance, must be within [0, 1]");
			}
		}
		
		public ChanceBased
		{
			var chanceSum = Arrays.stream(items).mapToDouble(Item::chance).sum();
			Assert.that(chanceSum == 1.0).orThrowMessage("Chances must sum up to 1.0");
		}
	}
	
	record ProportionBased(ProportionBased.Item... items)
	{
		public record Item(Integer groupSize, Double proportion) {
			public Item {
				Assert.that(0 <= proportion && proportion <= 1).orThrowMessage("Invalid value for proportion, must be within [0, 1]");
			}
		}
		
		public ProportionBased {
			var chanceSum = Arrays.stream(items).mapToDouble(Item::proportion).sum();
			Assert.that(chanceSum == 1.0).orThrowMessage("Chances must sum up to 1.0");
		}
	}
	
	/**
	 * A generator that either creates a non-pregrouping student, or a clique or pregrouping
	 * students.
	 *
	 * @param groupSize The size of clique
	 * @param chance Chance of creating a clique of given size. Value between [0, 1]
	 * @return
	 */
	static PregroupingGenerator singlePregroupingSizeOnly(int groupSize, double chance)
	{
		return new ChanceBased(
				new Item(1, 1 - chance),
				new Item(groupSize, chance)
		);
	}
	
	static PregroupingGenerator CE10Like()
	{
		// sortof based on CE10
		return new ChanceBased(
				new Item(1, 0.3),
				new Item(2, 0.03),
				new Item(3, 0.15),
				new Item(4, 0.27),
				new Item(5, 0.25)
		);
	}
	
	static PregroupingGenerator none()
	{
		return () -> 1;
	}
}
