package scheduling;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import graph.structures.Path;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Duty;
import modelling.Task;
import modelling.Template;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Pair;

public class ConstructDuty
{
	public static Duty constructDuty(Template template, Path<Task, Connection> path)
	{
		// Compute cost and list of tasks.
		int cost = 0;
		int startTime = Integer.MAX_VALUE;
		int endTime = 0;
		List<Connection> connections = new ArrayList<>();
		Set<Task> tasks = new LinkedHashSet<>();
		for (DirectedGraphArc<Task, Connection> arc : path.getArcs())
		{
			cost += arc.getData().getCost();
			connections.add(arc.getData());

			startTime = Math.min(startTime, ArcTime.getStartTime(arc));
			endTime = Math.max(endTime, ArcTime.getEndTime(arc));
			if (!arc.getData().getConnectionType().equals(ConnectionType.SINK))
			{
				tasks.add(arc.getTo());
			}
		}

		// Artificially set index to -1.
		return new Duty(-1, template.getCrewID(), template, startTime, endTime, cost, connections, tasks);
	}

	public static List<Pair<SchedulingColumn, Double>> constructDutyColumns(PricingProblem pricingProblem,
			List<Path<Task, Connection>> paths)
	{
		List<Pair<SchedulingColumn, Double>> columns = new ArrayList<>();
		for (Path<Task, Connection> path : paths)
		{
			// Compute the objective coefficient of the duty. This is stored in both arcs
			// and nodes.
			double objective = 0;
			for (DirectedGraphArc<Task, Connection> arc : path.getArcs())
			{
				objective += arc.getCost();
			}

			DutyColumn column = new DutyColumn(objective, constructDuty(pricingProblem.getTemplate(), path));
			columns.add(new Pair<>(column, path.getWeight()));
		}
		return columns;
	}
}
