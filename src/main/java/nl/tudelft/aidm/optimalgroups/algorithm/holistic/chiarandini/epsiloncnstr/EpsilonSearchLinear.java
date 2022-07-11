package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.epsiloncnstr;

public class EpsilonSearchLinear implements EpsilonSearch
	{
		private int current;
		
		public EpsilonSearchLinear(int maxValue)
		{
			this.current = maxValue;
		}
		
		@Override
		public int initial()
		{
			return current;
		}
		
		@Override
		public int next(boolean currentIsFeasible)
		{
			if (currentIsFeasible) {
				// We're trying epsilons from max to 0, the very first value
				// that is feasible is the winner
				return current;
			} else {
				return Math.max(--current, 0);
			}
		}
	}