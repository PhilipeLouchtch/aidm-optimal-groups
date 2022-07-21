package nl.tudelft.aidm.optimalgroups.experiment.viz;

import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.table.TableRow;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.metric.rank.SumOfRanks;
import nl.tudelft.aidm.optimalgroups.metric.rank.WorstAssignedRank;
import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.comparison.ParetoComperator;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a table comparing the results of two mechanisms, currently this is assumed to be the Fair mechanism
 * and the Chiarandini. The Table contains four columns, the dataset identifier, two columns with relative/absolute metrics
 * per mechanism and a last column containing the profile difference between the two results/mechanisms.
 *
 * For example:
 *
 * |Dataset | Fairness | Chiarandini | Profile                  |
 * --------------------------------------------------------------
 * |   CE11	| 478 (-39)|	517 (+39)|[+3 +7 -1 -1 -3 -1 -4 0 0]|
 */
public class FairVsChiaComparisonTable
{
	private final Collection<Result> results;
	
	public FairVsChiaComparisonTable(Collection<Result> results)
	{
		this.results = results;
	}
	
	public Table asMarkdownTable()
	{
		var rows = new ArrayList<List<String>>();
		
		var mechanismMain = "Fairness";
		var mechanismOther = "Chiarandini";
		
		var header = List.of("Dataset", mechanismMain, mechanismOther, "Profile delta (%s - %s)".formatted(mechanismMain, mechanismOther));
		rows.add(header);
		
		for (Result result : this.results)
		{
			var comparisonOutcome = new ComparisonOutcome(result.fairness(), mechanismMain, result.chiarandini(), mechanismOther);
			
			var datasetId = result.datasetContext().identifier();
			if (datasetId.startsWith("CourseEdition")) {
				datasetId = datasetId.replaceAll("^CourseEdition\\[(\\d+)].+$", "CE$1");
			}
			
			var row = List.of(
					datasetId,
					comparisonOutcome.fairnessOutcomeAsString(),
					comparisonOutcome.vanillaOutcomeAsString(),
					comparisonOutcome.profileDeltaFairnessVsVanilla()
			);
			
			rows.add(row);
		}
		
		return rows.stream()
				.map(row -> new TableRow(row))
				.collect(Collectors.collectingAndThen(Collectors.toList(), Table::new));
	}
	
	private record SingleResultForComparison(Matching<Agent, Project> matchingResult, String mechanismId, WorstAssignedRank worstRank, SumOfRanks sumOfRanks, Profile profile)
	{
		SingleResultForComparison(Matching<Agent, Project> matching, String mechanismId)
		{
			this(matching, mechanismId, WorstAssignedRank.ProjectToStudents.in(matching), SumOfRanks.of(matching), Profile.of(matching));
		}
	}
	
	static final class ComparisonOutcome
	{
		private final SingleResultForComparison fairness;
		private final SingleResultForComparison chiarandini;
		
		public ComparisonOutcome(Matching<Agent, Project> fairness, String mechanismNameOne, Matching<Agent, Project> chiarandini, String mechanismNameTwo)
		{
			this.fairness = new SingleResultForComparison(fairness, mechanismNameOne);
			this.chiarandini = new SingleResultForComparison(chiarandini, mechanismNameTwo);
		}
		
		public String profileDeltaFairnessVsVanilla()
		{
			var profileOfFairResult = fairness.profile();
			var profileOfChiarandiniResult = chiarandini.profile();
			var paretoOutcome = new ParetoComperator().compare(profileOfFairResult, profileOfChiarandiniResult);
			
			return switch (paretoOutcome) {
				case BETTER, WORSE, NONE -> profileOfFairResult.subtracted(profileOfChiarandiniResult)
				                                               .toString();
				case SAME -> Profile.empty().toString();
			};
		}
		
		public String fairnessOutcomeAsString()
		{
			return cellTextOutcome(fairness, chiarandini);
		}
		
		public String vanillaOutcomeAsString()
		{
			return cellTextOutcome(chiarandini, fairness);
		}
	
		public String cellTextOutcome(SingleResultForComparison outcomeOne, SingleResultForComparison outcomeTwo)
		{
			var profileThis = outcomeOne.profile();
			var profileThat = outcomeTwo.profile();
			
			var paretoOutcome = new ParetoComperator().compare(profileThis, profileThat);
			
			return switch (paretoOutcome) {
				case BETTER -> asStringIfParetoBetter(outcomeOne, outcomeTwo);
				case SAME -> asStringSameResult(outcomeOne, outcomeTwo);
				case WORSE -> asStringIfParetoWorse(outcomeOne, outcomeTwo);
				case NONE -> asStringIfParetoNone(outcomeOne, outcomeTwo);
			};
		}
		
		private boolean isWorstRankSame(SingleResultForComparison a, SingleResultForComparison b)
		{
			return a.worstRank().compareTo(b.worstRank()) == 0;
		}
		
		private String cellResult(SingleResultForComparison current, SingleResultForComparison other)
		{
			var sumOfRanksDelta = current.sumOfRanks.asInt() - other.sumOfRanks.asInt();
			
			return "%s (%s) [%s]".formatted(
					current.sumOfRanks().asInt(),
					sumOfRanksDelta,
					current.worstRank.asInt()
			);
		}
		
		private String asStringIfParetoBetter(SingleResultForComparison a, SingleResultForComparison b)
		{
			return bold(cellResult(a, b));
		}
		
		private String asStringIfParetoNone(SingleResultForComparison current, SingleResultForComparison other)
		{
			return cellResult(current, other);
		}
		
		private String asStringIfParetoWorse(SingleResultForComparison current, SingleResultForComparison other)
		{
			return cellResult(current, other);
		}
		
		private String asStringSameResult(SingleResultForComparison current, SingleResultForComparison other)
		{
			return cellResult(current, other);
		}
		
	}
	
	//////////////////////
	
	
	public record Result(
			Matching<Agent, Project> fairness,
			Matching<Agent, Project> chiarandini
	) {
		public Result
		{
			Assert.that(fairness.datasetContext().equals(chiarandini.datasetContext()))
					.orThrowMessage("Dataset mismatch");
		}
		
		public DatasetContext datasetContext()
		{
			return fairness.datasetContext();
		}
		
		public enum ParetoComparisonOutcome
		{
			FAIRNESS,
			CHIA,
			EQUAL,
			NONE
		}
		
		public ParetoComparisonOutcome paretoComparisonFairnessVsVanilla()
		{
			var fairnessProfile = Profile.of(fairness);
			var vanillaProfile = Profile.of(chiarandini);
			
			var outcome = new ParetoComperator().compare(fairnessProfile, vanillaProfile);
			
			return switch (outcome) {
				case BETTER -> ParetoComparisonOutcome.FAIRNESS;
				case WORSE -> ParetoComparisonOutcome.CHIA;
				case SAME -> ParetoComparisonOutcome.EQUAL;
				case NONE -> ParetoComparisonOutcome.NONE;
			};
		}
	}
	
	private static String bold(String text)
	{
		return "**" + text + "**";
	}
	
	private static String conditional(String textTrue, String textFalse, boolean condition)
	{
		if (condition)
			return textTrue;
		else
			return textFalse;
	}
}
