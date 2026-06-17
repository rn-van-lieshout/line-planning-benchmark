package scheduling.branching;

import java.util.LinkedHashSet;
import java.util.Set;

import modelling.Connection;
import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionConnections
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final int bound;
	private final Set<Connection> connections;

	public BranchingDecisionConnections(boolean isAllowed, int bound, Set<Connection> connections)
	{
		this.isAllowed = isAllowed;
		this.bound = bound;
		this.connections = connections;
	}

	public Set<Connection> getConnections()
	{
		return connections;
	}

	@Override
	public boolean isCompatible(PricingProblem pricingProblem)
	{
		return true;
	}

	@Override
	public void modifyPricingProblem(PricingProblem pricingProblem)
	{
		// Do nothing.
	}

	@Override
	public Set<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingConstraints()
	{
		Set<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> constraints = new LinkedHashSet<>();
		ConstraintType constraintType = isAllowed ? ConstraintType.GREATER : ConstraintType.LESSER;
		constraints.add(new BranchingConstraintConnections(constraintType, bound, connections));
		return constraints;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + bound;
		result = prime * result + ((connections == null) ? 0 : connections.hashCode());
		result = prime * result + (isAllowed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingDecisionConnections other = (BranchingDecisionConnections) obj;
		if (bound != other.bound) return false;
		if (connections == null)
		{
			if (other.connections != null) return false;
		}
		else if (!connections.equals(other.connections)) return false;
		if (isAllowed != other.isAllowed) return false;
		return true;
	}
}