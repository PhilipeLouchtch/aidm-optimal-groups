package nl.tudelft.aidm.optimalgroups;

import nl.tudelft.aidm.optimalgroups.algorithm.group.*;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.BepSysImprovedGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.project.*;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.pregroupprefs.PregroupingGenerator;
import nl.tudelft.aidm.optimalgroups.metric.*;
import nl.tudelft.aidm.optimalgroups.metric.dataset.AvgPreferenceRankOfProjects;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.GroupPreferenceSatisfactionHistogram;
import nl.tudelft.aidm.optimalgroups.metric.matching.gini.GiniCoefficientGroupRank;
import nl.tudelft.aidm.optimalgroups.metric.matching.gini.GiniCoefficientStudentRank;
import nl.tudelft.aidm.optimalgroups.metric.profile.GroupRankProfile;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCR;
import nl.tudelft.aidm.optimalgroups.metric.profile.StudentRankProfile;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCRGroup;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCRStudent;
import nl.tudelft.aidm.optimalgroups.metric.rank.histrogram.AssignedProjectRankGroupHistogram;
import nl.tudelft.aidm.optimalgroups.metric.rank.histrogram.AssignedProjectRankStudentHistogram;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class Application
{
	public static final int courseEditionId = 10;
	public static final int iterations = 1;
	public static final String groupMatchingAlgorithm = "CombinedPreferencesGreedy";
	public static String preferenceAggregatingMethod = "Borda";
	public static final String projectAssignmentAlgorithm = "MaxFlow";

	public static void main(String[] args)
	{
		// "Fetch" agents and from DB before loop; they don't change for another iteration
//		DatasetContext datasetContext = CourseEdition.fromLocalBepSysDbSnapshot(courseEditionId);
		DatasetContext datasetContext = GeneratedDataContext.withNormallyDistributedProjectPreferences(150, 40, GroupSizeConstraint.manual(4,5), 4, PregroupingGenerator.none());
		printDatasetInfo(datasetContext);

		var avgPreferenceRankOfProjects = AvgPreferenceRankOfProjects.fromAgents(datasetContext.allAgents(), datasetContext.allProjects());
		avgPreferenceRankOfProjects.displayChart();

		double[] studentAUPCRs = new double[iterations];
		double[] groupAUPCRs = new double[iterations];

		GroupPreferenceSatisfactionHistogram[] groupPreferenceSatisfactionDistributions = new GroupPreferenceSatisfactionHistogram[iterations];
		AssignedProjectRankGroupHistogram[] groupProjectRankDistributions = new AssignedProjectRankGroupHistogram[iterations];
		AssignedProjectRankStudentHistogram[] studentProjectRankDistributions = new AssignedProjectRankStudentHistogram[iterations];

		// Perform the group making, project assignment and metric calculation inside the loop
		for (int iteration = 0; iteration < iterations; iteration++) {

			printIterationNumber(iteration);

			GroupFormingAlgorithm formedGroups = formGroups(datasetContext);
			GroupToProjectMatching<Group.FormedGroup> groupProjectMatching =  assignGroupsToProjects(datasetContext, formedGroups);

			Matching<Group.FormedGroup, Project> matching = groupProjectMatching;
			var matchingFromStudentPerspective = AgentToProjectMatching.from(matching);

//			Matching<Group.FormedGroup, Project> matching = new ILPPPDeterminedMatching(datasetContext);

			var studentProfileCurve = new StudentRankProfile(matchingFromStudentPerspective);
			studentProfileCurve.displayChart("");


			var groupAggRankProfile = new GroupRankProfile(matching);
			groupAggRankProfile.displayChart();

//			ProfileCurveOfMatching groupProfileCurve = new ProjectProfileCurveGroup(matching);
//			groupProfile.printResult();

			GiniCoefficientStudentRank giniStudentRank = new GiniCoefficientStudentRank(matchingFromStudentPerspective);
			giniStudentRank.printResult(System.out);

			GiniCoefficientGroupRank giniGroupRank = new GiniCoefficientGroupRank(matching);
			giniGroupRank.printResult(System.out);

			AUPCR studentAUPCR = new AUPCRStudent(matchingFromStudentPerspective, datasetContext.allProjects(), datasetContext.allAgents());
			//studentAUPCR.printResult();

			AUPCR groupAUPCR = new AUPCRGroup(matching, datasetContext.allProjects(), datasetContext.allAgents());
			//groupAUPCR.printResult();

			GroupPreferenceSatisfactionHistogram groupPreferenceDistribution = new GroupPreferenceSatisfactionHistogram(matching, 20);
			//groupPreferenceDistribution.printResult();

			AssignedProjectRankGroupHistogram groupProjectRankDistribution = new AssignedProjectRankGroupHistogram(matching, datasetContext.allProjects());
			//groupProjectRankDistribution.printResult();

			AssignedProjectRankStudentHistogram studentProjectRankDistribution = new AssignedProjectRankStudentHistogram(matching, datasetContext.allProjects());
			//studentProjectRankDistribution.printResult();

			// Remember metrics
			studentAUPCRs[iteration] = studentAUPCR.asDouble();
			groupAUPCRs[iteration] = groupAUPCR.asDouble();
			groupPreferenceSatisfactionDistributions[iteration] = groupPreferenceDistribution;
			groupProjectRankDistributions[iteration] = groupProjectRankDistribution;
			studentProjectRankDistributions[iteration] = studentProjectRankDistribution;
		}

		// Calculate all the averages and print them to the console
		float studentAUPCRAverage = 0;
		float groupAUPCRAverage = 0;
		for (int iteration = 0; iteration < iterations; iteration++) {
			studentAUPCRAverage += studentAUPCRs[iteration] / studentAUPCRs.length;
			groupAUPCRAverage += groupAUPCRs[iteration] / groupAUPCRs.length;
		}

		Histogram.AverageHistogram groupPreferenceSatisfactionDistribution = new Histogram.AverageHistogram(groupPreferenceSatisfactionDistributions);
		groupPreferenceSatisfactionDistribution.printToTxtFile("outputtxt/groupPreferenceSatisfaction.txt");
		//groupPreferenceSatisfactionDistribution.printResult();

		printAveragePeerSatisfaction(groupPreferenceSatisfactionDistribution);

		Histogram.AverageHistogram groupProjectRankDistribution = new Histogram.AverageHistogram(groupProjectRankDistributions);
		groupProjectRankDistribution.printToTxtFile("outputtxt/groupProjectRank.txt");
		//groupProjectRankDistribution.printResult();

		Histogram.AverageHistogram studentProjectRankDistribution = new Histogram.AverageHistogram(studentProjectRankDistributions);
		studentProjectRankDistribution.printToTxtFile("outputtxt/studentProjectRank.txt");
		//studentProjectRankDistribution.printResult();

		printStudentAupcrAverage(studentAUPCRAverage);
		writeToFile("outputtxt/studentAUPCR.txt", String.valueOf(studentAUPCRAverage));

		printGroupAupcrAverage(groupAUPCRAverage);
		writeToFile("outputtxt/groupAUPCR.txt", String.valueOf(groupAUPCRAverage));
	}

	private static void printDatasetInfo(DatasetContext courseEdition)
	{
		System.out.println("Amount of projects: " + courseEdition.allProjects().count());
		System.out.println("Amount of students: " + courseEdition.allAgents().count());
	}

	private static GroupToProjectMatching<Group.FormedGroup> assignGroupsToProjects(DatasetContext datasetContext, GroupFormingAlgorithm formedGroups)
	{
		if (projectAssignmentAlgorithm.equals("RSD")) {
			return new RandomizedSerialDictatorship(datasetContext, formedGroups.asFormedGroups(), datasetContext.allProjects());
		} else {
			return new GroupProjectMaxFlow(datasetContext, formedGroups.asFormedGroups(), datasetContext.allProjects());
		}
	}

	private static GroupFormingAlgorithm formGroups(DatasetContext courseEdition)
	{
		if (groupMatchingAlgorithm.equals("CombinedPreferencesGreedy")) {
			return new CombinedPreferencesGreedy(courseEdition);
		}
		else if (groupMatchingAlgorithm.equals("BEPSysFixed")) {
			return new BepSysImprovedGroups(courseEdition.allAgents(), courseEdition.groupSizeConstraint(), true);
		}
		else {
			return new BepSysImprovedGroups(courseEdition.allAgents(), courseEdition.groupSizeConstraint(), false);
		}
	}

	public static void writeToFile(String fileName, String content) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false));
			writer.write(content);
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void printIterationNumber(int iteration)
	{
		System.out.printf("Iteration %d\n", iteration+1);
	}

	private static void printAveragePeerSatisfaction(Histogram.AverageHistogram groupPreferenceSatisfactionDistribution)
	{
		System.out.println("Average group preference satisfaction: " + groupPreferenceSatisfactionDistribution.average());
	}

	private static void printGroupAupcrAverage(float groupAUPCRAverage)
	{
		System.out.printf("AUPCR - Group aggregate pref (average over %d iterations: %f)\n", iterations, groupAUPCRAverage);
	}

	private static void printStudentAupcrAverage(float studentAUPCRAverage)
	{
		System.out.printf("AUPCR - Individual student   (average over %d iterations: %f)\n", iterations, studentAUPCRAverage);
	}
}
