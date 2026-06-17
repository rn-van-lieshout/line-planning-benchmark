package scheduling.branching;

import java.util.LinkedHashSet;
import java.util.Set;

import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionConnectionTime
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final int bound;
	private final int block;
	private final boolean incoming;
	private final int duration;

	public BranchingDecisionConnectionTime(boolean isAllowed, int bound, int block, boolean incoming, int duration)
	{
		this.isAllowed = isAllowed;
		this.bound = bound;
		this.block = block;
		this.incoming = incoming;
		this.duration = duration;
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
		constraints.add(new BranchingConstraintConnectionTime(constraintType, bound, block, incoming, duration));
		return constraints;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + block;
		result = prime * result + bound;
		result = prime * result + duration;
		result = prime * result + (incoming ? 1231 : 1237);
		result = prime * result + (isAllowed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingDecisionConnectionTime other = (BranchingDecisionConnectionTime) obj;
		if (block != other.block) return false;
		if (bound != other.bound) return false;
		if (duration != other.duration) return false;
		if (incoming != other.incoming) return false;
		if (isAllowed != other.isAllowed) return false;
		return true;
	}
}