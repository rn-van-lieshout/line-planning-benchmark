package scheduling.constraints;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import modelling.Template;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.ArcTime;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class EarlyRotationConstraint extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Template firstDayTemplate;
	private final Template secondDayTemplate;
	private final int startTime;

	public EarlyRotationConstraint(Template firstDayTemplate, Template secondDayTemplate, int startTime)
	{
		super(ConstraintType.LESSER, 1);

		this.firstDayTemplate = firstDayTemplate;
		this.secondDayTemplate = secondDayTemplate;
		this.startTime = startTime;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			Template template = c.getDuty().getTemplate();
			return (template.equals(firstDayTemplate) && c.getDuty().getStartTime() >= startTime)
					|| (template.equals(secondDayTemplate) && c.getDuty().getStartTime() <= startTime);
		}
		return false;
	}

	@Override
	public double getCoefficient(SchedulingColumn column)
	{
		return 1;
	}
	
	@Override
	public void updateGenericDuals(SchedulingInstance instance, double dual)
	{
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(PricingProblem pricingProblem, double dual)
	{
		PricingGraph pricingGraph = pricingProblem.getPricingGraph();
		if (pricingProblem.getTemplate().equals(firstDayTemplate))
		{
			for (DirectedGraphArc<Task, Connection> sourceArc : pricingGraph.getOutArcs(pricingGraph.getSourceNode()))
			{
				if (ArcTime.getStartTime(sourceArc) >= startTime)
				{
					sourceArc.addDual(dual, 1);
				}
			}
		}
		if (pricingProblem.getTemplate().equals(secondDayTemplate))
		{
			for (DirectedGraphArc<Task, Connection> sourceArc : pricingGraph.getOutArcs(pricingGraph.getSourceNode()))
			{
				if (ArcTime.getStartTime(sourceArc) <= startTime)
				{
					sourceArc.addDual(dual, 1);
				}
			}
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null)
		{
			return false;
		}
		if (!(o instanceof EarlyRotationConstraint))
		{
			return false;
		}
		EarlyRotationConstraint constraint = (EarlyRotationConstraint) o;
		return constraint.firstDayTemplate.equals(firstDayTemplate)
				&& constraint.secondDayTemplate.equals(secondDayTemplate)
				&& constraint.startTime == startTime;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 17;
		result = prime * result + ((firstDayTemplate == null) ? 0 : firstDayTemplate.hashCode());
		result = prime * result + ((secondDayTemplate == null) ? 0 : secondDayTemplate.hashCode());
		result = prime * result + startTime;
		return result;
	}
}
