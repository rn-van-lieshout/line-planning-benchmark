package operationalScheduling.columnGeneration;

import java.util.LinkedHashSet;
import java.util.Set;

import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionMultipleDuty
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final Set<DutyColumn> dutyColumns;

	public BranchingDecisionMultipleDuty(boolean isAllowed, Set<DutyColumn> dutyColumns)
	{
		this.isAllowed = isAllowed;
		this.dutyColumns = dutyColumns;
	}

	public Set<DutyColumn> getDutyColumns()
	{
		return dutyColumns;
	}

	@Override
	public boolean isCompatible(PricingProblem pricingProblem)
	{
		for (DutyColumn dutyColumn : dutyColumns)
		{
			if (dutyColumn.getDuty().getTemplate().equals(pricingProblem.getTemplate()))
			{
				return false;
			}
		}
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
		int bound = isAllowed ? dutyColumns.size() : 0;
		ConstraintType constraintType = isAllowed ? ConstraintType.GREATER : ConstraintType.LESSER;
		constraints.add(new MultipleDutyBranchingConstraint(constraintType, bound, dutyColumns));
		return constraints;
	}
}