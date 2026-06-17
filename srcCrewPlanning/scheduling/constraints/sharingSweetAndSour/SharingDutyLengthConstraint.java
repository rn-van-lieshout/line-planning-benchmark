package scheduling.constraints.sharingSweetAndSour;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import scheduling.ArcTime;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class SharingDutyLengthConstraint extends AbstractSharingConstraint
{
	public SharingDutyLengthConstraint(Station crewBase)
	{
		super(ConstraintType.LESSER, 0, Configuration.getConfiguration().getDoubleProperty("THRESHOLD_REDUCTION")
				* Configuration.getConfiguration().getDoubleProperty("MAX_AVERAGE_DUTY_LENGTH"),
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
		coefficient += (double) duty.getWorkload();
		coefficient -= (double) threshold;
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
		for (DirectedGraphArc<Task, Connection> sourceArc : graph.getOutArcs(graph.getSourceNode()))
		{
			sourceArc.addDual((-ArcTime.getStartTime(sourceArc) - threshold) * dual, 1);
		}
		for (DirectedGraphArc<Task, Connection> sinkArc : graph.getInArcs(graph.getSinkNode()))
		{
			sinkArc.addDual(ArcTime.getEndTime(sinkArc) * dual, 1);
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
