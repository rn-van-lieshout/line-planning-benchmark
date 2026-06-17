package scheduling.branching;

import java.time.LocalDate;
import java.util.Objects;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingConstraintDutiesPerTask
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Task task;

	public BranchingConstraintDutiesPerTask(ConstraintType constraintType, int bound, Task task)
	{
		super(constraintType, bound);

		this.task = task;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			if (c.getDuty().getTasks().contains(task))
			{
				return true;
			}
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
		// Place connection dual on connection arcs.
		for (LocalDate date : instance.getDays())
		{
			for (DirectedGraphArc<Task, Connection> arc : instance.getDailyPricingGraph(date).getInArcs(task))
			{
				arc.addDual(dual, 0);
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
		result = prime * result + Objects.hash(task);
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
		BranchingConstraintDutiesPerTask other = (BranchingConstraintDutiesPerTask) obj;
		return Objects.equals(task, other.task);
	}
}
