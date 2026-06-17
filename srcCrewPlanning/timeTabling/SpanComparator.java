package timeTabling;

import java.util.Comparator;

import graph.structures.digraph.DirectedGraphArc;

public class SpanComparator implements Comparator<DirectedGraphArc<Event, Activity>>
{
	@Override
	public int compare(DirectedGraphArc<Event, Activity> o1, DirectedGraphArc<Event, Activity> o2)
	{
		return o1.getData().getSpan() - o2.getData().getSpan();
	}
}
