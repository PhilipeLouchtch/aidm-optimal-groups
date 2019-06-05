package nl.tudelft.aidm.optimalgroups;

import nl.tudelft.aidm.optimalgroups.algorithm.group.*;
import nl.tudelft.aidm.optimalgroups.algorithm.project.*;
import nl.tudelft.aidm.optimalgroups.metric.*;
import nl.tudelft.aidm.optimalgroups.model.entity.*;
import org.sql2o.GenericDatasource;

import javax.sql.DataSource;
import java.util.Map;

public class Application
{
	public static void main(String[] args)
	{
		DataSource dataSource;

		if (false)
			dataSource = new GenericDatasource("jdbc:mysql://localhost:3306/aidm", "henk", "henk");
		else
			dataSource = new GenericDatasource("jdbc:mysql://localhost:3306/bepsys?serverTimezone=UTC", "root", "");

		Agents agents = Agents.from(dataSource, 10);
		Projects projects = Projects.fromDb(dataSource, 10);
		System.out.println("Amount of projects: " + projects.count());

		BepSysWithRandomGroups formedGroups = new BepSysWithRandomGroups(agents, 4, 6);
		//MaxFlow maxflow = new MaxFlow(formedGroups.finalFormedGroups(), projects);
		RandomizedSerialDictatorship rsd = new RandomizedSerialDictatorship(formedGroups.finalFormedGroups(), projects);

		//Matching<Group.FormedGroup, Project.ProjectSlot> matching = maxflow.result();
		Matching<Group.FormedGroup, Project.ProjectSlot> matching = rsd.result();

		Profile studentProfile = new Profile.StudentProjectProfile(matching);
		studentProfile.printResult();

		Profile groupProfile = new Profile.GroupProjectProfile(matching);
		groupProfile.printResult();

		AUPCR studentAUPCR = new AUPCR.StudentAUPCR(matching, projects, agents);
		studentAUPCR.printResult();

		AUPCR groupAUPCR = new AUPCR.GroupAUPCR(matching, projects, agents);
		groupAUPCR.printResult();

		Distribution groupPreferenceDistribution = new GroupPreferenceSatisfactionDistribution(matching, 20);
		groupPreferenceDistribution.printResult();
	}
}
