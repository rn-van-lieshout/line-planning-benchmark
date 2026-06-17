package scheduling.columnGeneration;

import optimisation.columnGeneration.AbstractColumn;
import scheduling.SchedulingInstance;

public abstract class SchedulingColumn extends AbstractColumn<SchedulingInstance, PricingProblem>
{
	public SchedulingColumn(double cost, boolean isRemovableColumn, boolean isIntegerValued)
	{
		super(cost, isRemovableColumn, isIntegerValued);
	}
}
