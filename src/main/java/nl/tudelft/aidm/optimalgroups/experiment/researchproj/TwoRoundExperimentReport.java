package nl.tudelft.aidm.optimalgroups.experiment.researchproj;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.steppschuh.markdowngenerator.Markdown;
import nl.tudelft.aidm.optimalgroups.experiment.agp.ExperimentResult;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.ExperimentReportInHtml;
import nl.tudelft.aidm.optimalgroups.experiment.viz.RankProfilePlot;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.util.List;
import java.util.stream.Collectors;

public class TwoRoundExperimentReport
{
	private final StringBuffer doc;
	private final AgentToProjectMatching matchingRoundOne;
	private final AgentToProjectMatching matchingRoundTwo;
	private final Agents allAgents;
	private final Agents individualAgents;
	private final Agents groupingAgents;
	private final Agents indifferentAgents;
	
	public TwoRoundExperimentReport(AgentToProjectMatching matchingRoundOne, AgentToProjectMatching matchingRoundTwo, Agents allAgents, Agents individualAgents, Agents groupingAgents, Agents indifferentAgents)
	{
		this.matchingRoundOne = matchingRoundOne;
		this.matchingRoundTwo = matchingRoundTwo;
		
		this.allAgents = allAgents;
		this.individualAgents = individualAgents;
		this.groupingAgents = groupingAgents;
		this.indifferentAgents = indifferentAgents;
		doc = new StringBuffer();
	}

	public String asMarkdownSource()
	{
		return experimentToMarkdown();
	}

	private String experimentToMarkdown()
	{

//		doc.append(Markdown.heading("Simulation results", 1));
//		doc.append("\n");
//
//		doc.append( Markdown.heading("Experiment - " + experiment.datasetContext.identifier(), 2).toString() )
//			.append("\n");
//		doc.append( datasetInfo(experiment) );
//		doc.append( summary(experiment.result()) );
//		doc.append( algoResultsInMarkdown(experiment.result().results) );
//		doc.append( Markdown.rule() )
//			.append("\n");
		algoResultInMarkdown();

		return doc.toString();
	}
	
	public ExperimentReportInHtml asHtmlReport()
	{
		var markdown = experimentToMarkdown();
		
		/* Markdown to Html stuff */
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
		
		Parser parser = Parser.builder(options).build();
		HtmlRenderer renderer = HtmlRenderer.builder(options).build();
		
		Document parsed = parser.parse(markdown);
		var html = renderer.render(parsed);
		
		return new ExperimentReportInHtml(html);
	}
	
	private void algoResultInMarkdown()
	{
		var numberAllStudents = allAgents.count();
		var numberIndividualStudents = individualAgents.count();
		var numberStudentsWithGroupPref = groupingAgents.count();
		var numberIndifferentStudents = indifferentAgents.count();

		heading("Two round matching pareto", 2);
			// explain algo here

			heading("Dataset info", 3);
			
				heading(Markdown.code(allAgents.datasetContext.identifier()).toString(), 4);

				heading(numberAllStudents + " students, of which:", 4);
					unorderedList(
						String.format("%s / %s individual students (empty group-pref, or does not meet condition)", numberIndividualStudents, numberAllStudents),
						String.format("%s / %s students who want to pre-group", numberStudentsWithGroupPref, numberAllStudents),
						String.format("%s / %s indifferent students (empty project pref)", numberIndifferentStudents, numberAllStudents)
					);

			heading("Round one: group-preference agnostic matching", 3);
			
				var matchingRoundOneIndividualOnly = filterMatching(matchingRoundOne, individualAgents);
				var matchingRoundOneGroupedOnly = filterMatching(matchingRoundOne, groupingAgents);
				var chart = new RankProfilePlot(
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingRoundOneIndividualOnly), "'Single' student"),
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingRoundOneGroupedOnly), "'Pregrouping' student")
					).asChart("Round one");
				

				// stacked matching profile bar chart - individual, pre-grouping students
				// stackedbarchart_roundone
				embed(chart);

				// Objective functions
				// obj x
				// obj y ...


			heading("Round two: group and undominated individuals constraints", 3);
		
				var matchingRoundTwoIndividualOnly = filterMatching(matchingRoundTwo, individualAgents);
				var matchingRoundTwoGroupedOnly = filterMatching(matchingRoundTwo, groupingAgents);
				
				var chart2 = new RankProfilePlot(
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingRoundTwoIndividualOnly), "'Single' student"),
						new RankProfilePlot.NamedRankProfile(Profile.of(matchingRoundTwoGroupedOnly), "'Pregrouping' student")
					).asChart("Round one");
				
				embed(chart2);
				
				// text: (note the text in the introduction. So only need to refer to concepts already introduced)
					// Wherein we set the grouping constraints for the students that are allowed to form groups according to their group preference
					// and the matching is such that:
					//    1) it uses the same obj function as round 1,
					//    2) the profile of the individual students may only be better or same as that achieved during round 1
		
				// stackedbarchart_roundtwo

				// Objective functions
				// obj x
				// obj y ...
		
				// Metrics:
				//  - aupcr individual
				//  - aupcr grouped
		        //  - worst individual
				//  - worst grouped
		
				// fairness between grouped and individual... ?
	}
	
	private Matching<Agent, Project> filterMatching(Matching<Agent, Project> matching, Agents included)
	{
		var filtered = matching.asList()
                .stream()
				.filter(match -> included.contains(match.from()))
				.collect(Collectors.toList());
		
		return new AgentToProjectMatching.Simple(included.datasetContext, filtered);
	}

	private void heading(String value, int level)
	{
		doc.append(Markdown.heading(value, level)).append("\n");
	}

	private void unorderedList(String... items)
	{
		doc.append(Markdown.unorderedList((Object[]) items)).append("\n");
	}

	private String summary(ExperimentResult experimentResult)
	{
		String doc = Markdown.heading("Summary of results", 3) + "\n";

		doc += Markdown.heading("Algorithm popularity", 4) + "\n" +
			Markdown.italic("Algorithm name followed by the number of agents, in braces, that prefer it over the other") + "\n" +
			Markdown.unorderedList((Object[]) experimentResult.popularityMatrix().asSet().toArray(Object[]::new)) + "\n";

		return doc;
	}

	private void embed(JFreeChart chart)
	{
		try {
			var data = ChartUtils.encodeAsPNG(chart.createBufferedImage(1000,800));
			var imgData = "data:image/png;base64," + new String(Base64.encodeBase64(data));
			
			doc.append(Markdown.image(imgData)).append("\n\n");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
