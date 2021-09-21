package nl.tudelft.aidm.optimalgroups.experiment.paper.fairness.report;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.steppschuh.markdowngenerator.Markdown;
import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.experiment.agp.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentAlgorithmSubresult;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentResult;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.ExperimentReportInHtml;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.profile.RankProfileOfIndividualAndGroupingStudents;
import nl.tudelft.aidm.optimalgroups.metric.PopularityMatrix;
import nl.tudelft.aidm.optimalgroups.metric.dataset.AvgPreferenceRankOfProjects;
import nl.tudelft.aidm.optimalgroups.metric.group.LeastWorstIndividualRankInGroupDistribution;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.metric.matching.NumberAgentsMatched;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCRStudent;
import nl.tudelft.aidm.optimalgroups.metric.matching.gini.GiniCoefficientStudentRank;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import plouchtch.assertion.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("DuplicatedCode")
public class FairnessVsVanillaQualityExperimentReport
{
	private final DatasetContext datasetContext;
	private final Pregrouping pregrouping;
	private final List<GroupProjectAlgorithm.Result> results;
	
	private StringBuffer doc;
	public FairnessVsVanillaQualityExperimentReport(DatasetContext datasetContext, Pregrouping pregrouping, ArrayList<GroupProjectAlgorithm.Result> results)
	{
		this.datasetContext = datasetContext;
		this.pregrouping = pregrouping;
		this.results = results;
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

	public String asMarkdownSource()
	{
		return experimentToMarkdown();
	}

	private String experimentToMarkdown()
	{
		doc = new StringBuffer();
		
		heading("Experiment - " + datasetContext.identifier(), 1);
			
			datasetInfo();
			summary(results);
			
			for (var result : results) {
				algoResultInMarkdown(result);
			}

		return doc.toString();
	}

	private void datasetInfo()
	{
		var numberAllStudents = datasetContext.allAgents().count();
		var numProjects = datasetContext.allProjects().count();
		var groupSize = datasetContext.groupSizeConstraint();
		
		var indifferentAgents = datasetContext.allAgents().asCollection()
			                        .stream().filter(agent -> agent.projectPreference().isCompletelyIndifferent())
			                        .collect(collectingAndThen(toList(), Agents::from));
		
		var cliques = pregrouping.groups();
		
		var numberIndividualStudents = numberAllStudents - cliques.asAgents().count();
		var numberStudentsWithGroupPref = cliques.asAgents().count();
		var numberIndifferentStudents = indifferentAgents.count();
		
		// slots...
		boolean allProjectsHaveSameAmountOfSlots = datasetContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).distinct().count() == 1;
		Assert.that(allProjectsHaveSameAmountOfSlots).orThrowMessage("Not implemented: handling projects with heterogeneous amount of slots");
		var numSlots = datasetContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).findAny().getAsInt();
		

		heading("Dataset info", 2);

		unorderedList(
			"\\#agents: " + numberAllStudents,
			"\\#projects: " + numProjects,
			"\\#slots per project: " + numSlots,
			"group sizes, min: " + groupSize.minSize() + ", max: " + groupSize.maxSize()
		);
		
		heading(numberAllStudents + " students, of which:", 3);
		unorderedList(
			String.format("%s / %s individual students (empty group-pref, or does not meet condition)", numberIndividualStudents, numberAllStudents),
			String.format("%s / %s students who want to pre-group", numberStudentsWithGroupPref, numberAllStudents),
			String.format("%s / %s indifferent students (empty project pref)", numberIndifferentStudents, numberAllStudents)
		);

		image(AvgPreferenceRankOfProjects.ofAgentsInDatasetContext(datasetContext).asChart());

//		var binnedProjectPreferences = BinnedProjectPreferences.exactTopRanksBins(dataContext, 3, 30);
//		doc.append(binnedProjectPreferences.asMarkdownTable()).append("\n");
	}

	private void summary(List<GroupProjectAlgorithm.Result> results)
	{
		var popMatrix = new PopularityMatrix.TopicGroup(results);
		var items = popMatrix.asSet().stream().map(Object::toString).collect(toList()).toArray(String[]::new);
		
		heading("Summary of results", 2);
			heading("Algorithm popularity", 3);
				doc.append(Markdown.italic("Algorithm name followed by the number of agents, in braces, that prefer it over the other") + "\n");
				unorderedList(items);
	}

	private void algoResultInMarkdown(GroupProjectAlgorithm.Result algoResult)
	{
		var datasetContext = algoResult.producedMatching().datasetContext();
		
		var preformedGroups = pregrouping.groups();
		var pregroupingStudents = preformedGroups.asAgents();
		var singleStudents = datasetContext.allAgents().without(pregroupingStudents);
		
		var matchingIndividualsToProjects = AgentToProjectMatching.from(algoResult.producedMatching());
		
		var matchingSingles = matchingIndividualsToProjects.filteredBy(singleStudents);
		var matchingPregrouped = matchingIndividualsToProjects.filteredBy(pregroupingStudents);
		
		var studentPerspectiveMetrics = new MatchingMetrics.StudentProject(AgentToProjectMatching.from(algoResult.producedMatching()));
		var groupPerspectiveMetrics = new MatchingMetrics.GroupProject(algoResult.producedMatching());
		
		heading("Algorithm: " + algoResult.algo().name(), 2);

			heading("Individuals' perspective", 3);
	
				var numStudentsMatched = NumberAgentsMatched.fromGroupMatching(algoResult.producedMatching()).asInt();
				int numStudentsInDataset = datasetContext.allAgents().count();
				text("Number of students matched: %s (out of: %s)\n\n", numStudentsMatched, numStudentsInDataset);
		
				var rankDistribution = new RankProfileOfIndividualAndGroupingStudents(matchingSingles, matchingPregrouped)
						.asChart(algoResult.algo().name());
				image(rankDistribution);
		
//				var groups = algoResult.producedMatching().asList().stream().map(Match::from).collect(Collectors.toList());
//				var bestWorstIndividualRankInGroupDistribution = new LeastWorstIndividualRankInGroupDistribution(groups).asChart();
//				image(bestWorstIndividualRankInGroupDistribution);
		
		
				heading("General perspective", 4);
				
					unorderedList(
						"Gini: " + studentPerspectiveMetrics.giniCoefficient().asDouble(),
						"AUPCR: " + studentPerspectiveMetrics.aupcr().asDouble(),
						"Worst rank: " + studentPerspectiveMetrics.worstRank().asInt()
					);
		
				heading("'Single' students perspective", 4);
				//			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
				
					var giniSingles = new GiniCoefficientStudentRank(matchingSingles);
					var aupcrSingles = new AUPCRStudent(matchingSingles);
					
					unorderedList(
						"Gini: " + giniSingles.asDouble(),
						"AUPCR: " + aupcrSingles.asDouble()
					);
				
				heading("'Pre-grouped' students perspective", 4);
				//			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
		
					var giniPregrouped = new GiniCoefficientStudentRank(matchingPregrouped);
					var aupcrPregrouped = new AUPCRStudent(matchingPregrouped);
		
					unorderedList(
						"Gini: " + giniPregrouped.asDouble(),
						"AUPCR: " + aupcrPregrouped.asDouble()
					);


//			heading("Groups' perspective", 4);
////			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
//
//				unorderedList(
//					"Gini: " + groupPerspectiveMetrics.giniCoefficient().asDouble(),
//					"AUPCR: " + groupPerspectiveMetrics.aupcr().asDouble()
//				);
				
	}
	
	private void heading(String value, int level)
	{
		doc.append(Markdown.heading(value, level)).append("\n");
	}
	
	private void unorderedList(String... items)
	{
		doc.append(Markdown.unorderedList((Object[]) items)).append("\n\n");
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
}
