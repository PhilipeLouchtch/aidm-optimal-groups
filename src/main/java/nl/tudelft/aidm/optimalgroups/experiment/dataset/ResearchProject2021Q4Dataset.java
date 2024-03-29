package nl.tudelft.aidm.optimalgroups.experiment.dataset;

import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.FilteredAgentsCourseEdition;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.matchfix.MatchFixes;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import plouchtch.assertion.Assert;

public class ResearchProject2021Q4Dataset extends FilteredAgentsCourseEdition
{
	private static final int courseEditionId = 39;
	
	private final String stringIdentifier;
	
	/**
	 * Creates a processed instance of the RP 2021 Q4 dataset
	 * @return The filtered dataset
	 */
	public static ResearchProject2021Q4Dataset getInstance()
	{
		var ce = CourseEditionFromDb.fromLocalBepSysDbSnapshot(courseEditionId);
		
		return new ResearchProject2021Q4Dataset(ce);
	}
	
	/**
	 * Private constructor, simply passes the params to the ManualDatasetContext constructor which then simply sets the fields
	 */
	private ResearchProject2021Q4Dataset(CourseEdition courseEdition)
	{
		super(courseEdition, RP2021Q4Aux::agentsFilter);
		
		this.stringIdentifier = courseEdition.identifier() + "_processed";
	}
	
	@Override
	public String identifier()
	{
		return stringIdentifier;
	}
	
	@Override
	public String toString()
	{
		return stringIdentifier;
	}
	
	/**
	 * @return The matches that need to be "fixed" - a list of predetermined matching outcomes
	 */
	public MatchFixes matchesToFix()
	{
		return RP2021Q4Aux.matchesToFix(this);
	}
	
	/**
	 * @return the course edition id this dataset has in bepsys
	 */
	public int courseEditionId()
	{
		return courseEditionId;
	}
}
