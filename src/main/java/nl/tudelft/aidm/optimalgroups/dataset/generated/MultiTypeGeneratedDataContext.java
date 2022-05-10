package nl.tudelft.aidm.optimalgroups.dataset.generated;

import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.MultiTypeProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.NormallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

public class MultiTypeGeneratedDataContext extends GeneratedDataContext
{
	public MultiTypeGeneratedDataContext(int numAgents, Projects projects, GroupSizeConstraint groupSizeConstraint, ProjectPreferenceGenerator projPrefGenerator, PregroupingGenerator pregroupingGenerator)
	{
		super(numAgents, projects, groupSizeConstraint, projPrefGenerator, pregroupingGenerator);
	}
	
//	public static MultiTypeGeneratoredDataContext makeNewWith(int numProjects, int numAgents, int numSlotsPerProject, GroupSizeConstraint groupSizeConstraint, MultiTypeProjectPreferencesGenerator.Type... types)
//	{
//		var projects = Projects.generated(numProjects, numSlotsPerProject);
//		var generator = new MultiTypeProjectPreferencesGenerator(types);
//		return new MultiTypeGeneratoredDataContext(numAgents, projects, groupSizeConstraint, generator);
//	}
	
	public static MultiTypeGeneratedDataContext makeNewWith40302010Types(int numProjects, int numAgents, int numSlotsPerProject, GroupSizeConstraint groupSizeConstraint)
	{
		var projects = Projects.generated(numProjects, numSlotsPerProject);
		var generator = new MultiTypeProjectPreferencesGenerator(
				new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(projects, 4), 0.4),
				new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(projects, 3), 0.3),
				new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(projects, 4), 0.2),
				new MultiTypeProjectPreferencesGenerator.Type(new NormallyDistributedProjectPreferencesGenerator(projects, 1), 0.1)
		);
		
		return new MultiTypeGeneratedDataContext(numAgents, projects, groupSizeConstraint, generator, PregroupingGenerator.CE10Like());
	}
}
