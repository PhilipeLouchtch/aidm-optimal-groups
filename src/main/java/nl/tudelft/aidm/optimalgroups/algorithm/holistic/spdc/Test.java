package nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.agents.SoloAgentGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.NormallyDistributedProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.UnanimousProjectPreferencesGenerator;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.base.ListBasedProjectPreferences;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

class Test
{
	public static void main(String[] args)
	{
		var projects = Projects.generated(13, 1);
		var preference = new ListBasedProjectPreferences(projects.asCollection().stream().toList());
		var projPrefGen = new UnanimousProjectPreferencesGenerator(preference);
		
		var dataset = new GeneratedDataContext(50, projects, GroupSizeConstraint.manual(3, 4), new SoloAgentGenerator(projPrefGen));
		
		var sdpcMech = new GroupProjectAlgorithm.SDPCWithSlots();
		
		var result = sdpcMech.determineMatching(dataset);
		
		System.out.println("Done");
	}
}