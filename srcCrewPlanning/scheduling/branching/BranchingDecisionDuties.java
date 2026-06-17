package scheduling.branching;

import java.util.LinkedHashSet;
import java.util.Set;

import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionDuties
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final Set<DutyColumn> duties;

	public BranchingDecisionDuties(boolean isAllowed, Set<DutyColumn> duties)
	{
		this.isAllowed = isAllowed;
		this.duties = duties;
	}
	
	public Set<DutyColumn> getDuties()
	{
		return duties;
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
		int bound = isAllowed ? duties.size() : 0;
		ConstraintType constraintType = isAllowed ? ConstraintType.GREATER : ConstraintType.LESSER;
		constraints.add(new BranchingConstraintDuties(constraintType, bound, duties));
		return constraints;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((duties == null) ? 0 : duties.hashCode());
		result = prime * result + (isAllowed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingDecisionDuties other = (BranchingDecisionDuties) obj;
		if (duties == null)
		{
			if (other.duties != null) return false;
		}
		else if (!duties.equals(other.duties)) return false;
		if (isAllowed != other.isAllowed) return false;
		return true;
	}
}