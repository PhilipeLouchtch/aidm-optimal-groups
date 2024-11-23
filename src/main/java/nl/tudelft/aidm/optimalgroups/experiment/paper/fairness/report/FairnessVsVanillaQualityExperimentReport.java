package nl.tudelft.aidm.optimalgroups.experiment.paper.fairness.report;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.steppschuh.markdowngenerator.Markdown;
import net.steppschuh.markdowngenerator.table.Table;
import nl.tudelft.aidm.optimalgroups.Algorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.experiment.viz.RankProfilePlot;
import nl.tudelft.aidm.optimalgroups.metric.PopularityMatrix2;
import nl.tudelft.aidm.optimalgroups.metric.dataset.AvgPreferenceRankOfProjects;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.metric.matching.NumberAgentsMatched;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCRStudent;
import nl.tudelft.aidm.optimalgroups.metric.matching.gini.GiniCoefficientStudentRank;
import nl.tudelft.aidm.optimalgroups.metric.matching.group.Togetherness;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

@SuppressWarnings("DuplicatedCode")
public class FairnessVsVanillaQualityExperimentReport
{
	private final DatasetContext datasetContext;
	private final Pregrouping pregrouping;
	private final List<GroupProjectAlgorithm.Result> results;
	
	private final Agents agentsPregrouping;
	private final Agents agentsSingle;
	
	private StringBuffer doc;
	public FairnessVsVanillaQualityExperimentReport(DatasetContext datasetContext, Pregrouping pregrouping, List<GroupProjectAlgorithm.Result> results)
	{
		this.datasetContext = datasetContext;
		this.pregrouping = pregrouping;
		this.results = results;
		
		this.agentsPregrouping = pregrouping.groups().asAgents();
		this.agentsSingle = datasetContext.allAgents().without(agentsPregrouping);
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
				horizontalLine();
				algoResultInMarkdown(result);
			}

		return doc.toString();
	}

	private void datasetInfo()
	{
		var numberAllStudents = datasetContext.allAgents().count();
		var numProjects = datasetContext.allProjects().count();
		
		var indifferentAgents = datasetContext.allAgents().asCollection()
			                        .stream().filter(agent -> agent.projectPreference().isCompletelyIndifferent())
			                        .collect(collectingAndThen(toList(), Agents::from));
		
		var cliques = pregrouping.groups();
		
		var numberIndividualStudents = numberAllStudents - cliques.asAgents().count();
		var numberStudentsWithGroupPref = cliques.asAgents().count();
		var numberIndifferentStudents = indifferentAgents.count();
		
//		 slots...
//		boolean allProjectsHaveSameAmountOfSlots = datasetContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).distinct().count() == 1;
//		Assert.that(allProjectsHaveSameAmountOfSlots).orThrowMessage("Not implemented: handling projects with heterogeneous amount of slots");
		var numSlotsInfo = (datasetContext instanceof SDUDatasetContext)
				? "#slots per project: variable (SDU dataset)"
				: "\\#slots per project: " + datasetContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).findAny().getAsInt();
		
		var groupSizeInfo = (datasetContext instanceof SDUDatasetContext)
				? "group sizes: SDU dataset - multiple"
				: "group sizes, min: %s, max: %s".formatted(datasetContext.groupSizeConstraint().minSize(), datasetContext.groupSizeConstraint().maxSize());
		
		heading("Dataset info", 2);
		unorderedList(
			"\\#agents: " + numberAllStudents,
			"\\#projects: " + numProjects,
			numSlotsInfo,
			groupSizeInfo
		);
		
		heading(numberAllStudents + " students, of which:", 3);
		unorderedList(
			String.format("%s / %s individual students (have no grouping pref, or do not meet conditions)", numberIndividualStudents, numberAllStudents),
			String.format("%s / %s of which are indifferent (no project preference)", numberIndifferentStudents, numberAllStudents),
			String.format("%s / %s students who want to pre-group -- %s groups", numberStudentsWithGroupPref, numberAllStudents, pregrouping.groups().count())
		);
		
		var pregroupingClusters = pregrouping.groups().asCollection().stream().map(g -> g.members().count()).collect(groupingBy(x->x, counting()));
		var pregroupingClustersStrings = pregroupingClusters.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> String.format("Groups with %s members: %s", entry.getKey(), entry.getValue()))
				.toArray(String[]::new);
		
		heading("Requested, valid pre-groupings:", 3);
		unorderedList(pregroupingClustersStrings);

		image(AvgPreferenceRankOfProjects.ofAgentsInDatasetContext(datasetContext).asChart());
		

//		var binnedProjectPreferences = BinnedProjectPreferences.exactTopRanksBins(dataContext, 3, 30);
//		doc.append(binnedProjectPreferences.asMarkdownTable()).append("\n");
	}

	private void summary(List<GroupProjectAlgorithm.Result> results)
	{
		record GroupProjectAlgoResultForPopularityMatrix(Algorithm algo, AgentToProjectMatching producedMatching) implements Algorithm.Result<Algorithm, AgentToProjectMatching> {}
		
		var resultsAgentProject = results.stream()
				.map(result -> new GroupProjectAlgoResultForPopularityMatrix(result.algo(), AgentToProjectMatching.from(result.producedMatching())))
				.toList();
		
		Function<PopularityMatrix2, String[]> popMatrixToStrings = popMatrix -> {
			var henkie = (List<String>) popMatrix.deduplicatedByWinner().stream().map(o -> o.toString()).toList();
			return henkie.toArray(String[]::new);
		};
		
		var popAll = popMatrixToStrings.apply(PopularityMatrix2.from(resultsAgentProject));
		var popSingles = popMatrixToStrings.apply(PopularityMatrix2.from(resultsAgentProject, agentsSingle));
		var popPregrouping = popMatrixToStrings.apply(PopularityMatrix2.from(resultsAgentProject, agentsPregrouping));
		
		heading("Summary of results", 2);
			heading("Algorithm popularity", 3);
				doc.append(Markdown.italic("Algorithm name followed by the number of agents, in braces, that prefer it over the other") + "\n");
				heading("All agents", 4);
					unorderedList(popAll);
				heading("'Single' agents", 4);
					unorderedList(popSingles);
				heading("'Pregrouping' agents", 4);
					unorderedList(popPregrouping);
	}

	private void algoResultInMarkdown(GroupProjectAlgorithm.Result algoResult)
	{
		var matching = algoResult.producedMatching();
		var datasetContext = matching.datasetContext();
		
		
		var preformedGroups = pregrouping.groups();
		
		var matchingIndividualsToProjects = AgentToProjectMatching.from(matching);
		
		var matchingPregroupedSatisfied = AgentToProjectMatching.from( matching.filteredBySubsets(preformedGroups) ).filteredBy(agentsPregrouping);
		
		var pregroupingStudentsSatisfied = matchingPregroupedSatisfied.agents();
		var pregroupingStudentsUnsatisfied = agentsPregrouping.without(pregroupingStudentsSatisfied);
		var matchingPregroupedUnsatisfied = matchingIndividualsToProjects.filteredBy(pregroupingStudentsUnsatisfied);
		
		var matchingSingles = matchingIndividualsToProjects.filteredBy(agentsSingle);
		var matchingPregrouped = matchingIndividualsToProjects.filteredBy(agentsPregrouping);
		
		
		var studentPerspectiveMetrics = new MatchingMetrics.StudentProject(AgentToProjectMatching.from(matching));
		var groupPerspectiveMetrics = new MatchingMetrics.GroupProject(matching);
		
		heading("Algorithm: " + algoResult.algo().name(), 2);

			heading("Individuals' perspective", 3);
	
				var numStudentsMatched = NumberAgentsMatched.fromGroupMatching(matching).asInt();
				int numStudentsInDataset = datasetContext.allAgents().count();
				text("Number of students matched: %s (out of: %s)\n\n", numStudentsMatched, numStudentsInDataset);
		
				var rankDistribution = new RankProfilePlot(
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingSingles), "'Single' student"),
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingPregroupedSatisfied), "Satisfied 'pregrouping' student"),
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingPregroupedUnsatisfied), "Unsatisfied 'pregrouping' student"));
				
				image( rankDistribution.asChart(algoResult.algo().name()) );
				table( rankDistribution.asTable().asMarkdownTable() );
		
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
				
				heading("'Pre-grouped' students perspective (regardless of grouping satisfaction)", 4);
				//			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
		
					var giniPregrouped = new GiniCoefficientStudentRank(matchingPregrouped);
					var aupcrPregrouped = new AUPCRStudent(matchingPregrouped);
		
					var numPreformedGroupsTogether = Togetherness.from(matching, preformedGroups).numGroups();
					unorderedList(
							String.format("Number of preformed groups together: %s / %s ", numPreformedGroupsTogether, preformedGroups.count()),
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
