package graph.algorithms.tree;

import java.util.LinkedHashSet;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.DirectedGraphNodeIndex;

public class Prim<V extends DirectedGraphNodeIndex, A>
{
	public DirectedGraph<V, A> minimumSpanningTree(DirectedGraph<V, A> graph)
	{
		// Initialise empty tree.
		DirectedGraph<V, A> tree = new DirectedGraph<>();

		// Initialise set of nodes not in the tree.
		Set<V> remainingNodes = new LinkedHashSet<>();
		for (V node : graph.getNodes())
		{
			remainingNodes.add(node);
		}

		// Iteratively add the cheapest edge.
		while (remainingNodes.size() > 0)
		{
			// If tree is empty, add arbitrary node.
			if (tree.getNumberOfNodes() == 0)
			{
				V nextNode = null;
				for (V node : remainingNodes)
				{
					nextNode = node;
					break;
				}
				tree.addNode(nextNode);
				remainingNodes.remove(nextNode);
			}
			else
			{
				// Identify the cheapest edge from a node in the tree to a node not in the tree.
				double minCost = Double.MAX_VALUE;
				DirectedGraphArc<V, A> minCostArc = null;
				for (DirectedGraphArc<V, A> arc : graph.getArcs())
				{
					if (arc.getCost() > minCost)
					{
						continue;
					}
					if (remainingNodes.contains(arc.getFrom()) && remainingNodes.contains(arc.getTo()))
					{
						continue;
					}
					if (tree.getNodes().contains(arc.getFrom()) && tree.getNodes().contains(arc.getTo()))
					{
						continue;
					}
					minCostArc = arc;
					minCost = arc.getCost();
				}

				// Add the arc and node to the tree.
				V nextNode = remainingNodes.contains(minCostArc.getFrom()) ? minCostArc.getFrom() : minCostArc.getTo();
				tree.addNode(nextNode);
				tree.addArc(minCostArc.getFrom(), minCostArc.getTo(), minCostArc.getData(), minCost);
				remainingNodes.remove(nextNode);
			}
		}

		// Return tree.
		return tree;
	}
}
