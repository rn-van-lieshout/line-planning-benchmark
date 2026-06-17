package linePlanning.constraints;

import linePlanning.columnGeneration.LineColumn;
import linePlanning.columnGeneration.LinePlanningColumn;
import linePlanning.columnGeneration.LinePlanningPP;
import linePlanning.instance.Line;
import linePlanning.instance.LinePlanningInstance;
import optimisation.columnGeneration.AbstractConstraint;

public class ReverseLineConstraint extends AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP>
{
	private final Line line;

	public ReverseLineConstraint(Line line)
	{
		super(ConstraintType.EQUALITY, 0);

		this.line = line;
	}

	@Override
	public boolean containsColumn(LinePlanningColumn column)
	{
		if (column instanceof LineColumn)
		{
			LineColumn lineColumn = (LineColumn) column;
			return lineColumn.getLine().equals(line) || lineColumn.getLine().equals(line.getReverseLine());
		}
		return false;
	}

	@Override
	public double getCoefficient(LinePlanningColumn column)
	{
		LineColumn lineColumn = (LineColumn) column;
		if (lineColumn.getLine().equals(line))
		{
			return 1;
		}
		return -1;
	}
	

	@Override
	public void updateGenericDuals(LinePlanningInstance instance, double dual)
	{
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(LinePlanningPP pricingProblem, double dual)
	{
		// Do nothing.
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		ReverseLineConstraint other = (ReverseLineConstraint) obj;
		if (line == null)
		{
			if (other.line != null) return false;
		}
		else if (!line.equals(other.line)) return false;
		return true;
	}
}
