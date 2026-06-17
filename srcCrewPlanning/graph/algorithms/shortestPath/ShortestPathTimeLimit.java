package graph.algorithms.shortestPath;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.activation.Activation;
import modelling.Connection;
import modelling.Task;
import scheduling.ArcTime;

/**
 * Class that solves a shortest path problem on a directed acyclic graph using
 * dynamic programming.
 * 
 * @author Bart van Rossum
 */
public class ShortestPathTimeLimit
{
	private final Task origin;
	private final Task destination;
	private final int earliestTime;
	private final int latestTime;

	private final int maxIndex;
	private double[] distances;
	private DirectedGraphArc<Task, Connection>[] arcs;
	private final boolean forwardDirection;

	/**
	 * Initialise distance and predecessor maps, and then computes the shortest path
	 * from origin to destination.
	 * 
	 * @param graph            directed acyclic graph which we assume to be
	 *                         topologically sorted
	 * @param origin           origin node
	 * @param destination      destination node
	 * @param forwardDirection whether we move in forward direction or not
	 */
	public ShortestPathTimeLimit(DirectedGraph<Task, Connection> graph, Task origin, Task destination,
			boolean forwardDirection, int earliestTime, int latestTime)
	{
		this.origin = origin;
		this.destination = destination;
		this.forwardDirection = forwardDirection;
		this.earliestTime = earliestTime;
		this.latestTime = latestTime;

		graph.setNodeIndices();
		this.maxIndex = Math.max(origin.getNodeIndex(), destination.getNodeIndex());
		initialiseArrays(graph);
		updateDistances(graph);
	}

	public double getDistance(Task target)
	{
		return distances[target.getNodeIndex()];
	}

	public boolean containsPath(Task target)
	{
		if (target.getNodeIndex() >= distances.length)
		{
			return false;
		}
		return distances[target.getNodeIndex()] < Double.MAX_VALUE;
	}

	public List<DirectedGraphArc<Task, Connection>> getPath(Task target)
	{
		List<DirectedGraphArc<Task, Connection>> path = new ArrayList<>();
		Task current = target;
		while (!current.equals(origin))
		{
			DirectedGraphArc<Task, Connection> arc = arcs[current.getNodeIndex()];
			path.add(arc);
			if (forwardDirection)
			{
				current = arc.getFrom();
			}
			else
			{
				current = arc.getTo();
			}
		}
		return path;
	}

	@SuppressWarnings("unchecked")
	private void initialiseArrays(DirectedGraph<Task, Connection> graph)
	{
		this.distances = new double[maxIndex + 1];
		this.arcs = (DirectedGraphArc<Task, Connection>[]) Array.newInstance(DirectedGraphArc.class, maxIndex + 1);

		for (int i = 0; i < maxIndex + 1; i++)
		{
			distances[i] = Double.MAX_VALUE;
		}
		distances[origin.getNodeIndex()] = 0;
	}

	private void updateDistances(DirectedGraph<Task, Connection> graph)
	{
		if (forwardDirection)
		{
			for (int i = origin.getNodeIndex(); i < destination.getNodeIndex(); i++)
			{
				// Assume that nodes are sorted based on increasing start times: we can exit the
				// loop when the start time exceeds the time limit.
				Task node = graph.getNodes().get(i);
				boolean isRegularNode = (i > 0 && i < graph.getNumberOfNodes() - 1);
				if (node.getStartTime() > latestTime && isRegularNode)
				{
					break;
				}
				if ((node.getStartTime() < earliestTime || node.getEndTime() > latestTime) && isRegularNode)
				{
					continue;
				}
				if (!Activation.getActivation().getActivationFunction().isActiveNode(node))
				{
					continue;
				}
				if (distances[node.getNodeIndex()] == Double.MAX_VALUE)
				{
					continue;
				}
				for (DirectedGraphArc<Task, Connection> arc : graph.getOutArcs(node))
				{
					if (ArcTime.getEndTime(arc) > latestTime)
					{
						continue;
					}
					if (ArcTime.getStartTime(arc) < earliestTime)
					{
						continue;
					}
					if (!Activation.getActivation().getActivationFunction().isActiveArc(arc))
					{
						continue;
					}
					processArc(node, arc.getTo(), arc);
				}
			}
		}
		else
		{
			for (int i = origin.getNodeIndex(); i > destination.getNodeIndex(); i--)
			{
				// Assume that nodes are sorted based on increasing start times: we can exit the
				// loop when the start time exceeds the time limit.
				Task node = graph.getNodes().get(i);
				boolean isRegularNode = (i > 0 && i < graph.getNumberOfNodes() - 1);
				if (node.getStartTime() < earliestTime && isRegularNode)
				{
					break;
				}
				if ((node.getStartTime() < earliestTime || node.getEndTime() > latestTime) && isRegularNode)
				{
					continue;
				}
				if (!Activation.getActivation().getActivationFunction().isActiveNode(node))
				{
					continue;
				}
				if (distances[node.getNodeIndex()] == Double.MAX_VALUE)
				{
					continue;
				}
				for (DirectedGraphArc<Task, Connection> arc : graph.getInArcs(node))
				{
					if (ArcTime.getStartTime(arc) < earliestTime || ArcTime.getEndTime(arc) > latestTime)
					{
						continue;
					}
					if (!Activation.getActivation().getActivationFunction().isActiveArc(arc))
					{
						continue;
					}
					processArc(node, arc.getFrom(), arc);
				}
			}
		}
	}

	private void processArc(Task from, Task to, DirectedGraphArc<Task, Connection> arc)
	{
		double distance = arc.getWeight();
		if (distances[from.getNodeIndex()] + distance < distances[to.getNodeIndex()])
		{
			distances[to.getNodeIndex()] = distances[from.getNodeIndex()] + distance;
			arcs[to.getNodeIndex()] = arc;
		}
	}
}
