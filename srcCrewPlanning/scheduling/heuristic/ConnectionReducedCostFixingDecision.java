package scheduling.heuristic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.structures.CleanPricingGraph;
import graph.structures.PricingGraph;
import modelling.Connection;
import modelling.Task;
import optimisation.BAP.AbstractReducedCostFixingDecision;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class ConnectionReducedCostFixingDecision
		extends AbstractReducedCostFixingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final LocalDate date;
	private final Set<Connection> allowedConnections;

	public ConnectionReducedCostFixingDecision(LocalDate date, Set<Connection> allowedConnections)
	{
		this.date = date;
		this.allowedConnections = allowedConnections;
	}

	public Set<Connection> getAllowedConnections()
	{
		return allowedConnections;
	}

	@Override
	public void modifyInstance(SchedulingInstance instance)
	{
		// Construct a new pricing graph, containing only connections that are not fixed
		// to zero.
		PricingGraph graph = instance.getDailyPricingGraph(date);
		int numOld = graph.getNumberOfArcs();
		List<Task> tasks = new ArrayList<>();
		Map<Integer, Task> indexMap = new LinkedHashMap<>();
		for (int i = 1; i < graph.getNodes().size() - 1; i++)
		{
			Task task = graph.getNodes().get(i);
			tasks.add(task);
			indexMap.put(task.getIndex(), task);
		}
		PricingGraph newGraph = CleanPricingGraph.initialisePricingGraph(date, tasks, indexMap, allowedConnections);
		int numNew = newGraph.getNumberOfArcs();
		System.out.println("New graph has " + numNew + " arcs, compared to " + numOld + " in old graph.");
		instance.setDailyPricingGraph(date, newGraph);
	}
}
