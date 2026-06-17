package scheduling.branching;

import java.time.LocalDate;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Constants;
import modelling.Duty;
import modelling.Task;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.ArcTime;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingConstraintDutiesPerHour
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final int hour;

	public BranchingConstraintDutiesPerHour(ConstraintType constraintType, int bound, int hour)
	{
		super(constraintType, bound);

		this.hour = hour;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			Duty duty = ((DutyColumn) column).getDuty();
			return (duty.getStartTime() < (hour + Constants.SECONDS_PER_HOUR) && hour <= duty.getEndTime());
		}
		return false;
	}

	@Override
	public double getCoefficient(SchedulingColumn column)
	{
		return 1;
	}

	@Override
	public void updateGenericDuals(SchedulingInstance instance, double dual)
	{
		// We must add the dual if the duty is active between [hour, hour + 1).
		// We place the dual on the source arc if the startTime is < hour + 1.
		// We deduct the dual on the sink arc if the end time is before hour.
		LocalDate date = instance.getDays().get(0);
		PricingGraph graph = instance.getDailyPricingGraph(date);
		for (DirectedGraphArc<Task, Connection> sourceArc : graph.getOutArcs(graph.getSourceNode()))
		{
			int startTime = ArcTime.getStartTime(sourceArc);
			if (startTime < hour + Constants.SECONDS_PER_HOUR)
			{
				sourceArc.addDual(dual, 0);
			}
		}
		for (DirectedGraphArc<Task, Connection> sinkArc : graph.getInArcs(graph.getSinkNode()))
		{
			int endTime = ArcTime.getEndTime(sinkArc);
			if (endTime < hour)
			{
				sinkArc.addDual(-dual, 0);
			}
		}
	}

	@Override
	public void updatePricingProblemDuals(PricingProblem pricingProblem, double dual)
	{
		// Do nothing.
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + hour;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BranchingConstraintDutiesPerHour other = (BranchingConstraintDutiesPerHour) obj;
		if (hour != other.hour)
			return false;
		return true;
	}
}
