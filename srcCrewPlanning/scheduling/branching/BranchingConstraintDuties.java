package scheduling.branching;

import java.util.Set;

import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingConstraintDuties
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Set<DutyColumn> duties; 
	
	public BranchingConstraintDuties(ConstraintType constraintType, int bound, Set<DutyColumn> duties)
	{
		super(constraintType, bound);
		
		this.duties = duties;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			return duties.contains(column);
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
		// Do nothing.
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
		result = prime * result + ((duties == null) ? 0 : duties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingConstraintDuties other = (BranchingConstraintDuties) obj;
		if (duties == null)
		{
			if (other.duties != null) return false;
		}
		else if (!duties.equals(other.duties)) return false;
		return true;
	}
}
