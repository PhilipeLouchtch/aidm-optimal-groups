package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.epsiloncnstr;

public class EpsilonSearchBinary implements EpsilonSearch
	{
		private int lb;
		private int ub;
		
		private int current;
		
		public EpsilonSearchBinary(int maxValue)
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