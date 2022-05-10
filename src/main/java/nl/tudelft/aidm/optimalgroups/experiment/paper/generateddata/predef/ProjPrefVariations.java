package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef;

import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ExponentiallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.MultiTypeProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.NormallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.UniformProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.NamedPrefGenerator;
import nl.tudelft.aidm.optimalgroups.model.pref.base.ListBasedProjectPreferences;

import java.util.ArrayList;

public interface ProjPrefVariations
{
	
	// "extremely linear"
	static NamedPrefGenerator singleton()
	{
		return new NamedPrefGenerator("singleton", p -> () -> new ListBasedProjectPreferences(new ArrayList<>(p.asCollection())));
	}
	
	static NamedPrefGenerator linearPerturbedSlightly()
	{
		return new NamedPrefGenerator("linear_perturbed_1", p -> new ExponentiallyDistributedProjectPreferencesGenerator(p, 1));
	}
	
	static NamedPrefGenerator linearPerturbedMore()
	{
		return new NamedPrefGenerator("linear_perturbed_4", p -> new ExponentiallyDistributedProjectPreferencesGenerator(p, 4));
	}
	
	static NamedPrefGenerator random()
	{
		return new NamedPrefGenerator("random", UniformProjectPreferencesGenerator::new);
	}
	
	static NamedPrefGenerator realistic()
	{
		return new NamedPrefGenerator("realistic",
				p -> new MultiTypeProjectPreferencesGenerator(
						new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(p, 4), 0.45),
						new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(p, 3), 0.35),
						new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(p, 4), 0.2)
				)
		);
	}
}
