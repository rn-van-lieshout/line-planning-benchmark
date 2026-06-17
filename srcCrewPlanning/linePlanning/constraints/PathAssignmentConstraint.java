package linePlanning.constraints;

import linePlanning.columnGeneration.LinePlanningColumn;
import linePlanning.columnGeneration.LinePlanningPP;
import linePlanning.columnGeneration.PathColumn;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import optimisation.columnGeneration.AbstractConstraint;

public class PathAssignmentConstraint
		extends AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP>
{
	private final ODPair ODPair;

	public PathAssignmentConstraint(ODPair ODPair)
	{
		super(ConstraintType.GREATER, ODPair.getVolume());

		this.ODPair = ODPair;
	}

	public ODPair getODPair()
	{
		return ODPair;
	}

	@Override
	public boolean containsColumn(LinePlanningColumn column)
	{
		if (column instanceof PathColumn)
		{
			PathColumn pathColumn = (PathColumn) column;
			return pathColumn.getPath().getODPair().equals(ODPair);
		}
		return false;
	}

	@Override
	public double getCoefficient(LinePlanningColumn column)
	{
		return 1;
	}
	
	@Override
	public void updateGenericDuals(LinePlanningInstance instance, double dual)
	{
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(LinePlanningPP pricingProblem, double dual)
	{
		if (pricingProblem.getOrigin().equals(ODPair.getOrigin()))
		{
			pricingProblem.addODPairDual(ODPair, dual);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((ODPair == null) ? 0 : ODPair.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		PathAssignmentConstraint other = (PathAssignmentConstraint) obj;
		if (ODPair == null)
		{
			if (other.ODPair != null) return false;
		}
		else if (!ODPair.equals(other.ODPair)) return false;
		return true;
	}
}
