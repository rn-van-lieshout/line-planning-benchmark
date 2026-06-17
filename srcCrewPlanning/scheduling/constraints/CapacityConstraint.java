package scheduling.constraints;

import java.time.LocalDate;

import modelling.Duty;
import modelling.Station;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class CapacityConstraint extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Station crewBase;
	private final LocalDate day;

	public CapacityConstraint(Station crewBase, LocalDate day, int capacity)
	{
		super(ConstraintType.LESSER, capacity);

		this.crewBase = crewBase;
		this.day = day;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			Duty duty = c.getDuty();
			return duty.getTemplate().getCrewBase().equals(crewBase) && duty.getTemplate().getDay().isEqual(day);
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
		if (pricingProblem.getTemplate().getCrewBase().equals(crewBase) && pricingProblem.getDay() == day)
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
		result = prime * result + ((day == null) ? 0 : day.hashCode());
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
		CapacityConstraint other = (CapacityConstraint) obj;
		if (crewBase == null)
		{
			if (other.crewBase != null)
				return false;
		}
		else
			if (!crewBase.equals(other.crewBase))
				return false;
		if (day == null)
		{
			if (other.day != null)
				return false;
		}
		else
			if (!day.equals(other.day))
				return false;
		return true;
	}
}
