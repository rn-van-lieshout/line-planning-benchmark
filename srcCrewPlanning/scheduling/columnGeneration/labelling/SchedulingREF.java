package scheduling.columnGeneration.labelling;

import graph.algorithms.spprc.Label;
import graph.algorithms.spprc.REF;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Task;
import modelling.constraints.dutyConstraints.DutyLength;
import modelling.constraints.dutyConstraints.MealBreakFunction;
import scheduling.ArcTime;

public class SchedulingREF extends REF<Task, Connection>
{
	public final static int INDEX_DEPOT = 0;
	public final static int INDEX_START_TIME = 1;
	public final static int INDEX_WORKLOAD = 2;
	public final static int INDEX_WITHOUT_BREAK = 3;
	public final static int INDEX_HAD_BREAK = 4;

	@Override
	public int[] extendResourceVector(Label<Task, Connection> label, DirectedGraphArc<Task, Connection> arc,
			boolean forward)
	{
		// Retrieve resource vector.
		int[] resources = label.getResourceVector().clone();

		// Update depot.
		if (arc.getData().getConnectionType().equals(ConnectionType.SOURCE))
		{
			resources[INDEX_DEPOT] = arc.getData().getStartBlock();
		}
		if (arc.getData().getConnectionType().equals(ConnectionType.SINK))
		{
			resources[INDEX_DEPOT] = arc.getData().getEndBlock();
		}

		// Update the meal break.
		resources[INDEX_WITHOUT_BREAK] += arc.getData().getDuration();
		if (arc.getData().hasBreak())
		{
			int timeBeforeBreak = forward ? resources[INDEX_WORKLOAD] + arc.getData().getTimeBeforeBreak()
					: resources[INDEX_WORKLOAD] + arc.getData().getTimeAfterBreak();
			int timeAfterBreak = forward ? arc.getData().getTimeAfterBreak() : arc.getData().getTimeBeforeBreak();
			if (MealBreakFunction.satisfiesMealBreakConstraint(timeBeforeBreak))
			{
				resources[INDEX_WITHOUT_BREAK] = timeAfterBreak;
				resources[INDEX_HAD_BREAK] = 1;
			}
		}

		// Update duration and start time.
		resources[INDEX_WORKLOAD] += arc.getData().getDuration();
		resources[INDEX_START_TIME] = Math.min(resources[INDEX_START_TIME], ArcTime.getStartTime(arc));
		if (resources[INDEX_START_TIME] == 0)
		{
			resources[INDEX_START_TIME] = ArcTime.getStartTime(arc);
		}

		// Return new resource vector.
		return resources;
	}

	@Override
	public boolean isFeasible(int[] resourceVector)
	{
		int startTime = resourceVector[INDEX_START_TIME];
		int endTime = startTime + resourceVector[INDEX_WORKLOAD];
		return DutyLength.isFeasibleEndTime(startTime, endTime)
				&& MealBreakFunction.satisfiesMealBreakConstraint(resourceVector[INDEX_WITHOUT_BREAK]);
	}

	@Override
	public boolean isFeasibleAtSink(int[] resourceVector)
	{
		if (resourceVector[INDEX_HAD_BREAK] == 0)
		{
			return false;
		}
		return isFeasible(resourceVector);
	}

	@Override
	public boolean dominates(int[] first, int[] second)
	{
		// The first resource vector dominates the second if it has a lower workload,
		// lower time without break, and has had the same amount of break opportunities.
		if (first[INDEX_WORKLOAD] > second[INDEX_WORKLOAD])
		{
			return false;
		}
		if (first[INDEX_WITHOUT_BREAK] > second[INDEX_WITHOUT_BREAK])
		{
			return false;
		}
		if (first[INDEX_HAD_BREAK] < second[INDEX_HAD_BREAK])
		{
			return false;
		}
		return true;
	}

	@Override
	public int[] concatenateResourceVectors(DirectedGraphArc<Task, Connection> arc, int[] forwardVector,
			int[] backwardVector)
	{
		int[] resources = forwardVector.clone();

		// Update depot.
		if (arc.getData().getConnectionType().equals(ConnectionType.SOURCE))
		{
			resources[INDEX_DEPOT] = arc.getData().getStartBlock();
		}
		if (arc.getData().getConnectionType().equals(ConnectionType.SINK))
		{
			resources[INDEX_DEPOT] = arc.getData().getEndBlock();
		}

		// Compute the minimum break option. We consider a break in the first part,
		// break on the current arc, or break in the second part.
		int timeWithoutBreak = Integer.MAX_VALUE;
		if (resources[INDEX_HAD_BREAK] == 1)
		{
			timeWithoutBreak = resources[INDEX_WITHOUT_BREAK] + arc.getData().getDuration()
					+ backwardVector[INDEX_WORKLOAD];
		}
		if (backwardVector[INDEX_HAD_BREAK] == 1)
		{
			timeWithoutBreak = Math.min(timeWithoutBreak,
					resources[INDEX_WORKLOAD] + arc.getData().getDuration() + backwardVector[INDEX_WITHOUT_BREAK]);
		}
		if (arc.getData().hasBreak())
		{
			int timeBeforeBreak = resources[INDEX_WORKLOAD] + arc.getData().getTimeBeforeBreak();
			int timeAfterBreak = arc.getData().getTimeAfterBreak() + backwardVector[INDEX_WORKLOAD];
			if (MealBreakFunction.satisfiesMealBreakConstraint(Math.max(timeBeforeBreak, timeAfterBreak)))
			{
				timeWithoutBreak = Math.max(timeBeforeBreak, timeAfterBreak);
			}
		}
		resources[INDEX_HAD_BREAK] = MealBreakFunction.satisfiesMealBreakConstraint(timeWithoutBreak) ? 1 : 0;
		resources[INDEX_WITHOUT_BREAK] = timeWithoutBreak;

		// Update duration and start time.
		resources[INDEX_WORKLOAD] += arc.getData().getDuration() + backwardVector[INDEX_WORKLOAD];
		resources[INDEX_START_TIME] = Math.min(resources[INDEX_START_TIME], ArcTime.getStartTime(arc));
		if (resources[INDEX_START_TIME] == 0)
		{
			resources[INDEX_START_TIME] = ArcTime.getStartTime(arc);
		}

		// Return vector.
		return resources;
	}

	@Override
	public int[] getEmptyResourceVector()
	{
		return new int[5];
	}
}