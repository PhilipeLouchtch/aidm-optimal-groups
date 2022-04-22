package nl.tudelft.aidm.optimalgroups.model.matching;

import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ListBasedMatching<F, T> implements Matching<F, T>
{
	private final DatasetContext datasetContext;
	private List<Match<F, T>> backingList;
	
	/**
	 * Constructs an empty Matching
	 * @param datasetContext The dataset context for which this matching belongs to (partially)
	 */
	public ListBasedMatching(DatasetContext datasetContext)
	{
		this(datasetContext, new ArrayList<>());
	}

	protected ListBasedMatching(DatasetContext datasetContext, List<Match<F,T>> backingList)
	{
		this.datasetContext = datasetContext;
		this.backingList = backingList;
	}

	// Todo: make immutable
	public void add(Match<F,T> match)
	{
		this.backingList.add(match);
	}

	@Override
	public List<Match<F, T>> asList()
	{
		return Collections.unmodifiableList(backingList);
	}

	@Override
	public DatasetContext datasetContext()
	{
		return datasetContext;
	}
}
