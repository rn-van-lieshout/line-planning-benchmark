package graph.algorithms.spprc.labelComparators;

import java.util.Comparator;

import graph.algorithms.spprc.Label;

public class LabelCostComparator<V, A> implements Comparator<Label<V, A>>
{
	@Override
	public int compare(Label<V, A> o1, Label<V, A> o2)
	{
		return Double.compare(o1.getCost(), o2.getCost());
	}
}