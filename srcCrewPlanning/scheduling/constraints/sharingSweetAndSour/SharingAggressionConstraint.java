package scheduling.constraints.sharingSweetAndSour;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class SharingAggressionConstraint extends AbstractSharingConstraint
{
	public SharingAggressionConstraint(Station crewBase)
	{
		super(ConstraintType.LESSER, 0, Configuration.getConfiguration().getDoubleProperty("THRESHOLD_REDUCTION")
				* Configuration.getConfiguration().getDoubleProperty("MAX_PERCENTAGE_AGGRESSION"),
				crewBase);
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			return c.getDuty().getTemplate().getCrewBase().equals(crewBase);
		}
		return false;
	}

	@Override
	public double getCoefficient(SchedulingColumn column)
	{
		double coefficient = 0;
		Duty duty = ((DutyColumn) column).getDuty();
		for (Task task : duty.getTasks())
		{
			coefficient += task.getAggressionTime();
			coefficient -= (double) threshold * task.getRefWorkingTime();
		}
		return coefficient;
	}
	
	@Override
	public void updateGenericDuals(SchedulingInstance instance, double dual)
	{
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(PricingProblem pricingProblem, double dual)
	{
		if (!pricingProblem.getTemplate().getCrewBase().equals(crewBase))
		{
			return;
		}

		PricingGraph graph = pricingProblem.getPricingGraph();
		for (DirectedGraphArc<Task, Connection> arc : graph.getArcs())
		{
			Task task = arc.getTo();
			if (!task.cover())
			{
				continue;
			}
			double weight = (double) task.getAggressionTime()
					- (double) threshold * task.getRefWorkingTime();
			arc.addDual(weight * dual, 1);
		}
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		return true;
	}
}
