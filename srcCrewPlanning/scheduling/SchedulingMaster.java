package scheduling;

import java.util.LinkedHashMap;
import java.util.Map;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import modelling.Duty;
import modelling.Task;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractMasterProblem;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import scheduling.constraints.CoverConstraint;

/**
 * General class that models a linear program containing duties, templates, and
 * linear constraints involving duties and/or templates.
 * 
 * @author Bart van Rossum
 *
 */
public class SchedulingMaster extends AbstractMasterProblem<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public SchedulingMaster(SchedulingInstance problem) throws IloException
	{
		super(problem);
	}

	@Override
	public void addColumn(SchedulingColumn column) throws IloException
	{
		// Initialise a new column.
		IloColumn columnVar = cplex.column(objective, column.getCoefficient());

		// Loop over all branching constraints.
		for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : branchingConstraints)
		{
			if (constraint.containsColumn(column))
			{
				columnVar = columnVar
						.and(cplex.column(constraintMap.get(constraint), constraint.getCoefficient(column)));
			}
		}

		// Loop over generic constraints.
		for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : instance
				.getGenericConstraints())
		{
			if (constraint.containsColumn(column))
			{
				columnVar = columnVar
						.and(cplex.column(constraintMap.get(constraint), constraint.getCoefficient(column)));
			}
		}

		// Loop over duty- or template-specific constraints.
		if (column instanceof DutyColumn)
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			Duty duty = dutyColumn.getDuty();

			// Avoid duplicate columns.
			if (columnMap.containsKey(dutyColumn))
			{
				return;
			}

			// Add to task constraints.
			for (Task task : duty.getTasks())
			{
				if (!task.cover())
				{
					continue;
				}
				CoverConstraint constraint = instance.getCoverConstraint(task);
				if (constraint.containsColumn(dutyColumn))
				{
					columnVar = columnVar
							.and(cplex.column(constraintMap.get(constraint), constraint.getCoefficient(column)));
				}
			}

			// Add to template-specific constraints.
			for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : instance
					.getGroupedConstraints(duty.getTemplate().getDay(), duty.getTemplate()))
			{
				if (constraint.containsColumn(column))
				{
					columnVar = columnVar
							.and(cplex.column(constraintMap.get(constraint), constraint.getCoefficient(column)));
				}
			}
		}
		columnMap.put(column, cplex.numVar(columnVar, 0, Double.MAX_VALUE));
	}

	@Override
	public SchedulingSolution getSolution() throws IloException
	{
		// Retrieve all columns with positive coefficients.
		Map<SchedulingColumn, Double> solutionMap = new LinkedHashMap<>();
		for (SchedulingColumn column : columnMap.keySet())
		{
			double value = cplex.getValue(columnMap.get(column));
			if (value > 0)
			{
				solutionMap.put(column, value);
			}
		}
		return new SchedulingSolution(getObjectiveValue(), solutionMap);
	}

	@Override
	public double getReducedCost(SchedulingColumn column, boolean useSeparationDuals)
	{
		double reducedCost = column.getCoefficient();

		// Loop over all branching constraints.
		for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : branchingConstraints)
		{
			if (constraint.containsColumn(column))
			{
				double dual = useSeparationDuals ? separationDuals.get(constraint) : currentDuals.get(constraint);
				double coefficient = constraint.getCoefficient(column);
				reducedCost -= dual * coefficient;
			}
		}

		// Loop over generic constraints.
		for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : instance
				.getGenericConstraints())
		{
			if (constraint.containsColumn(column))
			{
				double dual = useSeparationDuals ? separationDuals.get(constraint) : currentDuals.get(constraint);
				double coefficient = constraint.getCoefficient(column);
				reducedCost -= dual * coefficient;
			}
		}

		// Loop over duty- or template-specific constraints.
		if (column instanceof DutyColumn)
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			Duty duty = dutyColumn.getDuty();

			if (columnMap.containsKey(dutyColumn))
			{
				return Double.MAX_VALUE;
			}

			// Add to task constraints.
			for (Task task : duty.getTasks())
			{
				if (!task.cover())
				{
					continue;
				}
				CoverConstraint constraint = instance.getCoverConstraint(task);
				if (constraint.containsColumn(dutyColumn))
				{
					double dual = useSeparationDuals ? separationDuals.get(constraint) : currentDuals.get(constraint);
					double coefficient = constraint.getCoefficient(column);
					reducedCost -= dual * coefficient;
				}
			}

			// Add to template-specific constraints.
			for (AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint : instance
					.getGroupedConstraints(duty.getTemplate().getDay(), duty.getTemplate()))
			{
				if (constraint.containsColumn(column))
				{
					double dual = useSeparationDuals ? separationDuals.get(constraint) : currentDuals.get(constraint);
					double coefficient = constraint.getCoefficient(column);
					reducedCost -= dual * coefficient;
				}
			}
		}
		return reducedCost;
	}
}