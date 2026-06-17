package scheduling.branching;

import modelling.Station;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingConstraintCrewBase
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Station crewBase;

	public BranchingConstraintCrewBase(ConstraintType constraintType, int bound, Station crewBase)
	{
		super(constraintType, bound);

		this.crewBase = crewBase;
	}

	public Station getCrewBase()
	{
		return crewBase;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			if (((DutyColumn) column).getDuty().getTemplate().getCrewBase().equals(crewBase))
			{
				return true;
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
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(PricingProblem pricingProblem, double dual)
	{
		if (pricingProblem.getTemplate().getCrewBase().equals(crewBase))
		{
			pricingProblem.addFixedDual(dual);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((crewBase == null) ? 0 : crewBase.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingConstraintCrewBase other = (BranchingConstraintCrewBase) obj;
		if (crewBase == null)
		{
			if (other.crewBase != null) return false;
		}
		else if (!crewBase.equals(other.crewBase)) return false;
		return true;
	}
}
