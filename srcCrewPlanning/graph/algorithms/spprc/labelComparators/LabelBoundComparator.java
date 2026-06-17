package graph.algorithms.spprc.labelComparators;

import java.util.Comparator;

import graph.algorithms.spprc.Label;

public class LabelBoundComparator<V, A> implements Comparator<Label<V, A>>
{
	@Override
	public int compare(Label<V, A> o1, Label<V, A> o2)
	{
		return Double.compare(o1.getCompletionBound(), o2.getCompletionBound());
	}
}