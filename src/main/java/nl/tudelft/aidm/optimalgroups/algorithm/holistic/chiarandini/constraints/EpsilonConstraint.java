package nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.constraints;

import gurobi.GRBException;
import gurobi.GRBModel;

public interface EpsilonConstraint extends Constraint
{
	/**
	 * Set the epsilon value. The default value is a maximal epsilon.
	 * Use this function to change the epsilon value. Note, do this
	 * BEFORE {@link Constraint#apply}-ing THE CONSTRAINT
	 * @param epsilon
	 */
	void setEpsilon(int epsilon);
}
