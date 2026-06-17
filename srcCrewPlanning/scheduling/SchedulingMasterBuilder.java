package scheduling;

import ilog.concert.IloException;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class SchedulingMasterBuilder
{
	private SchedulingInstance problem;

	public SchedulingMasterBuilder setProblem(SchedulingInstance problem)
	{
		this.problem = problem;
		return this;
	}

	public SchedulingMaster build() throws IloException
	{
		SchedulingMaster model = new SchedulingMaster(problem);

		// Add all constraints.
		for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : problem.getConstraints())
		{
			model.addConstraint(constraint);
		}
		return model;
	}
}
