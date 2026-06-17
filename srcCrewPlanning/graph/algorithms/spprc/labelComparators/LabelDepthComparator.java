package graph.algorithms.spprc.labelComparators;

import java.util.Comparator;

import graph.algorithms.spprc.Label;

public class LabelDepthComparator<V, A> implements Comparator<Label<V, A>>
{
	@Override
	public int compare(Label<V, A> o1, Label<V, A> o2)
	{
		if (o1.getDepth() > o2.getDepth())
		{
			return -1;
		}
		if (o1.getDepth() < o2.getDepth())
		{
			return 1;
		}
		return Double.compare(o1.getCompletionBound(), o2.getCompletionBound());
	}
}