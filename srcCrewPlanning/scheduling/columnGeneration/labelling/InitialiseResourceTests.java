package scheduling.columnGeneration.labelling;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import graph.algorithms.shortestPath.ShortestPath;
import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.activation.Activation;
import graph.structures.digraph.activation.DefaultActivationFunction;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Station;
import modelling.Task;

public class InitialiseResourceTests
{
	public static LengthResourceTest initialiseLengthResourceTest(List<Station> crewBases,
			List<PricingGraph> pricingGraphs)
	{
		// Reset to default activation function.
		Activation.getActivation().setActivationFunction(new DefaultActivationFunction());

		// Initialise maps.
		Map<Integer, Map<Task, Integer>> forwardTimeCompletionMap = new LinkedHashMap<>();
		Map<Integer, Map<Task, Integer>> backwardTimeCompletionMap = new LinkedHashMap<>();

		// Iterate over crew bases and pricing graphs.
		for (Station crewBase : crewBases)
		{
			int depot = crewBase.getIndex();
			forwardTimeCompletionMap.put(depot, new LinkedHashMap<>());
			backwardTimeCompletionMap.put(depot, new LinkedHashMap<>());

			for (PricingGraph pricingGraph : pricingGraphs)
			{
				// Construct a network with duration as costs, only including source and sink
				// arcs to the current crew base.
				DirectedGraph<Task, Connection> graph = constructDurationNetwork(pricingGraph, depot);
				Task source = pricingGraph.getSourceNode();
				Task sink = pricingGraph.getSinkNode();

				// Compute the shortest path from each node to the sink.
				ShortestPath<Task, Connection> forwardShortestPath = new ShortestPath<>(graph, sink, source, false);
				ShortestPath<Task, Connection> backwardShortestPath = new ShortestPath<>(graph, source, sink, true);

				// Construct mapping of task to completion duration.
				for (Task task : graph.getNodes())
				{
					forwardTimeCompletionMap.get(depot).put(task, (int) forwardShortestPath.getDistance(task));
					backwardTimeCompletionMap.get(depot).put(task, (int) backwardShortestPath.getDistance(task));
				}
			}
		}

		// Return resource test.
		return new LengthResourceTest(forwardTimeCompletionMap, backwardTimeCompletionMap);
	}

	public static BreakResourceTest initialiseBreakResourceTest(List<PricingGraph> pricingGraphs,
			List<Station> crewBases)
	{
		// Reset to default activation function.
		Activation.getActivation().setActivationFunction(new DefaultActivationFunction());

		// Construct mapping of task to completion duration.
		Map<Integer, Map<Task, Integer>> forwardTimeCompletionMap = new LinkedHashMap<>();
		Map<Integer, Map<Task, Integer>> backwardTimeCompletionMap = new LinkedHashMap<>();
		Map<Task, Integer> forwardBreakCompletionMap = new LinkedHashMap<>();
		Map<Task, Integer> backwardBreakCompletionMap = new LinkedHashMap<>();

		// Iterate over crew bases and pricing graphs.
		for (Station crewBase : crewBases)
		{
			int depot = crewBase.getIndex();
			forwardTimeCompletionMap.put(depot, new LinkedHashMap<>());
			backwardTimeCompletionMap.put(depot, new LinkedHashMap<>());

			for (PricingGraph pricingGraph : pricingGraphs)
			{
				// Construct a network with duration as costs. Each break arc goes straight to
				// the sink.
				Task source = pricingGraph.getSourceNode();
				Task sink = pricingGraph.getSinkNode();

				// Compute the shortest path from each node to the nearest break.
				DirectedGraph<Task, Connection> graph = constructBreakNetwork(pricingGraph, source, sink, true);
				ShortestPath<Task, Connection> forwardShortestPath = new ShortestPath<>(graph, sink, source, false);
				for (Task task : graph.getNodes())
				{
					forwardBreakCompletionMap.put(task, (int) forwardShortestPath.getDistance(task));
				}

				// Compute the shortest path from each node to the nearest break.
				graph = constructBreakNetwork(pricingGraph, source, sink, false);
				ShortestPath<Task, Connection> backwardShortestPath = new ShortestPath<>(graph, source, sink, true);
				for (Task task : graph.getNodes())
				{
					backwardBreakCompletionMap.put(task, (int) backwardShortestPath.getDistance(task));
				}

				// Construct a network with duration as costs, only including source and sink
				// arcs to the current crew base.
				graph = constructDurationNetwork(pricingGraph, depot);

				// Compute the shortest path from each node to the sink.
				forwardShortestPath = new ShortestPath<>(graph, sink, source, false);
				backwardShortestPath = new ShortestPath<>(graph, source, sink, true);

				// Construct mapping of task to completion duration.
				for (Task task : graph.getNodes())
				{
					forwardTimeCompletionMap.get(depot).put(task, (int) forwardShortestPath.getDistance(task));
					backwardTimeCompletionMap.get(depot).put(task, (int) backwardShortestPath.getDistance(task));
				}
			}
		}

		// Return resource test.
		return new BreakResourceTest(forwardTimeCompletionMap, backwardTimeCompletionMap, forwardBreakCompletionMap,
				backwardBreakCompletionMap);
	}

	private static DirectedGraph<Task, Connection> constructBreakNetwork(DirectedGraph<Task, Connection> graph,
			Task source, Task sink, boolean forward)
	{
		DirectedGraph<Task, Connection> breakNetwork = new DirectedGraph<>();
		for (Task task : graph.getNodes())
		{
			breakNetwork.addNode(task);
		}
		for (DirectedGraphArc<Task, Connection> arc : graph.getArcs())
		{
			if (arc.getData().hasBreak())
			{
				if (forward)
				{
					// Each break arc leads directly to the sink.
					breakNetwork.addArc(arc.getFrom(), sink, arc.getData(),
							arc.getData().getTimeBeforeBreak());
				}
				else
				{
					breakNetwork.addArc(source, arc.getTo(), arc.getData(), arc.getData().getTimeAfterBreak());
				}
			}
			else
			{
				// Arcs without a break may not lead to the source or sink, depending on the
				// direction.
				if (forward && arc.getTo().equals(sink))
				{
					continue;
				}
				if (!forward && arc.getFrom().equals(source))
				{
					continue;
				}
				breakNetwork.addArc(arc.getFrom(), arc.getTo(), arc.getData(),
						arc.getData().getDuration());
			}
		}
		return breakNetwork;
	}

	private static DirectedGraph<Task, Connection> constructDurationNetwork(DirectedGraph<Task, Connection> graph,
			int depot)
	{
		DirectedGraph<Task, Connection> durationNetwork = new DirectedGraph<>();
		for (Task task : graph.getNodes())
		{
			durationNetwork.addNode(task);
		}
		for (DirectedGraphArc<Task, Connection> arc : graph.getArcs())
		{
			if (arc.getData().getConnectionType().equals(ConnectionType.SOURCE)
					&& arc.getData().getStartBlock() != depot)
			{
				continue;
			}
			if (arc.getData().getConnectionType().equals(ConnectionType.SINK) && arc.getData().getEndBlock() != depot)
			{
				continue;
			}
			durationNetwork.addArc(arc.getFrom(), arc.getTo(), arc.getData(),
					arc.getData().getDuration());
		}
		return durationNetwork;
	}
}
