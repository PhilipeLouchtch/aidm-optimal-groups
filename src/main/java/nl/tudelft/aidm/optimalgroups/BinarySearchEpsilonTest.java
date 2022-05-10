package nl.tudelft.aidm.optimalgroups;

import plouchtch.assertion.Assert;

public class BinarySearchEpsilonTest
{
	// Note! I am not looking for an epsilon value per se
	// I'm mere using binary search technique to narrow in on the highest epsilon
	// for the constraint. The actual value is of no importance, I only wish to solve
	// a model for the highest possible epsilon value without needing to iterate over
	// the domain of epsilon - as performant as possible

	public static class EpsilonSearch
	{
		private int lb;
		private int ub;
		
		private int current;
		
		public EpsilonSearch(int maxValue)
		{
			this.lb = 0;
			this.ub = maxValue;
			
			this.current = calc(lb, ub);
		}
		
		public int initial()
		{
			return current;
		}
		
		public int next(boolean currentIsFeasible)
		{
			if (currentIsFeasible) {
				// Epsilon is feasible, can we go higher?
				lb = current+1;
			} else {
				// Epsilon not feasible, have to try smaller
				ub = current-1;
			}
			
			current = calc(lb, ub);
			return current;
		}
		
		private static int calc(int lb, int ub)
		{
			return (ub + lb) >>> 1;
		}
	}
	
	public static void test(int numPregroups, int maxPregroupsFeasible)
	{
		boolean[] epsilonFeasible = new boolean[numPregroups+1];
		for (int i = 0; i <= maxPregroupsFeasible; i++) epsilonFeasible[i] = true;
		
		System.out.printf("Exp - #grps[%s] - #feas[%s]%n", numPregroups, maxPregroupsFeasible);
		
		final var epsilonSearch = new EpsilonSearch(numPregroups);
		
		var epsilon = epsilonSearch.initial();

		System.out.print("  trying: ");
		while(true) {
			
			var isFeasible = epsilonFeasible[epsilon];
			System.out.printf("%s[%s], ", epsilon, isFeasible ? 'y' : 'n');
			
			var nextEpsilon = epsilonSearch.next(isFeasible);
			
			if (nextEpsilon == epsilon)
				break;
			else
				epsilon = nextEpsilon;
		}
		
		System.out.printf("  FOUND: %s%n", epsilon);
		Assert.that(epsilon == maxPregroupsFeasible).orThrowMessage("Wasn't the right answer, try fixing the algo");
	}
	
	
	public static void main(String[] args)
	{
		for (int numPregroups = 1; numPregroups <= 50; numPregroups++)
		{
			for (int maxPregroupsFeasible = 0; maxPregroupsFeasible <= numPregroups; maxPregroupsFeasible++)
			{
				test(numPregroups, maxPregroupsFeasible);
			}
		}
	}
	
	
}
