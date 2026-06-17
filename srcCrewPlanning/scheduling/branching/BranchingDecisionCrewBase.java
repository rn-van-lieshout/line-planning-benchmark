package scheduling.branching;

import java.util.LinkedHashSet;
import java.util.Set;

import modelling.Station;
import optimisation.BAP.AbstractBranchingDecision;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class BranchingDecisionCrewBase
		extends AbstractBranchingDecision<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final boolean isAllowed;
	private final int bound; 
	private final Station crewBase;
	
	public BranchingDecisionCrewBase(boolean isAllowed, int bound, Station crewBase)
	{
		this.isAllowed = isAllowed;
		this.bound = bound;
		this.crewBase = crewBase;
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
		constraints.add(new BranchingConstraintCrewBase(constraintType, bound, crewBase));
		return constraints;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + bound;
		result = prime * result + ((crewBase == null) ? 0 : crewBase.hashCode());
		result = prime * result + (isAllowed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BranchingDecisionCrewBase other = (BranchingDecisionCrewBase) obj;
		if (bound != other.bound) return false;
		if (crewBase == null)
		{
			if (other.crewBase != null) return false;
		}
		else if (!crewBase.equals(other.crewBase)) return false;
		if (isAllowed != other.isAllowed) return false;
		return true;
	}
}