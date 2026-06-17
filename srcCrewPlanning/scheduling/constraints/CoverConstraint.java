package scheduling.constraints;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class CoverConstraint extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Task task;

	public CoverConstraint(Task task)
	{
		super(ConstraintType.GREATER, 1);

		this.task = task;
	}

	public CoverConstraint(ConstraintType constraintType, Task task)
	{
		super(constraintType, 1);

		this.task = task;
	}

	public Task getTask()
	{
		return task;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			return c.getDuty().getTasks().contains(task);
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
		// Update arc duals.
		PricingGraph pricingGraph = instance.getDailyPricingGraph(task.getDay());
		for (DirectedGraphArc<Task, Connection> arc : pricingGraph.getOutArcs(task))
		{
			arc.addDual(dual, 0);
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
		result = prime * result + ((task == null) ? 0 : task.hashCode());
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
		CoverConstraint other = (CoverConstraint) obj;
		if (task == null)
		{
			if (other.task != null)
				return false;
		}
		else
			if (!task.equals(other.task))
				return false;
		return true;
	}
}
