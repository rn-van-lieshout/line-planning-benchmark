package graph.algorithms.spprc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import graph.algorithms.shortestPath.ShortestPath;
import graph.algorithms.shortestPath.arcWeightFunctions.AbstractArcWeightFunction;
import graph.algorithms.shortestPath.arcWeightFunctions.DefaultArcWeightFunction;
import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.DirectedGraphNodeIndex;
import graph.structures.digraph.activation.Activation;
import optimisation.columnGeneration.PricingSettings;

public class MonodirectionalLabelling<V extends DirectedGraphNodeIndex, A>
{
	private AbstractArcWeightFunction<V, A> arcWeightFunction = new DefaultArcWeightFunction<>();
	private REF<V, A> resourceExtensionFunction;
	private final List<AbstractResourceCompletionTest<V, A>> completionTests;

	public MonodirectionalLabelling()
	{
		this.completionTests = new ArrayList<>();
	}
	
	public void setArcWeightFunction(AbstractArcWeightFunction<V, A> arcWeightFunction)
	{
		this.arcWeightFunction = arcWeightFunction;
	}

	public void addCompletionBound(AbstractResourceCompletionTest<V, A> completionTest)
	{
		completionTests.add(completionTest);
	}

	public List<List<DirectedGraphArc<V, A>>> applyLabelling(DirectedGraph<V, A> graph, V sourceNode, V sinkNode,
			REF<V, A> extensionFunction, double reducedCostThreshold, boolean applyDominance)
	{
		this.resourceExtensionFunction = extensionFunction;

		// Initialise a list of paths.
		List<List<DirectedGraphArc<V, A>>> paths = new ArrayList<>();

		// Solve a shortest path from sink to all other nodes, that can be used as
		// completion bound.
		ShortestPath<V, A> shortestPath = new ShortestPath<>(graph, sinkNode, sourceNode, false, arcWeightFunction);
		if (shortestPath.getDistance(sourceNode) > reducedCostThreshold)
		{
			return paths;
		}

		// Initialise queue with a single source node.
		Map<V, LinkedList<Label<V, A>>> queueMap = new LinkedHashMap<>();
		int nodeIndex = sourceNode.getNodeIndex();
		V currentNode = sourceNode;
		Label<V, A> sourceLabel = new Label<>(null, null, extensionFunction.getEmptyResourceVector(), 0,
				shortestPath.getDistance(sourceNode));
		addLabel(sourceLabel, sourceNode, queueMap, applyDominance);

		// Perform labelling.
		while (nodeIndex < sinkNode.getNodeIndex())
		{
			// Skip if no labels in the bucket.
			if (!queueMap.containsKey(currentNode))
			{
				nodeIndex++;
				currentNode = graph.getNodes().get(nodeIndex);
				continue;
			}
			// Expand all labels in the current bucket.
			for (Label<V, A> label : queueMap.get(currentNode))
			{
				// Expand label.
				arcLoop:
				for (DirectedGraphArc<V, A> arc : graph.getOutArcs(currentNode))
				{
					// Skip deactivated arcs.
					if (!Activation.getActivation().getActivationFunction().isActiveArc(arc))
					{
						continue;
					}

					// Skip arcs that lead to infeasible labels.
					int[] extendedResourceVector = extensionFunction.extendResourceVector(label, arc, true);
					if (!extensionFunction.isFeasible(extendedResourceVector))
					{
						continue;
					}

					// Expand label.
					double expandedLabelCost = label.getCost() + arcWeightFunction.getArcWeight(arc);
					double completionBound = expandedLabelCost + shortestPath.getDistance(arc.getTo());
					Label<V, A> expandedLabel = new Label<>(label, arc, extendedResourceVector, expandedLabelCost,
							completionBound);

					// If at sink, terminate.
					if (arc.getTo().equals(sinkNode))
					{
						if (!extensionFunction.isFeasibleAtSink(extendedResourceVector))
						{
							continue;
						}
						if (expandedLabel.getCost() > reducedCostThreshold)
						{
							continue;
						}

						// Use backtracking to construct a path and add to path list.
						paths.add(backtrackPath(expandedLabel));
					}
					else
					{
						// Completion bound on reduced cost.
						if (expandedLabel.getCompletionBound() > reducedCostThreshold)
						{
							continue;
						}

						// Completion bound based on resource.
						for (AbstractResourceCompletionTest<V, A> completionTest : completionTests)
						{
							if (!completionTest.isFeasible(expandedLabel, true))
							{
								continue arcLoop;
							}
						}

						// Add the label.
						addLabel(expandedLabel, arc.getTo(), queueMap, applyDominance);
					}
				}
			}
			// Update current node index.
			nodeIndex++;
			currentNode = graph.getNodes().get(nodeIndex);
		}
		return paths;
	}

	private List<DirectedGraphArc<V, A>> backtrackPath(Label<V, A> label)
	{
		List<DirectedGraphArc<V, A>> path = new ArrayList<>();
		Label<V, A> currentLabel = label;
		while (true)
		{
			path.add(0, currentLabel.getArc());
			currentLabel = currentLabel.getPreviousLabel();
			// if (currentLabel == null)
			if (currentLabel.getArc() == null)
			{
				break;
			}
		}
		return path;
	}

	private void addLabel(Label<V, A> label, V to, Map<V, LinkedList<Label<V, A>>> queueMap, boolean applyDominance)
	{
		// Initialise a new bucket for this node, if necessary.
		if (!queueMap.containsKey(to))
		{
			queueMap.put(to, new LinkedList<>());
		}
		LinkedList<Label<V, A>> queue = queueMap.get(to);

		// Only apply dominance checks if activated. This allows us to enumerate labels
		// too.
		if (!applyDominance)
		{
			queue.add(label);
			return;
		}

		// Dominance checks.
		double cost = label.getCost();
		boolean inserted = false;
		ListIterator<Label<V, A>> iterator = queue.listIterator();
		while (iterator.hasNext())
		{
			// Retrieve label.
			Label<V, A> otherLabel = iterator.next();
			double otherCost = otherLabel.getCost();

			// First check if a label is dominated by another label with lower or equal
			// reduced cost.
			if (cost >= otherCost)
			{
				// Perform dominance check.
				if (dominates(otherLabel, label))
				{
					return;
				}
			}

			// The label is not yet inserted and not dominated, so we add it to the bucket.
			if (cost < otherCost && !inserted)
			{
				inserted = true;
				iterator.previous();
				iterator.add(label);
				otherLabel = iterator.next();
			}

			// We check if the new label dominates any labels in the current bucket.
			if (cost <= otherCost)
			{
				// Perform dominance check.
				if (dominates(label, otherLabel))
				{
					iterator.remove();
				}
			}
		}

		// Add to the bucket if not yet added.
		if (!inserted)
		{
			queue.add(label);
		}
	}

	private boolean dominates(Label<V, A> label, Label<V, A> otherLabel)
	{
		if (PricingSettings.EXACT_PRICING)
		{
			return resourceExtensionFunction.dominates(label.getResourceVector(), otherLabel.getResourceVector());
		}
		return resourceExtensionFunction.dominatesHeuristically(label.getResourceVector(),
				otherLabel.getResourceVector());
	}
}