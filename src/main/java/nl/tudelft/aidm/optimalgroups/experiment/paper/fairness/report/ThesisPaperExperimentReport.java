package nl.tudelft.aidm.optimalgroups.experiment.paper.fairness.report;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.steppschuh.markdowngenerator.Markdown;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.Pregrouping;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.experiment.agp.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentAlgorithmSubresult;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentResult;
import nl.tudelft.aidm.optimalgroups.metric.group.LeastWorstIndividualRankInGroupDistribution;
import nl.tudelft.aidm.optimalgroups.metric.matching.NumberAgentsMatched;
import nl.tudelft.aidm.optimalgroups.metric.matching.aupcr.AUPCRStudent;
import nl.tudelft.aidm.optimalgroups.metric.matching.gini.GiniCoefficientStudentRank;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import plouchtch.assertion.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("DuplicatedCode")
public class ThesisPaperExperimentReport
{
	private StringBuffer doc;
	
	private final List<Experiment> experiments;

	public ThesisPaperExperimentReport(Experiment... experiments)
	{
		this(List.of(experiments));
	}

	public ThesisPaperExperimentReport(List<Experiment> experiments)
	{
		this.experiments = experiments;
	}
	
	
	private String experimentToMarkdown()
	{
		doc = new StringBuffer();

		heading("Simulation results", 1);

		for (Experiment experiment : experiments)
		{
			heading("Experiment - " + experiment.datasetContext.identifier(), 2);
			
				datasetInfo(experiment);
				summary(experiment.result()) ;
				
				doc.append( algoResultsInMarkdown(experiment.result().results) );
				horizontalLine();
		}

		return doc.toString();
	}

	private void datasetInfo(Experiment experiment)
	{
		var dataContext = experiment.datasetContext;
		var numberAllStudents = dataContext.allAgents().count();
		var numProjects = dataContext.allProjects().count();
		var groupSize = dataContext.groupSizeConstraint();
		
		var indifferentAgents = dataContext.allAgents().asCollection()
			                        .stream().filter(agent -> agent.projectPreference().isCompletelyIndifferent())
			                        .collect(collectingAndThen(toList(), Agents::from));
		
		var cliques = CliqueGroups.from(dataContext.allAgents());
		
		var numberIndividualStudents = numberAllStudents - cliques.asAgents().count();
		var numberStudentsWithGroupPref = cliques.asAgents().count();
		var numberIndifferentStudents = indifferentAgents.count();
		
		// slots...
		boolean allProjectsHaveSameAmountOfSlots = dataContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).distinct().count() == 1;
		Assert.that(allProjectsHaveSameAmountOfSlots).orThrowMessage("Not implemented: handling projects with heterogeneous amount of slots");
		var numSlots = dataContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).findAny().getAsInt();
		

		heading("Dataset info", 3);

		unorderedList(
			"\\#agents: " + numberAllStudents,
			"\\#projects: " + numProjects,
			"\\#slots per project: " + numSlots,
			"group sizes, min: " + groupSize.minSize() + ", max: " + groupSize.maxSize()
		);
		
		heading(numberAllStudents + " students, of which:", 4);
		unorderedList(
			String.format("%s / %s individual students (empty group-pref, or does not meet condition)", numberIndividualStudents, numberAllStudents),
			String.format("%s / %s students who want to pre-group", numberStudentsWithGroupPref, numberAllStudents),
			String.format("%s / %s indifferent students (empty project pref)", numberIndifferentStudents, numberAllStudents)
		);

		JFreeChart distribProjectsInPreferencesChart = experiment.projectRankingDistribution.asChart();
		doc.append(Markdown.image(embed(distribProjectsInPreferencesChart))).append("\n\n");

//		var binnedProjectPreferences = BinnedProjectPreferences.exactTopRanksBins(dataContext, 3, 30);
//		doc.append(binnedProjectPreferences.asMarkdownTable()).append("\n");
	}

	private StringBuffer algoResultsInMarkdown(List<ExperimentAlgorithmSubresult> algoResults)
	{
		var doc = new StringBuffer();
		for (ExperimentAlgorithmSubresult algoResult : algoResults)
		{
			doc.append(algoResultInMarkdown(algoResult));
		}

		return doc;
	}

	private StringBuffer algoResultInMarkdown(ExperimentAlgorithmSubresult algoResult)
	{
		var datasetContext = algoResult.producedMatching().datasetContext();
		
		var cliques = CliqueGroups.from(datasetContext.allAgents());
		var pregroupingStudents = cliques.asAgents();
		var singleStudents = datasetContext.allAgents().without(pregroupingStudents);
		
		var matchingIndividualsToProjects = AgentToProjectMatching.from(algoResult.producedMatching());
		
		var matchingSingles = matchingIndividualsToProjects.filteredBy(singleStudents);
		var matchingPregrouped = matchingIndividualsToProjects.filteredBy(pregroupingStudents);
		
		var doc = new StringBuffer();

		heading("Algorithm: " + algoResult.algo().name(), 3);

			heading("Individuals' perspective", 4);
	
				var numStudentsMatched = NumberAgentsMatched.fromGroupMatching(algoResult.producedMatching()).asInt();
				int numStudentsInDataset = datasetContext.allAgents().count();
				text("Number of students matched: %s (out of: %s)\n\n", numStudentsMatched, numStudentsInDataset);
		
				var rankDistribution = algoResult.studentPerspectiveMetrics.rankDistribution().asChart(algoResult.algo().name());
				image(rankDistribution);
		
				var groups = algoResult.producedMatching().asList().stream().map(Match::from).collect(Collectors.toList());
				var bestWorstIndividualRankInGroupDistribution = new LeastWorstIndividualRankInGroupDistribution(groups).asChart();
				image(bestWorstIndividualRankInGroupDistribution);
		
		
				heading("General perspective", 5);
				
					unorderedList(
						"Gini: " + algoResult.studentPerspectiveMetrics.giniCoefficient().asDouble(),
						"AUPCR: " + algoResult.studentPerspectiveMetrics.aupcr().asDouble(),
						"Worst rank: " + algoResult.studentPerspectiveMetrics.worstRank().asInt()
					);
		
				heading("'Single' students perspective", 5);
				//			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
				
					var giniSingles = new GiniCoefficientStudentRank(matchingSingles);
					var aupcrSingles = new AUPCRStudent(matchingSingles);
					
					unorderedList(
						"Gini: " + giniSingles.asDouble(),
						"AUPCR: " + aupcrSingles.asDouble()
					);
				
				heading("'Pre-grouped' students perspective", 5);
				//			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
		
					var giniPregrouped = new GiniCoefficientStudentRank(matchingPregrouped);
					var aupcrPregrouped = new AUPCRStudent(matchingPregrouped);
		
					unorderedList(
						"Gini: " + giniPregrouped.asDouble(),
						"AUPCR: " + aupcrPregrouped.asDouble()
					);


			heading("Groups' perspective", 4);
//			doc.append(Markdown.image(embed(algoResult.projectProfileCurveGroup.asChart())) + "\n");
		
				unorderedList(
					"Gini: " + algoResult.groupPerspectiveMetrics.giniCoefficient().asDouble(),
					"AUPCR: " + algoResult.groupPerspectiveMetrics.aupcr().asDouble()
				);
		
			
		

		return doc;
	}
	
	private void heading(String value, int level)
	{
		doc.append(Markdown.heading(value, level)).append("\n");
	}
	
	private void unorderedList(String... items)
	{
		doc.append(Markdown.unorderedList((Object[]) items)).append("\n");
	}
	
	private void text(String text)
	{
		doc.append(Markdown.text(text));
	}
	
	private void text(String format, Object... args)
	{
		text(String.format(format, args));
	}

	private void summary(ExperimentResult experimentResult)
	{
		var items = experimentResult.popularityMatrix().asSet().stream().map(Object::toString).collect(toList()).toArray(String[]::new);
		
		heading("Summary of results", 3);
			heading("Algorithm popularity", 4);
				doc.append(Markdown.italic("Algorithm name followed by the number of agents, in braces, that prefer it over the other") + "\n");
				unorderedList(items);
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
	
	public String asMarkdownSource()
	{
		return experimentToMarkdown();
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
	
	public void writeAsPdfToFile(File file)
	{
		var html = this.asHtmlSource();
		
		MutableDataSet options = new MutableDataSet();

		try {
			PdfConverterExtension.exportToPdf(file.getAbsolutePath(), html, "", options);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
