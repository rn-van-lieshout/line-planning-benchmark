package linePlanning.columnGeneration;

import linePlanning.instance.LinePlanningInstance;
import optimisation.columnGeneration.AbstractColumn;

public abstract class LinePlanningColumn extends AbstractColumn<LinePlanningInstance, LinePlanningPP>
{
	public LinePlanningColumn(double cost, boolean isRemovableColumn, boolean isIntegerValued)
	{
		super(cost, isRemovableColumn, isIntegerValued);
	}
}
