package scheduling.branching;

import java.time.LocalDate;
import java.util.Set;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingConstraintConnections
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Set<Connection> connections;

	public BranchingConstraintConnections(ConstraintType constraintType, int bound, Set<Connection> connections)
	{
		super(constraintType, bound);

		this.connections = connections;
	}

	public Set<Connection> getConnections()
	{
		return connections;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			for (Connection connection : connections)
			{
				if (c.getDuty().getConnections().contains(connection))
				{
					return true;
				}
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
			for (DirectedGraphArc<Task, Connection> arc : instance.getDailyPricingGraph(date).getArcs())
			{
				if (connections.contains(arc.getData()))
				{
					arc.addDual(dual, 0);
				}
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
		result = prime * result + ((connections == null) ? 0 : connections.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingConstraintConnections other = (BranchingConstraintConnections) obj;
		if (connections == null)
		{
			if (other.connections != null) return false;
		}
		else if (!connections.equals(other.connections)) return false;
		return true;
	}
}
