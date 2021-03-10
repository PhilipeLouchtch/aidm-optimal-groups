package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.AssignmentConstraints;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ChiarandiniAgentToProjectMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.DistributiveWeightsObjective;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.StabilityConstraints;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.metric.matching.MatchingMetrics;
import nl.tudelft.aidm.optimalgroups.metric.rank.distribution.StudentRankDistributionInMatching;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.dataset.sequentual.SequentualDatasetContext;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

public class Chiarandini_Stable_MinimaxDistribOWA
{
	private final DatasetContext datasetContext;
	private final DistributiveWeightsObjective.WeightScheme weightScheme;

	public Chiarandini_Stable_MinimaxDistribOWA(DatasetContext datasetContext)
	{
		this.datasetContext = datasetContext;
		this.weightScheme = new Chiarandini_MinimaxDistribOWA.DistribOWAWeightScheme(datasetContext);
	}

	public nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching doIt()
	{
		return Try.getting(this::doItDirty)
			.or(Rethrow.asRuntime());
	}

	public nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching doItDirty() throws GRBException
	{
		var seqDatasetContext = SequentualDatasetContext.from(datasetContext);

		var env = new GRBEnv();
		env.start();

		var model = new GRBModel(env);

		AssignmentConstraints assignmentConstraints = AssignmentConstraints.createInModel(model, seqDatasetContext);
		DistributiveWeightsObjective.createInModel(model, seqDatasetContext, assignmentConstraints, weightScheme);

		var stability = StabilityConstraints.createInModel(model, assignmentConstraints, seqDatasetContext);
		stability.createStabilityFeasibilityConstraint(model);

		model.optimize();

		// extract x's and map to matching

		var matching = new ChiarandiniAgentToProjectMatching(assignmentConstraints.xVars, seqDatasetContext);

		env.dispose();

		return matching;
	}

	public static void main(String[] args) throws Exception
	{
		CourseEdition ce = CourseEdition.fromLocalBepSysDbSnapshot(10);


		var owaMinimaxChiarandini = new Chiarandini_MinimaxDistribOWA(ce);
		var resultOwa = owaMinimaxChiarandini.doIt();

		var metricsOwa = new MatchingMetrics.StudentProject(resultOwa);
		new StudentRankDistributionInMatching(resultOwa).displayChart("Chiarandini minimax-owa");


		var stableOwaMinimaxChiarandini = new Chiarandini_Stable_MinimaxDistribOWA(ce);
		var resultStableOwa = stableOwaMinimaxChiarandini.doIt();

		var metricsStableOwa = new MatchingMetrics.StudentProject(resultStableOwa);
		new StudentRankDistributionInMatching(resultStableOwa).displayChart("Chiarandini stable_minimax-owa");

		return;
	}

}
