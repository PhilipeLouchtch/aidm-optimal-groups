package nl.tudelft.aidm.optimalgroups.model.match;

import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListBasedMatching<F, T> implements Matching<F, T>
{
	private final DatasetContext datasetContext;
	private List<Match<F, T>> backingList;

	public ListBasedMatching(DatasetContext datasetContext)
	{
		this(datasetContext, new ArrayList<>());
	}

	ListBasedMatching(DatasetContext datasetContext, List<Match<F,T>> backingList)
	{
		this.datasetContext = datasetContext;
		this.backingList = backingList;
	}

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
