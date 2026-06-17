package scheduling.constraints.sharingSweetAndSour;

import modelling.Station;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public abstract class AbstractSharingConstraint
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	protected final double threshold;
	protected final Station crewBase;
	
	public AbstractSharingConstraint(ConstraintType constraintType, double bound, double threshold, Station crewBase)
	{
		super(constraintType, bound);

		this.threshold = threshold;
		this.crewBase = crewBase;
	}

	public double getThreshold()
	{
		return threshold;
	}
	
	public Station getCrewBase()
	{
		return crewBase;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((crewBase == null) ? 0 : crewBase.hashCode());
		long temp;
		temp = Double.doubleToLongBits(threshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		AbstractSharingConstraint other = (AbstractSharingConstraint) obj;
		if (crewBase == null)
		{
			if (other.crewBase != null) return false;
		}
		else if (!crewBase.equals(other.crewBase)) return false;
		if (Double.doubleToLongBits(threshold) != Double.doubleToLongBits(other.threshold)) return false;
		return true;
	}
}
