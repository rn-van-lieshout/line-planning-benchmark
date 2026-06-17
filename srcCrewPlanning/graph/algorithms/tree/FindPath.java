package graph.algorithms.tree;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.DirectedGraphNodeIndex;

public class FindPath<V extends DirectedGraphNodeIndex, A>
{
	public List<DirectedGraphArc<V, A>> getPath(DirectedGraph<V, A> tree, V to, V from,
			List<DirectedGraphArc<V, A>> curPath)
	{
		// Collect possible arcs to add to path.
		Set<DirectedGraphArc<V, A>> candidates = new LinkedHashSet<>();
		candidates.addAll(tree.getInArcs(from));
		candidates.addAll(tree.getOutArcs(from));

		// We cannot use arcs already on the path.
		candidates.removeAll(curPath);

		// Apply DFS.
		for (DirectedGraphArc<V, A> a : candidates)
		{
			V newNode = a.getFrom();
			if (a.getFrom().equals(from))
			{
				newNode = a.getTo();
			}
			List<DirectedGraphArc<V, A>> newPath = new ArrayList<>(curPath);
			newPath.add(a);
			if (newNode.equals(to))
			{
				return newPath;
			}
			else
			{
				// Continue recursion.
				List<DirectedGraphArc<V, A>> completePath = getPath(tree, to, newNode, newPath);
				if (completePath != null)
				{
					return completePath;
				}
			}
		}
		return null;
	}
}
