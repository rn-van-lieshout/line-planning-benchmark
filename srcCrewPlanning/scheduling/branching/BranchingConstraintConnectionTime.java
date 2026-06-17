package scheduling.branching;

import java.time.LocalDate;
import java.util.Objects;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Task;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingConstraintConnectionTime
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final int block;
	private final boolean incoming;
	private final int duration;

	public BranchingConstraintConnectionTime(ConstraintType constraintType, int bound, int block, boolean incoming,
			int duration)
	{
		super(constraintType, bound);

		this.block = block;
		this.incoming = incoming;
		this.duration = duration;
	}

	public int getBlock()
	{
		return block;
	}

	public boolean isIncoming()
	{
		return incoming;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			for (Connection connection : c.getDuty().getConnections())
			{
				if (incoming && connection.getConnectionType().equals(ConnectionType.SINK))
				{
					continue;
				}
				if (!incoming && connection.getConnectionType().equals(ConnectionType.SOURCE))
				{
					continue;
				}
				int connectionBlock = incoming ? connection.getEndBlock() : connection.getStartBlock();
				if (connectionBlock == block && connection.getDuration() <= duration)
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
				Connection connection = arc.getData();
				if (incoming && connection.getConnectionType().equals(ConnectionType.SINK))
				{
					continue;
				}
				if (!incoming && connection.getConnectionType().equals(ConnectionType.SOURCE))
				{
					continue;
				}
				int connectionBlock = incoming ? connection.getEndBlock() : connection.getStartBlock();
				if (connectionBlock == block && connection.getDuration() <= duration)
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
		result = prime * result + Objects.hash(block, duration, incoming);
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
		BranchingConstraintConnectionTime other = (BranchingConstraintConnectionTime) obj;
		return block == other.block && duration == other.duration && incoming == other.incoming;
	}
}
