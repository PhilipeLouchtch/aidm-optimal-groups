package nl.tudelft.aidm.optimalgroups.algorithm;

import nl.tudelft.aidm.optimalgroups.Algorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.BepSysImprovedGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.group.CombinedPreferencesGreedy;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.ilppp.ILPPPDeterminedMatching;
import nl.tudelft.aidm.optimalgroups.algorithm.project.GroupProjectMaxFlow;
import nl.tudelft.aidm.optimalgroups.algorithm.project.RandomizedSerialDictatorship;
import nl.tudelft.aidm.optimalgroups.model.matching.GroupToProjectMatching;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.group.Group;

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
			return "BepSys";
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

	class RSD implements GroupProjectAlgorithm
	{
		@Override
		public String name()
		{
			return "BepSys groups into Randomised Serial Dictatorship (IA with Random lottery)";
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
}
