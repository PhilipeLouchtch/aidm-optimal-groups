package nl.tudelft.aidm.optimalgroups.metric.profile;

import java.io.PrintStream;
import java.util.Map;

/**
 * The profile curve is a histogram of amount of Students/Groups (y-axis) being assigned the project that they ranked at x-th place
 *
 *         | X  <-- # agents with assignment they rate x ('rank')
 *         | X
 *         | X   X
 *         | X X X     X
 *         | X X X X   X
 *         | X X X X X X
 *         | -----------------
 * Rank -> | 1 2 3 4 5 6
 */
public abstract class AbstractRankProfile
{
    // Mapping of Rank -> |Agents with Rank|
    protected Map<Integer, Integer> profile = null;

    protected int worstRank = 1;


    public Map<Integer, Integer> asMap() {
        if (this.profile == null) {
            this.calculate();
            this.fillEmptyProfileValues();
        }
        return this.profile;
    }

    public int sumOfRanks() {
        int result = 0;
        for (Map.Entry<Integer, Integer> entry : this.asMap().entrySet()) {
            int rank = entry.getKey();
            int quantity = entry.getValue();

            result += (rank * quantity);
        }

        return result;
    }

    abstract protected void calculate();
    
    abstract public void printResult(PrintStream printStream);

    // Make sure all values are filled
    private void fillEmptyProfileValues() {
        for (int i = 1; i <= worstRank; i++) {
            if (!this.profile.containsKey(i)) {
                this.profile.put(i, 0);
            }
        }
    }

}
