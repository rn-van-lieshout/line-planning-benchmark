package graph.structures;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.algorithms.shortestPath.ShortestPathTimeLimit;
import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Constants;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import modelling.comparators.TaskComparator;
import scheduling.ArcTime;

public class CleanPricingGraph
{
	private final static int EARLIEST_START_TIME = 0;
	private final static int LATEST_END_TIME = 4 * Constants.SECONDS_PER_DAY;

	public static boolean removeConnection(Task startTask, Task endTask, Connection connection, Template template)
	{
		if (ArcTime.getStartTime(startTask, endTask, connection) < template.getStartTime())
		{
			return true;
		}
		if (ArcTime.getEndTime(startTask, endTask, connection) > template.getEndTime())
		{
			return true;
		}
		if (connection.getConnectionType().equals(ConnectionType.SOURCE))
		{
			// Check if start station of the source arc matches the template base.
			if (template.getCrewBase().getIndex() != connection.getStartBlock())
			{
				return true;
			}
		}
		if (connection.getConnectionType().equals(ConnectionType.SINK))
		{
			// Check if end station of the sink arc matches the template base.
			if (template.getCrewBase().getIndex() != connection.getEndBlock())
			{
				return true;
			}
		}
		return false;
	}

	public static PricingGraph initialisePricingGraph(LocalDate day, List<Task> tasks,
			Map<Integer, Task> taskMap, Collection<Connection> connections)
	{
		// Initialise new pricing graph.
		PricingGraph graph = new PricingGraph();

		// Add a source and sink node.
		Task sourceTask = new Task(-2, false, day, EARLIEST_START_TIME, EARLIEST_START_TIME,
				new Station(-1, "source"), new Station(-1, "source"), 0, 0, 0, 0);
		Task sinkTask = new Task(-1, false, day, LATEST_END_TIME, LATEST_END_TIME,
				new Station(-1, "sink"), new Station(-1, "sink"), 0, 0, 0, 0);
		graph.addNode(sourceTask);
		graph.addNode(sinkTask);

		// Add all tasks.
		for (Task task : tasks)
		{
			graph.addNode(task);
		}

		// Add all connections.
		for (Connection connection : connections)
		{
			Task startTask = null, endTask = null;
			switch (connection.getConnectionType())
			{
				case SOURCE:
					startTask = sourceTask;
					endTask = taskMap.get(connection.getEndBlock());
					break;
				case SINK:
					startTask = taskMap.get(connection.getStartBlock());
					endTask = sinkTask;
					break;
				case CONNECTION:
					startTask = taskMap.get(connection.getStartBlock());
					endTask = taskMap.get(connection.getEndBlock());
					break;
				default:
					break;
			}

			if (!graph.getNodes().contains(startTask) || !graph.getNodes().contains(endTask))
			{
				continue;
			}
			graph.addArc(startTask, endTask, connection, connection.getCost());
		}

		// Sort nodes, set node indices, and return the graph.
		graph.sortNodes(new TaskComparator());
		graph.setNodeIndices();
		return graph;
	}

	public static void expandPricingGraph(PricingGraph graph)
	{
		int index = 0;
		DirectedGraph<Task, Connection> durationNetwork = new DirectedGraph<>();
		for (Task task : graph.getNodes())
		{
			durationNetwork.addNode(task);
		}
		for (DirectedGraphArc<Task, Connection> arc : graph.getArcs())
		{
			index = Math.max(index, arc.getData().getIndex() + 1);
			durationNetwork.addArc(arc.getFrom(), arc.getTo(), arc.getData(),
					arc.getData().getDuration());
		}
		Task source = graph.getSourceNode();
		Task sink = graph.getSinkNode();
		for (Task task : durationNetwork.getNodes())
		{
			// Check which connections must be added.
			if (task.equals(sink) || task.equals(source))
			{
				continue;
			}
			ShortestPathTimeLimit shortestPath = new ShortestPathTimeLimit(durationNetwork, task, sink, true,
					task.getStartTime(), task.getStartTime() + Constants.MAXIMUM_DUTY_LENGTH);
			Set<Task> addConnection = new LinkedHashSet<>();
			for (Task otherTask : durationNetwork.getNodes())
			{
				if (!shortestPath.containsPath(otherTask))
				{
					continue;
				}
				if (otherTask.equals(sink) || otherTask.equals(source) || otherTask.equals(task))
				{
					continue;
				}
				addConnection.add(otherTask);
			}
			Set<Task> alreadyConnected = new LinkedHashSet<>();
			for (DirectedGraphArc<Task, Connection> arc : graph.getOutArcs(task))
			{
				alreadyConnected.add(arc.getTo());
			}
			addConnection.removeAll(alreadyConnected);

			// Add missing connections.
			for (Task otherTask : addConnection)
			{
				int duration = otherTask.getEndTime() - task.getEndTime();
				Connection connection = new Connection(index, ConnectionType.CONNECTION, task.getIndex(),
						otherTask.getIndex(), -1, -1, duration, duration, otherTask.getRefWorkingTime(), 0, 0, 0,
						new ArrayList<>());
				index++;
				graph.addArc(task, otherTask, connection, duration);
			}
		}

		// Sort nodes, clean the graph, and return it.
		graph.sortNodes(new TaskComparator());
	}

	public static class LinearTaskComparator implements Comparator<Task>
	{
		@Override
		public int compare(Task a, Task b)
		{
			if (a.getDay().isEqual(b.getDay()))
			{
				if (a.getEndTime() == b.getEndTime())
				{
					return a.getIndex() - b.getIndex();
				}
				return a.getEndTime() - b.getEndTime();
			}
			return a.getDay().compareTo(b.getDay());
		}
	}
}
