package graph.structures;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;

/**
 * Simple class that models a directed acyclic graph. We assume that this graph
 * has a sink and source node.
 * 
 * @author Bart van Rossum
 */
public class PricingGraph extends DirectedGraph<Task, Connection>
{
	public Task getSourceNode()
	{
		return nodes.get(0);
	}

	public Task getSinkNode()
	{
		return nodes.get(getNumberOfNodes() - 1);
	}

	public Set<Task> getStartNodes()
	{
		Set<Task> startNodes = new LinkedHashSet<>();
		for (DirectedGraphArc<Task, Connection> sourceArc : getOutArcs(getSourceNode()))
		{
			startNodes.add(sourceArc.getTo());
		}
		return startNodes;
	}

	public Set<Task> getEndNodes()
	{
		Set<Task> endNodes = new LinkedHashSet<>();
		for (DirectedGraphArc<Task, Connection> sinkArc : getInArcs(getSinkNode()))
		{
			endNodes.add(sinkArc.getFrom());
		}
		return endNodes;
	}

	public List<DirectedGraphArc<Task, Connection>> getBreakArcs()
	{
		List<DirectedGraphArc<Task, Connection>> breakArcs = new ArrayList<>();
		for (DirectedGraphArc<Task, Connection> arc : arcs)
		{
			if (arc.getData().hasBreak())
			{
				breakArcs.add(arc);
			}
		}
		return breakArcs;
	}

	public void resetWeights()
	{
		for (Task task : nodes)
		{
			for (DirectedGraphArc<Task, Connection> arc : getInArcs(task))
			{
				arc.setCost(arc.getData().getCost(), 0);
				arc.setCost(0, 1);

				arc.setDual(0, 0);
				arc.setDual(0, 1);
			}
		}
	}

	public void resetSecondWeights()
	{
		for (DirectedGraphArc<Task, Connection> arc : arcs)
		{
			arc.setCost(0, 1);
			arc.setDual(0, 1);
		}
	}

	/**
	 * Iteratively remove all nodes, and their arcs, that are not connected with
	 * both the source and sink. Note: this method assumes that the graph has been
	 * topologically sorted.
	 */
	public void cleanGraph()
	{
		boolean go = true;
		while (go)
		{
			go = false;
			for (int i = 1; i < getNumberOfNodes() - 1; i++)
			{
				if (getInDegree(nodes.get(i)) == 0 || getOutDegree(nodes.get(i)) == 0)
				{
					go = true;
					removeNode(nodes.get(i));
					break;
				}
			}
		}
	}

	public PricingGraph clone(boolean includeArcs)
	{
		PricingGraph clone = new PricingGraph();
		for (Task node : nodes)
		{
			clone.addNode(node);
		}
		if (includeArcs)
		{
			for (DirectedGraphArc<Task, Connection> arc : arcs)
			{
				clone.addArc(arc.getFrom(), arc.getTo(), arc.getData(), arc.getData().getCost());
			}
		}
		return clone;
	}
}
