package modelling.comparators;

import java.util.Comparator;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import scheduling.ArcTime;

public class ConnectionBreakStartTimeComparator implements Comparator<DirectedGraphArc<Task, Connection>>
{
	@Override
	public int compare(DirectedGraphArc<Task, Connection> a, DirectedGraphArc<Task, Connection> b)
	{
		// Note: We assume that both connections contain a break.
		int timeToBreakA = ArcTime.getStartTime(a) + a.getData().getTimeBeforeBreak();
		int timeToBreakB = ArcTime.getStartTime(b) + b.getData().getTimeBeforeBreak();
		return timeToBreakA - timeToBreakB;
	}
}
