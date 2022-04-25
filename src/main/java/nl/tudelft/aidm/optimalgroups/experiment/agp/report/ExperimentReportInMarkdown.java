package nl.tudelft.aidm.optimalgroups.experiment.agp.report;

import net.steppschuh.markdowngenerator.Markdown;
import nl.tudelft.aidm.optimalgroups.experiment.BinnedProjectPreferencesOverview;
import nl.tudelft.aidm.optimalgroups.experiment.agp.Experiment;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentAlgorithmSubresult;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentResult;
import nl.tudelft.aidm.optimalgroups.metric.group.LeastWorstIndividualRankInGroupDistribution;
import nl.tudelft.aidm.optimalgroups.metric.matching.NumberAgentsMatched;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import plouchtch.assertion.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class ExperimentReportInMarkdown
{
	private StringBuffer doc;
	
	private final List<Experiment> experiments;

	public ExperimentReportInMarkdown(Experiment... experiments)
	{
		this(List.of(experiments));
	}

	public ExperimentReportInMarkdown(List<Experiment> experiments)
	{
		this.experiments = experiments;
	}

	public String asMarkdownSource()
	{
		return experimentToMarkdown();
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
		var numAgents = dataContext.allAgents().count();
		var numProjects = dataContext.allProjects().count();
		var groupSize = dataContext.groupSizeConstraint();

		// slots...
		boolean allProjectsHaveSameAmountOfSlots = dataContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).distinct().count() == 1;
		Assert.that(allProjectsHaveSameAmountOfSlots).orThrowMessage("Not implemented: handling projects with heterogeneous amount of slots");
		var numSlots = dataContext.allProjects().asCollection().stream().mapToInt(value -> value.slots().size()).findAny().getAsInt();
		

		heading("Dataset info", 3);

		unorderedList(
			"\\#agents: " + numAgents,
			"\\#projects: " + numProjects,
			"\\#slots per project: " + numSlots,
			"group sizes, min: " + groupSize.minSize() + ", max: " + groupSize.maxSize()
		);

		JFreeChart distribProjectsInPreferencesChart = experiment.projectRankingDistribution.asChart();
		doc.append(Markdown.image(embed(distribProjectsInPreferencesChart))).append("\n\n");

		var binnedProjectPreferences = BinnedProjectPreferencesOverview.exactTopRanksBins(dataContext, 3, 30);
		doc.append(binnedProjectPreferences.asMarkdownTable()).append("\n");
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
		var doc = new StringBuffer();

		heading("Algorithm: " + algoResult.algo().name(), 3);

			heading("Student perspective", 4);
	
				var numStudentsMatched = NumberAgentsMatched.fromGroupMatching(algoResult.producedMatching()).asInt();
				int numStudentsInDataset = algoResult.producedMatching().datasetContext().allAgents().count();
				text("Number of students matched: %s (out of: %s)\n\n", numStudentsMatched, numStudentsInDataset);
		
				var rankDistribution = algoResult.studentPerspectiveMetrics.rankDistribution().asChart(algoResult.algo().name());
				image(rankDistribution);
		
				var groups = algoResult.producedMatching().asList().stream().map(Match::from).collect(Collectors.toList());
				var bestWorstIndividualRankInGroupDistribution = new LeastWorstIndividualRankInGroupDistribution(groups).asChart();
				image(bestWorstIndividualRankInGroupDistribution);
		
				unorderedList(
					"Gini: " + algoResult.studentPerspectiveMetrics.giniCoefficient().asDouble(),
					"AUPCR: " + algoResult.studentPerspectiveMetrics.aupcr().asDouble(),
					"Worst rank: " + algoResult.studentPerspectiveMetrics.worstRank().asInt()
				);


			heading("Group perspective", 4);
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
		heading("Summary of results", 3);

			heading("Algorithm popularity", 4);
				doc.append(Markdown.italic("Algorithm name followed by the number of agents, in braces, that prefer it over the other") + "\n");
				
				unorderedList(experimentResult.popularityMatrix().asSet().stream().map(Object::toString).toArray(String[]::new));
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
