package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

public enum PROJ_PRESSURE
{
	TIGHT(1), MID(1.5), LOOSE(2);
	
	public final double factor;
	
	PROJ_PRESSURE(double factor)
	{
		this.factor = factor;
	}
}
