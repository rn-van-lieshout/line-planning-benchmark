package modelling.comparators;

import java.util.Comparator;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import scheduling.ArcTime;

public class ConnectionEndTimeComparator implements Comparator<DirectedGraphArc<Task, Connection>>
{
	@Override
	public int compare(DirectedGraphArc<Task, Connection> a, DirectedGraphArc<Task, Connection> b)
	{
		if (ArcTime.getEndTime(a) == ArcTime.getEndTime(b))
		{
			return a.getData().getIndex() - b.getData().getIndex();
		}
		return ArcTime.getEndTime(a) - ArcTime.getEndTime(b);
	}
}