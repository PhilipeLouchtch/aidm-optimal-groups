package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.setting;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.UnsatisifedPregroupingAgents;
import nl.tudelft.aidm.optimalgroups.metric.group.NumGroupsPerGroupSizeDist;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.NumberPregroupingStudentsTogether;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.NumberProposedGroupsTogether;
import nl.tudelft.aidm.optimalgroups.metric.rank.SumOfRanks;
import nl.tudelft.aidm.optimalgroups.metric.rank.WorstAssignedRank;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import org.apache.commons.codec.binary.Base64;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("DuplicatedCode")
public class AnyVsExceptVsMaxPregroupReport
{
	private final DatasetContext datasetContext;
	private final GroupProjectAlgorithm challenger;
	private final List<GroupProjectAlgorithm> opponents;
	private final String datasetId;
	
	private final StringBuffer doc;
	
	public AnyVsExceptVsMaxPregroupReport(DatasetContext datasetContext, GroupProjectAlgorithm challenger, List<GroupProjectAlgorithm> opponents, String datasetId)
	{
		this.datasetContext = datasetContext;
		this.challenger = challenger;
		this.opponents = opponents;
		this.datasetId = datasetId;
		
		this.doc = new StringBuffer();
	}
	
	public String asHtmlSource()
	{
		Properties p = new Properties();
		p.setProperty("resource.loader", "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		p.setProperty("runtime.strict_mode.enable", "true");
		
		Velocity.init(p);
		
		var context = new VelocityContext();
		
		// put all kinds of things
		context.put("title", report_title());
		context.put("numStudentsAll", datasetContext.allAgents().count());
		
		var challengerResult = challenger.determineMatching(datasetContext);
		var pregrouping = challenger.pregroupingType().instantiateFor(datasetContext);
		
		context.put("challenger", new ChallengerResultData(challengerResult, pregrouping));
		
		// add oppos
		var opposData = opponents.stream().map((GroupProjectAlgorithm opponent) ->
		{
			var oppoResult = opponent.determineMatching(datasetContext);
			var oppoData = new OppoResultData(simple_name(opponent), oppoResult, challengerResult, pregrouping);
			return oppoData;
		})
        .toList();
		
		context.put("opponentsData", opposData);
		
		try
		{
			StringWriter sw = new StringWriter();
			
			Velocity.mergeTemplate("AnyVsExceptVsMaxReportTemplate.vm", RuntimeConstants.ENCODING_DEFAULT, context, sw);
			
			return sw.toString();
		}
		catch (Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}
	
	private String report_title()
	{
		return "%s - %s".formatted(datasetId, simple_name(challenger));
	}
	
	public record ChallengerResultData(GroupToProjectMatching matching, Pregrouping pregrouping)
	{
		public NumGroupsPerGroupSizeDist pregroupDists()
		{
			return new NumGroupsPerGroupSizeDist(pregrouping.groups(), matching.datasetContext().groupSizeConstraint().maxSize());
		}
		
		public ProjectStats soloProjectStats()
		{
			var soloCohort = matching.datasetContext().allAgents().without(pregrouping.groups().asAgents());
			var matchingAgentsToProjects = AgentToProjectMatching.from(matching);
			return ProjectStats.from(matchingAgentsToProjects.filteredBy(soloCohort));
		}
		
		public ProjectStats satisfiedProjectStats()
		{
			var satisfiedPregroupingAgents = pregrouping.groups().ofWhichSatisfiedIn(matching).asAgents();
			return ProjectStats.from(AgentToProjectMatching.from(matching).filteredBy(satisfiedPregroupingAgents));
		}
		
		public TogethernessStats satisfiedPregroupStats()
		{
			return TogethernessStats.from(matching, pregrouping);
		}
		
		public ProjectStats unsatisfiedProjectStats()
		{
			var unsatisfiedPregroupingAgents = new UnsatisifedPregroupingAgents(matching, pregrouping);
			return ProjectStats.from(AgentToProjectMatching.from(matching).filteredBy(unsatisfiedPregroupingAgents));
		}
		
		public int countUnsatisfied()
		{
			return new UnsatisifedPregroupingAgents(matching, pregrouping).count();
		}
	}
	
	public record OppoResultData(String title, GroupToProjectMatching matching, GroupToProjectMatching challengerMatching, Pregrouping pregrouping)
	{
		public NumGroupsPerGroupSizeDist pregroupDists()
		{
			return new NumGroupsPerGroupSizeDist(pregrouping.groups(), matching.datasetContext().groupSizeConstraint().maxSize());
		}
		
		public ProjectStatsWithDelta soloProjectStats()
		{
			var soloCohort = matching.datasetContext().allAgents().without(pregrouping.groups().asAgents());
			var matchingAgentsToProjects = AgentToProjectMatching.from(matching);
			
			return ProjectStats.from(matchingAgentsToProjects.filteredBy(soloCohort))
			                   .withDelta(AgentToProjectMatching.from(challengerMatching).filteredBy(soloCohort));
		}
		
		public ProjectStatsWithDelta satisfiedProjectStats()
		{
			var satisfiedPregroupingAgentsThis = pregrouping.groups().ofWhichSatisfiedIn(matching).asAgents();
			var satisfiedPregroupingAgentsChall = pregrouping.groups().ofWhichSatisfiedIn(challengerMatching).asAgents();
			
			return ProjectStats.from(AgentToProjectMatching.from(matching).filteredBy(satisfiedPregroupingAgentsThis))
			                   .withDelta(AgentToProjectMatching.from(challengerMatching).filteredBy(satisfiedPregroupingAgentsChall));
		}
		
		public TogethernessStatsWithDelta satisfiedPregroupStats()
		{
			return TogethernessStats.from(matching, pregrouping)
					       .withDeltasTo(challengerMatching);
		}
		
		public ProjectStatsWithDelta unsatisfiedProjectStats()
		{
			var unsatisfiedPregroupingAgents = new UnsatisifedPregroupingAgents(matching, pregrouping);
			var unsatisfiedInChall = new UnsatisifedPregroupingAgents(challengerMatching, pregrouping);
			
			return ProjectStats.from(AgentToProjectMatching.from(matching).filteredBy(unsatisfiedPregroupingAgents))
			                   .withDelta(AgentToProjectMatching.from(challengerMatching).filteredBy(unsatisfiedInChall));
		}
		
		public int countUnsatisfied()
		{
			return new UnsatisifedPregroupingAgents(matching, pregrouping).count();
		}
	}
	
	public record TogethernessStats(Pregrouping pregrouping,
							 NumberProposedGroupsTogether groupsTogether, int numTotalPregroups,
	                         NumberPregroupingStudentsTogether studentsTogether, int numTotalPregroupingStudents,
	                         NumGroupsPerGroupSizeDist numGroupsTogetherPerSize,
	                         Profile profileOnlySatisfiedPregroupers
	) {
		static TogethernessStats from(GroupToProjectMatching<?> matching, Pregrouping pregrouping)
		{
			// - number of pregroup groups together / of max
			// - number of pregroup students together / of max
			var groupsTogether = new NumberProposedGroupsTogether(matching, pregrouping.groups());
			var studentsTogether = new NumberPregroupingStudentsTogether(matching, pregrouping.groups());
			
			var satisfiedPregroupGroups = pregrouping.groups().ofWhichSatisfiedIn(matching);
			var numGroupsPerGroupSizeDist = new NumGroupsPerGroupSizeDist(satisfiedPregroupGroups, matching.datasetContext().groupSizeConstraint().maxSize());
			
			var onlySatisfiedPregroupingStudentsMatchings = AgentToProjectMatching.from(matching).filteredBy(satisfiedPregroupGroups.asAgents());
			var profileOnlySatisfiedPregroupers = Profile.of(onlySatisfiedPregroupingStudentsMatchings);
			
			return new TogethernessStats(pregrouping, groupsTogether, pregrouping.groups().count(), studentsTogether, pregrouping.groups().asAgents().count(), numGroupsPerGroupSizeDist, profileOnlySatisfiedPregroupers);
		}
		
		public TogethernessStatsWithDelta withDeltasTo(GroupToProjectMatching otherMatching)
		{
			var other = TogethernessStats.from(otherMatching, pregrouping);
			
			var delta = new TogethernessStatsDelta(this.groupsTogether.asInt() - other.groupsTogether.asInt(),
			                                  this.studentsTogether.asInt() - other.studentsTogether.asInt(),
			                                  profileOnlySatisfiedPregroupers.differenceTo(other.profileOnlySatisfiedPregroupers));
			
			return new TogethernessStatsWithDelta(pregrouping,
			                                      groupsTogether, numTotalPregroups,
			                                      studentsTogether, numTotalPregroupingStudents,
			                                      numGroupsTogetherPerSize,
			                                      profileOnlySatisfiedPregroupers,
			                                      delta);
		}
	}
	
	public record TogethernessStatsWithDelta(Pregrouping pregrouping,
							 NumberProposedGroupsTogether groupsTogether, int numTotalPregroups,
	                         NumberPregroupingStudentsTogether studentsTogether, int numTotalPregroupingStudents,
	                         NumGroupsPerGroupSizeDist numGroupsTogetherPerSize,
	                         Profile profileOnlySatisfiedPregroupers,
                             TogethernessStatsDelta delta)
	{
	}
	
	public record TogethernessStatsDelta(int groupsTogetherDelta,
	                              int studentsTogetherDelta,
	                              Profile profileDelta)
	{
		public int groupsTogether()
		{
			return groupsTogetherDelta;
		}
		
		public int studentsTogether()
		{
			return studentsTogetherDelta;
		}
	}
	
	public record ProjectStats(SumOfRanks sumOfRanks, WorstAssignedRank worstAssignedRank, Profile profile)
	{
		public static ProjectStats from(AgentToProjectMatching matching)
		{
			return new ProjectStats(SumOfRanks.of(matching), WorstAssignedRank.ProjectToStudents.in(matching), Profile.of(matching));
		}
		
		public ProjectStatsWithDelta withDelta(AgentToProjectMatching otherMatching)
		{
			var other = ProjectStats.from(otherMatching);
			return new ProjectStatsWithDelta(this, this.deltaBetween(otherMatching));
		}
		
		private ProjectStatsDelta deltaBetween(AgentToProjectMatching otherMatching)
		{
			var other = ProjectStats.from(otherMatching);
			
			return new ProjectStatsDelta(
					this.sumOfRanks.asInt() - other.sumOfRanks.asInt(),
					this.worstAssignedRank.asInt() - other.worstAssignedRank.asInt(),
					profile.differenceTo(Profile.of(otherMatching))
			);
		}
		
		
	}
	
	public record ProjectStatsWithDelta(SumOfRanks sumOfRanks, WorstAssignedRank worstAssignedRank, Profile profile, ProjectStatsDelta delta)
	{
		ProjectStatsWithDelta(ProjectStats stats, ProjectStatsDelta delta)
		{
			this(stats.sumOfRanks, stats.worstAssignedRank, stats.profile, delta);
		}
	}
	
	public record ProjectStatsDelta(int sorDelta, int worstDelta, Profile.ProfileDelta profileDelta)
	{
		public int sumOfRanks()
		{
			return sorDelta;
		}
		
		public int worstAssignedRank()
		{
			return worstDelta;
		}
	
		public Profile.ProfileDelta profile()
		{
			return profileDelta;
		}
	}
	
	public String simple_name(GroupProjectAlgorithm algorithm)
	{
		var canonical = algorithm.name().toLowerCase();
		
		if (canonical.contains("fair"))
		{
			if (canonical.contains("any"))
				return "FAIR - ANY";
			
			if (canonical.contains("except"))
				return "FAIR - EXCEPT";
			
			if (canonical.contains("max"))
				return "FAIR - MAX";
			
			throw new RuntimeException("Cant determine short name for '%s'".formatted(canonical));
		}
		
		if (canonical.contains("chiarandini") || canonical.contains("chiaranini"))
		{
			if (canonical.contains("any"))
				return "CHIA - ANY";
			
			if (canonical.contains("except"))
				return "CHIA - EXCEPT";
			
			if (canonical.contains("max"))
				return "CHIA - MAX";
		}
		
		throw new RuntimeException("Cant determine short name for '%s'".formatted(canonical));
	}
	
	public void writeAsHtmlToFile(File file)
	{
		var html = this.asHtmlSource();
//		var htmlStyled = htmlWithCss(html);

		try (var writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), false))) {
			writer.write(html);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String embed(JFreeChart chart)
	{
		try {
			var data = ChartUtils.encodeAsPNG(chart.createBufferedImage(1000,800));
			return "data:image/png;base64," + new String(Base64.encodeBase64(data));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
//	static String htmlWithCss(String html)
//	{
//		try
//		{
//			var css = new String(Thread.currentThread().getContextClassLoader().getResourceAsStream("markdown.css").readAllBytes(), StandardCharsets.UTF_8);
//
//			return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n" +
//				"<style type=\"text/css\">" + css + "</style>" +
//				"</head><body>" + html + "\n" +
//				"</body></html>";
//		}
//		catch (IOException e)
//		{
//			throw new RuntimeException(e);
//		}
//
//	}
}