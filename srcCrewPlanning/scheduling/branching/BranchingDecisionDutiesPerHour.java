package scheduling.branching;

import java.util.LinkedHashSet;
import java.util.Set;

import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionDutiesPerHour
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final int bound; 
	private final int hour;
	
	public BranchingDecisionDutiesPerHour(boolean isAllowed, int bound, int hour)
	{
		this.isAllowed = isAllowed;
		this.bound = bound;
		this.hour = hour;
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
		constraints.add(new BranchingConstraintDutiesPerHour(constraintType, bound, hour));
		return constraints;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + bound;
		result = prime * result + hour;
		result = prime * result + (isAllowed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingDecisionDutiesPerHour other = (BranchingDecisionDutiesPerHour) obj;
		if (bound != other.bound) return false;
		if (hour != other.hour) return false;
		if (isAllowed != other.isAllowed) return false;
		return true;
	}
}