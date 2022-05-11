package nl.tudelft.aidm.optimalgroups.algorithm;

import nl.tudelft.aidm.optimalgroups.Algorithm;
import nl.tudelft.aidm.optimalgroups.Application;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.BepSysImprovedGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.BepSysReworked;
import nl.tudelft.aidm.optimalgroups.algorithm.group.CombinedPreferencesGreedy;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.HumbleMiniMaxWithClosuresSearch;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.*;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.ObjectiveFunction;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.ilppp.ILPPPDeterminedMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.WorstAmongBestHumblePairingsSearch;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.solver.minizinc.GroupedProjectMinizincAllocation;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc.SDPCOrderedByPotentialGroupmates;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc.SDPC;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.spdc.SDPCPessimism;
import nl.tudelft.aidm.optimalgroups.algorithm.project.GroupProjectMaxFlow;
import nl.tudelft.aidm.optimalgroups.algorithm.project.RandomizedSerialDictatorship;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.FormedGroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProjectPreference;

import java.util.Objects;

public interface GroupProjectAlgorithm extends Algorithm
{
	GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext);

	class Result implements Algorithm.Result<GroupProjectAlgorithm, GroupToProjectMatching<Group.FormedGroup>>
	{
		private final GroupProjectAlgorithm algo;
		private final GroupToProjectMatching<Group.FormedGroup> result;

		public Result(GroupProjectAlgorithm algo, GroupToProjectMatching<Group.FormedGroup> result)
		{
			this.algo = algo;
			this.result = result;
		}

		@Override
		public Algorithm algo()
		{
			return algo;
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> producedMatching()
		{
			return result;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof Result)) return false;
			Result result1 = (Result) o;
			return algo.equals(result1.algo) &&
				result.equals(result1.result);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(algo, result);
		}
	}

	class BepSys implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			// TODO include Pref agg method
			return String.format("BepSys (OG-ish) - %s", Application.preferenceAggregatingMethod);
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var groups = new BepSysImprovedGroups(datasetContext.allAgents(), datasetContext.groupSizeConstraint(), true);
			var groupsToProjects = new GroupProjectMaxFlow(datasetContext, groups.asFormedGroups(), datasetContext.allProjects());

			return groupsToProjects;
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class BepSys_reworked implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return String.format("BepSys (reworked) - %s", Application.preferenceAggregatingMethod);
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var groups = new BepSysReworked(datasetContext.allAgents(), datasetContext.groupSizeConstraint());
			var groupsToProjects = new GroupProjectMaxFlow(datasetContext, groups.asFormedGroups(), datasetContext.allProjects());

			return groupsToProjects;
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class BepSys_ogGroups_minimizeIndividualDisutility implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			// TODO include Pref agg method
			return "BepSys OG Groups (min indiv disutil)";
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var groups = new BepSysImprovedGroups(datasetContext.allAgents(), datasetContext.groupSizeConstraint(), true);

			var groupsToProjects = new GroupProjectMaxFlow(datasetContext, groups.asFormedGroups(), datasetContext.allProjects(),

				// Cost assignment function: the max rank between the individuals within that group
				(projectPreference, theProject) -> {
					var aggPref = ((AggregatedProjectPreference) projectPreference);
					return aggPref.agentsAggregatedFrom().asCollection().stream()
						.map(Agent::projectPreference)
						.mapToInt(pp -> {
							var rank = pp.rankOf(theProject);
							if (rank.unacceptable()) return Integer.MAX_VALUE;
							if (rank.isCompletelyIndifferent()) return 0;
							return rank.asInt();
						})
						.max().orElseThrow();
				});

			return groupsToProjects;
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class BepSys_reworkedGroups_minimizeIndividualDisutility implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			// TODO include Pref agg method
			return "BepSys Reworked Groups (min indiv disutil)";
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var groups = new BepSysReworked(datasetContext.allAgents(), datasetContext.groupSizeConstraint());

			var groupsToProjects = new GroupProjectMaxFlow(datasetContext, groups.asFormedGroups(), datasetContext.allProjects(),

				// Cost assignment function: the max rank between the individuals within that group
				(projectPreference, theProject) -> {
					var aggPref = ((AggregatedProjectPreference) projectPreference);
					return aggPref.agentsAggregatedFrom().asCollection().stream()
						.map(Agent::projectPreference)
//						.filter(Predicate.not(ProjectPreference::isCompletelyIndifferent))
						.mapToInt(pp -> {
							var rank = pp.rankOf(theProject);
							if (rank.unacceptable()) return Integer.MAX_VALUE;
							if (rank.isCompletelyIndifferent()) return 0;
							return rank.asInt();
						})
						.max().orElseThrow(() -> {
							System.out.printf("henkq23234");
							return new RuntimeException();
						});
				});

			return groupsToProjects;
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class CombinedPrefs implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return "Peer and Topic preferences merging";
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var formedGroups = new CombinedPreferencesGreedy(datasetContext).asFormedGroups();
			var matching = new GroupProjectMaxFlow(datasetContext, formedGroups, datasetContext.allProjects());

			return matching;
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class ILPPP implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return "ILPPP";
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			return new ILPPPDeterminedMatching(datasetContext);
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class BEPSys_RSD implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return "BepSys groups -> Randomised SD";
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var formedGroups = new BepSysImprovedGroups(datasetContext.allAgents(), datasetContext.groupSizeConstraint(), true).asFormedGroups();
			var matching = new RandomizedSerialDictatorship(datasetContext, formedGroups, datasetContext.allProjects());

			return matching;
		}

		@Override
		public String toString()
		{
			return name();
		}
	}

	class Pessimism implements GroupProjectAlgorithm
	{
		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			WorstAmongBestHumblePairingsSearch p = new WorstAmongBestHumblePairingsSearch(datasetContext.allAgents(), datasetContext.allProjects(), datasetContext.groupSizeConstraint());
			var agentsToProjects = p.matching();

			return FormedGroupToProjectMatching.byTriviallyPartitioning(agentsToProjects);
		}

		@Override
		public String name()
		{
			return "MinMax BnB 'Pessimism'";
		}
	}

	class SDPCWithSlots implements GroupProjectAlgorithm
	{

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var sdpc = new SDPC(datasetContext.allAgents(), datasetContext.allProjects(), datasetContext.groupSizeConstraint());
			var matchingStudentsToProjects = sdpc.doIt();

			return FormedGroupToProjectMatching.byTriviallyPartitioning(matchingStudentsToProjects);
		}

		@Override
		public String name()
		{
			return "SDPC-S (project slots)";
		}
	}

	class SDPCWithSlots_potential_numgroupmates_ordered implements GroupProjectAlgorithm
	{

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var sdpc = new SDPCOrderedByPotentialGroupmates(datasetContext.allAgents(), datasetContext.allProjects(), datasetContext.groupSizeConstraint());
			var matchingStudentsToProjects = sdpc.doIt();

			return FormedGroupToProjectMatching.byTriviallyPartitioning(matchingStudentsToProjects);
		}

		@Override
		public String name()
		{
			return "SDPC-S (ordered by num potential groupmates) ";
		}
	}

	class Greedy_SDPC_Pessimism_inspired implements GroupProjectAlgorithm
	{
		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var sdpc = new SDPCPessimism(datasetContext.allAgents(), datasetContext.allProjects(), datasetContext.groupSizeConstraint());
			var matchingStudentsToProjects = sdpc.matching();

			return FormedGroupToProjectMatching.byTriviallyPartitioning(matchingStudentsToProjects);
		}

		@Override
		public String name()
		{
			return "Greedy (SDPC and Pessimism inspired)";
		}
	}

	class BB_SDPC implements GroupProjectAlgorithm
	{
		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new HumbleMiniMaxWithClosuresSearch(datasetContext.allAgents(), datasetContext.allProjects(), datasetContext.groupSizeConstraint());
			var matchingStudentsToProjects = algo.matching();

			return FormedGroupToProjectMatching.byTriviallyPartitioning(matchingStudentsToProjects);
		}

		@Override
		public String name()
		{
			return "Branch-n-Bound with closures (BB over SDPC)";
		}
	}

	class MinizincMIP implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return "MiniZinc - MinRankSum";
		}
		
		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var matching = new GroupedProjectMinizincAllocation(datasetContext).matching();

			return FormedGroupToProjectMatching.byTriviallyPartitioning(matching);
		}
	}

	class Chiarandini_Utilitarian_MinSum_IdentityScheme implements GroupProjectAlgorithm
	{
		private final PregroupingType pregroupingType;
		
		public Chiarandini_Utilitarian_MinSum_IdentityScheme(PregroupingType pregroupingType)
		{
			this.pregroupingType = pregroupingType;
		}
		
		@Override
		public String name()
		{
			return "Chiaranini Utilitarian MinSum - Identity Weights - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new Chiarandini_MinSumRank(datasetContext, pregroupingType);
			var matching = algo.doIt();

			return matching;
		}
	}

	class Chiarandini_Utilitarian_MinSum_ExpScheme implements GroupProjectAlgorithm
	{
		private final PregroupingType pregroupingType;
		
		public Chiarandini_Utilitarian_MinSum_ExpScheme(PregroupingType pregroupingType)
		{
			this.pregroupingType = pregroupingType;
		}
		
		@Override
		public String name()
		{
			return "Chiaranini Utilitarian MinSum - Exp Weights - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new Chiarandini_MinSumExpRank(datasetContext, pregroupingType);
			var matching = algo.doIt();

			return matching;
		}
	}

	class Chiarandini_Stable_Utilitarian_MinSum_IdentityScheme implements GroupProjectAlgorithm
	{
		private final PregroupingType pregroupingType;
		
		public Chiarandini_Stable_Utilitarian_MinSum_IdentityScheme(PregroupingType pregroupingType)
		{
			this.pregroupingType = pregroupingType;
		}
		
		@Override
		public String name()
		{
			return "Chiaranini Stable Utilitarian MinSum - Identity Weights - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new Chiarandini_Stable_MinSumRank(datasetContext, pregroupingType);
			var matching = algo.doIt();

			return matching;
		}
	}

	class Chiarandini_Stable_Utilitarian_MinSum_ExpScheme implements GroupProjectAlgorithm
	{
		private final PregroupingType pregroupingType;
		
		public Chiarandini_Stable_Utilitarian_MinSum_ExpScheme(PregroupingType pregroupingType)
		{
			this.pregroupingType = pregroupingType;
		}
		
		@Override
		public String name()
		{
			return "Chiaranini Stable Utilitarian MinSum - Exp Weights - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new Chiarandini_Stable_MinSumExpRank(datasetContext, pregroupingType);
			var matching = algo.doIt();

			return matching;
		}
	}

	class Chiarandini_MiniMax_OWA implements GroupProjectAlgorithm
	{
		private final PregroupingType pregroupingType;
		
		public Chiarandini_MiniMax_OWA(PregroupingType pregroupingType)
		{
			this.pregroupingType = pregroupingType;
		}
		
		@Override
		public String name()
		{
			return "Chiaranini MiniMax-OWA - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new Chiarandini_MinimaxOWA(datasetContext, pregroupingType);
			var matching = algo.doIt();

			return matching;
		}
	}

	class Chiaranini_Stable_MiniMax_OWA implements GroupProjectAlgorithm
	{
		private final PregroupingType pregroupingType;
		
		public Chiaranini_Stable_MiniMax_OWA(PregroupingType pregroupingType)
		{
			this.pregroupingType = pregroupingType;
		}
		
		@Override
		public String name()
		{
			return "Chiaranini Stable MiniMax-OWA - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new Chiarandini_Stable_MinimaxDistribOWA(datasetContext, pregroupingType);
			var matching = algo.doIt();

			return matching;
		}
	}
	
	class Chiarandini_Fairgroups implements GroupProjectAlgorithm
	{
		private final ObjectiveFunction objectiveFunction;
		private final PregroupingType pregroupingType;
		
		public Chiarandini_Fairgroups(ObjectiveFunction objectiveFunction, PregroupingType pregroupingType)
		{
			this.objectiveFunction = objectiveFunction;
			this.pregroupingType = pregroupingType;
		}

		@Override
		public String name()
		{
			return "Chiarandini w Fair pregrouping " + objectiveFunction.name() + " - " + pregroupingType.simpleName();
		}

		@Override
		public GroupToProjectMatching<Group.FormedGroup> determineMatching(DatasetContext datasetContext)
		{
			var algo = new MILP_Mechanism_FairPregrouping(datasetContext, objectiveFunction, pregroupingType);
			var matching = algo.doIt();
			
			return matching;
		}
	}
}
