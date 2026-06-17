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

public class BranchingConstraintDutyStartTime
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final int hour;

	public BranchingConstraintDutyStartTime(ConstraintType constraintType, int bound, int hour)
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
			int startTime = duty.getStartTime();
			return startTime >= hour && startTime < hour + Constants.SECONDS_PER_HOUR;
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
		// The dual applies to source arcs whose start time falls in [hour, hour + 1).
		LocalDate date = instance.getDays().get(0);
		PricingGraph graph = instance.getDailyPricingGraph(date);
		for (DirectedGraphArc<Task, Connection> sourceArc : graph.getOutArcs(graph.getSourceNode()))
		{
			int startTime = ArcTime.getStartTime(sourceArc);
			if (startTime >= hour && startTime < hour + Constants.SECONDS_PER_HOUR)
			{
				sourceArc.addDual(dual, 0);
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
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingConstraintDutyStartTime other = (BranchingConstraintDutyStartTime) obj;
		if (hour != other.hour) return false;
		return true;
	}
}
