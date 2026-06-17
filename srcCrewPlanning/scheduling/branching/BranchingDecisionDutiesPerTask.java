package scheduling.branching;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import modelling.Task;
import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionDutiesPerTask
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final int bound;
	private final Task task;
	
	public BranchingDecisionDutiesPerTask(boolean isAllowed, int bound, Task task)
	{
		this.isAllowed = isAllowed;
		this.bound = bound;
		this.task = task;
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
		constraints.add(new BranchingConstraintDutiesPerTask(constraintType, bound, task));
		return constraints;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(bound, isAllowed, task);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BranchingDecisionDutiesPerTask other = (BranchingDecisionDutiesPerTask) obj;
		return bound == other.bound && isAllowed == other.isAllowed && Objects.equals(task, other.task);
	}
}