package modelling.comparators;

import java.util.Comparator;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;

public class ConnectionTransferTimeComparator implements Comparator<DirectedGraphArc<Task, Connection>>
{
	@Override
	public int compare(DirectedGraphArc<Task, Connection> a, DirectedGraphArc<Task, Connection> b)
	{
		int transferA = a.getData().getDuration() - a.getTo().getDuration() - a.getFrom().getDuration();
		int transferB = b.getData().getDuration() - b.getTo().getDuration() - b.getFrom().getDuration();
		if (transferA == transferB)
		{
			return a.getData().getIndex() - b.getData().getIndex();
		}
		return transferA - transferB;
	}
}