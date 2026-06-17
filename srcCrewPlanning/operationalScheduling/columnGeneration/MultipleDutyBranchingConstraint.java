package operationalScheduling.columnGeneration;

import java.util.Set;

import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class MultipleDutyBranchingConstraint
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Set<DutyColumn> dutyColumns;

	public MultipleDutyBranchingConstraint(ConstraintType constraintType, int bound, Set<DutyColumn> dutyColumns)
	{
		super(constraintType, bound);

		this.dutyColumns = dutyColumns;
	}

	public Set<DutyColumn> getDutyColumns()
	{
		return dutyColumns;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			return dutyColumns.contains(c);
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
		result = prime * result + ((dutyColumns == null) ? 0 : dutyColumns.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		MultipleDutyBranchingConstraint other = (MultipleDutyBranchingConstraint) obj;
		if (dutyColumns == null)
		{
			if (other.dutyColumns != null) return false;
		}
		else if (!dutyColumns.equals(other.dutyColumns)) return false;
		return true;
	}
}
