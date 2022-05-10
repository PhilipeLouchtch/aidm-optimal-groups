package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

import java.util.function.Function;

public record NamedPrefGenerator(String shortName, Function<Projects, ProjectPreferenceGenerator> generatorPrototype)
{
	public ProjectPreferenceGenerator makeGeneratorFor(Projects projects)
	{
		return generatorPrototype.apply(projects);
	}
}
