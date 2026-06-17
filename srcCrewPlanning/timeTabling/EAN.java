package timeTabling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;

public class EAN extends DirectedGraph<Event, Activity>
{
	public EAN()
	{
		super();
	}

	/**
	 * Method for finding a cycle that is generated if an activity is added to a
	 * tree
	 */
	public List<Activity> getCycle(Activity a)
	{
		List<Activity> cycle = new ArrayList<>();
		cycle.add(a);
		List<Event> evInPath = new ArrayList<>();
		evInPath.add(a.getTo());
		// add the path from e2 to e1 using the tree
		List<DirectedGraphArc<Event, Activity>> path = getPath(a.getFrom(), a.getTo(), new ArrayList<>());
		for (DirectedGraphArc<Event, Activity> arc : path)
		{
			cycle.add(arc.getData());
		}
		return cycle;
	}

	/**
	 * Return the path between two nodes in a tree
	 */
	private List<DirectedGraphArc<Event, Activity>> getPath(Event to, Event from,
			List<DirectedGraphArc<Event, Activity>> curPath)
	{
		// System.out.println("\n Finding path to "+to+" from "+from);
		// System.out.println("Curpath: "+curPath);
		Set<DirectedGraphArc<Event, Activity>> candidates = new HashSet<>(); // all arcs that can be added to the path
																				// in a given iteration
		candidates.addAll(getInArcs(from));
		candidates.addAll(getOutArcs(from));
		candidates.removeAll(curPath); // cannot go back

		for (DirectedGraphArc<Event, Activity> a : candidates)
		{
			Event newNode = a.getFrom();
			if (a.getFrom().equals(from))
			{
				newNode = a.getTo();
			}
			List<DirectedGraphArc<Event, Activity>> newPath = new ArrayList<>(curPath);
			newPath.add(a);
			if (newNode.equals(to))
			{
				return newPath;
			}
			else
			{
				// continue recursion
				List<DirectedGraphArc<Event, Activity>> completePath = getPath(to, newNode, newPath);
				if (completePath != null)
				{
					return completePath;
				}
			}
		}

		return null;
	}

	/**
	 * Kruskal's
	 * 
	 * @return
	 */
	public EAN getMST()
	{
		EAN mst = new EAN();
		List<DirectedGraphArc<Event, Activity>> candidates = new ArrayList<>(arcs);
		candidates.sort(new SpanComparator());

		Map<Event, Set<Event>> eventToTree = new HashMap<>();
		for (Event e : nodes)
		{
			Set<Event> tree_e = new HashSet<>();
			tree_e.add(e);
			eventToTree.put(e, tree_e);
			mst.addNode(e);
		}

		for (DirectedGraphArc<Event, Activity> a : candidates)
		{
			Set<Event> treeFrom = eventToTree.get(a.getFrom());
			Set<Event> treeTo = eventToTree.get(a.getTo());
			if (!treeFrom.equals(treeTo))
			{
				mst.addArc(a.getFrom(), a.getTo(), a.getData(), a.getCost());
				Set<Event> treeCombined = new HashSet<>();
				treeCombined.addAll(treeFrom);
				treeCombined.addAll(treeTo);
				for (Event inTree : treeCombined)
				{
					eventToTree.put(inTree, treeCombined);
				}
			}
		}
		return mst;
	}

	/**
	 * Method for computing a MST. Assuming connectedness.
	 * 
	 * @return
	 */
	public EAN getMST_old()
	{
		EAN mst = new EAN();
		Set<Event> remainingNodes = new HashSet<>();
		for (Event e : nodes)
		{
			remainingNodes.add(e);
		}

		Event startNode = nodes.iterator().next();
		Set<DirectedGraphArc<Event, Activity>> candidates = new HashSet<>(); // all arcs that can be added in a given
																				// iteration
		candidates.addAll(getInArcs(startNode));
		candidates.addAll(getOutArcs(startNode));
		mst.addNode(startNode);
		remainingNodes.remove(startNode);

		while (!remainingNodes.isEmpty())
		{
			// find the cheapest candidate
			int minSpan = Integer.MAX_VALUE;
			DirectedGraphArc<Event, Activity> cheapest = null;
			for (DirectedGraphArc<Event, Activity> a : candidates)
			{
				if (a.getData().getSpan() < minSpan)
				{
					minSpan = a.getData().getSpan();
					cheapest = a;
				}
			}
			// check whether the head or tail of the cheapest arc is new
			Event newNode = cheapest.getFrom();
			if (remainingNodes.contains(cheapest.getTo()))
			{
				newNode = cheapest.getTo();
			}
			mst.addNode(newNode);
			remainingNodes.remove(newNode);
			candidates.addAll(getInArcs(newNode));
			candidates.addAll(getOutArcs(newNode));
			// add the cheapest arc to the MST and update candidates
			mst.addArc(cheapest.getFrom(), cheapest.getTo(), cheapest.getData(), cheapest.getCost());
			Set<DirectedGraphArc<Event, Activity>> toRemove = new HashSet<>();
			for (DirectedGraphArc<Event, Activity> a : candidates)
			{
				if (!remainingNodes.contains(a.getFrom()) && !remainingNodes.contains(a.getTo()))
				{
					toRemove.add(a);
				}
			}
			candidates.removeAll(toRemove);
		}
		return mst;
	}
}
