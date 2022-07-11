package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.epsiloncnstr;

interface EpsilonSearch
{
	int initial();
	int next(boolean currentIsFeasible);
}