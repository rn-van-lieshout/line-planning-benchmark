package graph.algorithms.tree;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.DirectedGraphNodeIndex;

public class Kruskal<V extends DirectedGraphNodeIndex, A>
{
	public DirectedGraph<V, A> minimumSpanningForest(DirectedGraph<V, A> graph)
	{
		// Initialise a forest.
		DirectedGraph<V, A> forest = new DirectedGraph<>();

		// Initialise a set containing each node.
		Map<V, Set<V>> setMap = new LinkedHashMap<>();
		for (V node : graph.getNodes())
		{
			Set<V> set = new LinkedHashSet<>();
			set.add(node);
			setMap.put(node, set);
			forest.addNode(node);
		}

		// Apply Kruskal's algorithm.
		for (DirectedGraphArc<V, A> arc : graph.getArcs())
		{
			V from = arc.getFrom();
			V to = arc.getTo();
			if (!setMap.get(from).equals(setMap.get(to)))
			{
				// Add arc to forest.
				forest.addArc(from, to, arc.getData(), arc.getCost());

				// Merge sets.
				Set<V> mergedSet = setMap.get(from);
				mergedSet.addAll(setMap.get(to));
				for (V node : mergedSet)
				{
					setMap.put(node, mergedSet);
				}
			}
		}

		// Return forest.
		return forest;
	}
}
