package nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.setting;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.steppschuh.markdowngenerator.Markdown;
import net.steppschuh.markdowngenerator.table.Table;
import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.SatisfiedPregroupingAgents;
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
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
	
	public String asMarkdownSource()
	{
		var pregrouping = challenger.pregroupingType().instantiateFor(datasetContext);
		var soloCohort = datasetContext.allAgents().without(pregrouping.groups().asAgents());
		
		var challengerResult = challenger.determineMatching(datasetContext);
		
		var challResultAgentProjectAll = AgentToProjectMatching.from(challengerResult);
		var challengerMatchingOnlySoloStudents = challResultAgentProjectAll.filteredBy(soloCohort);
		var satisfiedAgentsChall = pregrouping.groups().ofWhichSatisfied(challengerResult).asAgents();
		var matchingChallengerSatisfiedPregroupers = challResultAgentProjectAll.filteredBy(satisfiedAgentsChall);
		
		heading(1, "%s - %s".formatted(datasetId, simple_name(challenger)));
		
			heading(2, "Challenger %s".formatted(simple_name(challenger)));
			text("Pregrouping type: '%s'\n".formatted(challenger.pregroupingType().canonicalName()));
			
			heading(3, "solo");
				projectStats(challengerMatchingOnlySoloStudents);
				
			heading(3, "pregrouping");
				projectStats(matchingChallengerSatisfiedPregroupers);
				togethernessStats(challengerResult, pregrouping);
				
				heading(4, "- unsatisfied:");
					var unsatisfiedAgentsInChall = new UnsatisifedPregroupingAgents(challengerResult, pregrouping);
					var matchingUnsatisfiedInChall = challResultAgentProjectAll.filteredBy(unsatisfiedAgentsInChall);
					projectStats(matchingUnsatisfiedInChall);
				
			for (var oppo : opponents)
			{
				var oppoResult = oppo.determineMatching(datasetContext);
				
				var oppoResultAgentProjectAll = AgentToProjectMatching.from(oppoResult);
				var oppoMatchingOnlySoloStudents = oppoResultAgentProjectAll.filteredBy(soloCohort);
				
				heading(2, "Oppo %s".formatted(simple_name(oppo)));
				
				// Absolute metrics per cohort
				heading(3, "solo");
					// Project outcomes only
					// - relative profile
					// - absolute profile
					projectStatsWithRelative(oppoMatchingOnlySoloStudents, challengerMatchingOnlySoloStudents);
					
				heading(3, "pregrouping");
					// Pregroup grouping info:
					// - number of pregroup groups together / of max
					// - number of pregroup students together / of max
					togethernessStatsWithRelative(oppoResult, challengerResult, pregrouping);
					
					// Project outcome for the pregrouping students
					// - relative profile
					// - absolute profile
					var satisfiedAgentsOppo = new SatisfiedPregroupingAgents(oppoResult, pregrouping);
					projectStatsWithRelative(oppoResultAgentProjectAll.filteredBy(satisfiedAgentsOppo), matchingChallengerSatisfiedPregroupers);
				
					// of which unsatisifed, so lets look at their project outcome
					heading(4, "of which unsatisfied:");
						// Project outcome for the pregrouping students
						// - relative profile
						// - absolute profile
						var unsatisfiedAgentsOppo = new UnsatisifedPregroupingAgents(oppoResult, pregrouping);
						final var matchingUnsatisfiedInOppo = oppoResultAgentProjectAll.filteredBy(unsatisfiedAgentsOppo);
						projectStatsWithRelative(matchingUnsatisfiedInOppo, matchingUnsatisfiedInChall);
			}
			
		return doc.toString();
	}
	
	public void togethernessStats(GroupToProjectMatching<?> matching, Pregrouping pregrouping)
	{
		// - number of pregroup groups together / of max
		// - number of pregroup students together / of max
		NumberProposedGroupsTogether groupsTogether = new NumberProposedGroupsTogether(matching, pregrouping.groups());
		NumberPregroupingStudentsTogether studentsTogether = new NumberPregroupingStudentsTogether(matching, pregrouping.groups());
		
		var satisfiedPregroupGroups = pregrouping.groups().ofWhichSatisfied(matching);
		
		text("Groups together: %s / %s\n\n".formatted(groupsTogether.asInt(), pregrouping.groups().count()));
		text("Students together: %s / %s\n\n".formatted(studentsTogether.asInt(), pregrouping.groups().asAgents().count()));
		text("Dist (groups per size) %s\n\n".formatted(new NumGroupsPerGroupSizeDist(satisfiedPregroupGroups, matching.datasetContext().groupSizeConstraint().maxSize()).toString()));
		
		text("Profile project rank: \n\n" + Profile.of(AgentToProjectMatching.from(matching).filteredBy(satisfiedPregroupGroups.asAgents())).toString());
	}
	
	public void togethernessStatsWithRelative(GroupToProjectMatching matching, GroupToProjectMatching relative, Pregrouping pregrouping)
	{
		// - number of pregroup groups together / of max
		// - number of pregroup students together / of max
		var groupsTogether = new NumberProposedGroupsTogether(matching, pregrouping.groups());
		var studentsTogether = new NumberPregroupingStudentsTogether(matching, pregrouping.groups());
		
		text("Groups together: %s / %s  (%s) \n\n".formatted(
				groupsTogether.asInt(),
				pregrouping.groups().count(),
				new NumberProposedGroupsTogether(relative, pregrouping.groups()).asInt() - groupsTogether.asInt()
		     )
		);
		
		var studentsTogetherOther = new NumberPregroupingStudentsTogether(relative, pregrouping.groups());
		text("Students together: %s / %s  (%s) \n\n".formatted(
				studentsTogether.asInt(),
				pregrouping.groups().asAgents().count(),
				studentsTogether.asInt() - studentsTogetherOther.asInt()
		));
	}
	
	public void projectStats(AgentToProjectMatching matching)
	{
		// todo: pareto better check
		text("%s [%s]\n\n".formatted(SumOfRanks.of(matching).asInt(), WorstAssignedRank.ProjectToStudents.in(matching).asInt()));
		text("%s\n\n".formatted(Profile.of(matching).toString()));
	}
	
	public void projectStatsWithRelative(AgentToProjectMatching matching, AgentToProjectMatching matchingRel)
	{
		// todo: pareto better check
		text("%s (%s) - [%s (%s)]\n\n".formatted(
				SumOfRanks.of(matching).asInt(),
				SumOfRanks.of(matching).asInt() - SumOfRanks.of(matchingRel).asInt(),
				WorstAssignedRank.ProjectToStudents.in(matching).asInt(),
				WorstAssignedRank.ProjectToStudents.in(matching).asInt() - WorstAssignedRank.ProjectToStudents.in(matchingRel).asInt()
		));
		
		var profileThis = Profile.of(matching);
		
		text("Profile delta: %s\n\n".formatted(profileThis.subtracted(Profile.of(matchingRel)).toString()));
		text("profile full:  %s\n\n".formatted(profileThis.toString()));
	}
	
	public String simple_name(GroupProjectAlgorithm algorithm)
	{
		var canonical = algorithm.name().toLowerCase();
		
		if (canonical.contains("fair"))
		{
			if (canonical.contains("any"))
				return "FAIR - ANY";
			
			if (canonical.contains("max"))
				return "FAIR - MAX";
			
			if (canonical.contains("except"))
				return "FAIR - EXCEPT";
			
			throw new RuntimeException("Cant determine short name for '%s'".formatted(canonical));
		}
		
		if (canonical.contains("chiarandini") || canonical.contains("chiaranini"))
		{
			if (canonical.contains("any"))
				return "CHIA - ANY";
			
			if (canonical.contains("max"))
				return "CHIA - MAX";
			
			if (canonical.contains("except"))
				return "CHIA - EXCEPT";
		}
		
		throw new RuntimeException("Cant determine short name for '%s'".formatted(canonical));
	}
	
	public void writeAsHtmlToFile(File file)
	{
		var html = this.asHtmlSource();
		var htmlStyled = htmlWithCss(html);

		try (var writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), false))) {
			writer.write(htmlStyled);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String asHtmlSource()
	{
		var markdownSrc = this.asMarkdownSource();
		
		/* Markdown to Html stuff */
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

		Parser parser = Parser.builder(options).build();
		HtmlRenderer renderer = HtmlRenderer.builder(options).build();

		Document parsed = parser.parse(markdownSrc);
		var asHtmlSource = renderer.render(parsed);

		return asHtmlSource;
	}
	
	
	private void heading(int level, String value)
	{
		doc.append(Markdown.heading(value, level)).append("\n\n");
	}
	
	private void unorderedList(String... items)
	{
		doc.append(Markdown.unorderedList((Object[]) items)).append("\n\n\n");
	}
	
	private void text(String text)
	{
		doc.append(Markdown.text(text));
	}
	
	private void text(String format, Object... args)
	{
		text(String.format(format, args));
	}
	
	private void image(JFreeChart chart)
	{
		doc.append(Markdown.image(embed(chart))).append("\n\n");
	}
	
	private void table(Table table)
	{
		doc.append(table).append("\n\n");
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
	
	private void horizontalLine()
	{
		doc.append(Markdown.rule())
			.append("\n");
	}
	
	static String htmlWithCss(String html)
	{
		try
		{
			var css = new String(Thread.currentThread().getContextClassLoader().getResourceAsStream("markdown.css").readAllBytes(), StandardCharsets.UTF_8);

			return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n" +
				"<style type=\"text/css\">" + css + "</style>" +
				"</head><body>" + html + "\n" +
				"</body></html>";
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

	}
}
