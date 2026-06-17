package modelling.constraints.dutyConstraints;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Constants;
import modelling.Duty;
import modelling.Task;
import modelling.Connection.ConnectionType;
import modelling.comparators.ConnectionEndTimeComparator;
import scheduling.ArcTime;

public class FeasibilityChecker
{
	public static boolean checkFeasibility(Duty duty, PricingGraph graph)
	{
		// Retrieve and sort arcs in duty.
		List<DirectedGraphArc<Task, Connection>> arcs = new ArrayList<>();
		for (Connection connection : duty.getConnections())
		{
			for (DirectedGraphArc<Task, Connection> arc : graph.getArcs())
			{
				if (arc.getData().equals(connection))
				{
					arcs.add(arc);
					break;
				}
			}
		}
		if (arcs.size() != duty.getConnections().size())
		{
			System.out.println("Invalid connections.");
			return false;
		}
		arcs.sort(new ConnectionEndTimeComparator());	
		
		// Check duty length.
		int startTime = ArcTime.getStartTime(arcs.get(0));
		int endTime = ArcTime.getEndTime(arcs.get(arcs.size() - 1));
		if (!DutyLength.isFeasibleEndTime(startTime, endTime))
		{
			System.out.println("Duty length violated: " + startTime + " " + endTime);
			return false;
		}
		
		// Check whether start and end time are correct.
		if (duty.getStartTime() != startTime)
		{
			System.out.println("Start time is incorrect: " + startTime + " " + duty.getStartTime());
			return false;
		}
		if (duty.getEndTime() != endTime)
		{
			System.out.println("End time is incorrect: " + endTime + " " + duty.getEndTime());
			return false;
		}
		
		// Check whether all tasks are indeed contained.
		Set<Task> tasks = new LinkedHashSet<>();
		for (DirectedGraphArc<Task, Connection> arc : arcs)
		{
			if (arc.getTo().cover())
			{
				tasks.add(arc.getTo());
			}
		}
		for (Task task : duty.getTasks())
		{
			if (!tasks.contains(task))
			{
				System.out.println("Task is not actually contained: " + task.getIndex());
				return false;
			}
		}
		
		// Check whether start and end depot match.
		int startDepot = -1;
		int endDepot = -2;
		for (DirectedGraphArc<Task, Connection> arc : arcs)
		{
			if (arc.getData().getConnectionType().equals(ConnectionType.SOURCE))
			{
				startDepot = arc.getData().getStartBlock();
			}
			if (arc.getData().getConnectionType().equals(ConnectionType.SINK))
			{
				endDepot = arc.getData().getEndBlock();
			}
		}
		if (startDepot != endDepot)
		{
			System.out.println("Infeasible depots: " + startDepot + " " + endDepot);
			return false;
		}
		
		// Check whether the workload and costs are correct.
		int trueWorkload = 0;
		int trueCost = 0;
		for (DirectedGraphArc<Task, Connection> arc : arcs)
		{
			trueWorkload += arc.getData().getDuration();
			trueCost += arc.getData().getCost();
		}
		if (duty.getCost() != trueCost)
		{
			System.out.println("Costs are incorrect: " + trueCost + " " + duty.getCost());
			return false;
		}
		if (duty.getWorkload() != trueWorkload)
		{
			System.out.println("Workload is incorrect: " + trueWorkload + " " + duty.getWorkload());
			return false;
		}
		
		// Check whether arcs form a valid path through the network.
		for (int i = 0; i < arcs.size() - 1; i++)
		{
			Task from = arcs.get(i).getTo();
			Task to = arcs.get(i + 1).getFrom();
			if (!from.equals(to))
			{
				System.out.println("Invalid pair of arcs.");
			}
		}

		// Check the meal break constraint.
		int timeWithoutBreak = 0;
		boolean hasBreak = false;
		for (DirectedGraphArc<Task, Connection> arc : arcs)
		{
			timeWithoutBreak += arc.getData().getDuration();
			if (arc.getData().hasBreak())
			{
				// Update if possible.
				if ((ArcTime.getStartTime(arc) - startTime)
						+ arc.getData().getTimeBeforeBreak() <= Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK)
				{
					hasBreak = true;
					timeWithoutBreak = ArcTime.getEndTime(arc) - arc.getTo().getStartTime();
				}
			}
			if (timeWithoutBreak > Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK)
			{
				System.out.println("Violated time without meal break.");
				return false;
			}
		}
		if (!hasBreak)
		{
			System.out.println("Has no meal break");
			return false;
		}
		return true;
	}
}
