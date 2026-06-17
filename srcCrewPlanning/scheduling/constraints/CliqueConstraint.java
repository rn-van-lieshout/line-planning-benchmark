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

public class CliqueConstraint extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Template firstDayTemplate;
	private final Template secondDayTemplate;
	private final int endTimeFirstDay;
	private final int startTimeSecondDay;

	public CliqueConstraint(Template firstDayTemplate, Template secondDayTemplate, int endTimeFirstDay,
			int startTimeSecondDay)
	{
		super(ConstraintType.LESSER, 1);

		this.firstDayTemplate = firstDayTemplate;
		this.secondDayTemplate = secondDayTemplate;
		this.endTimeFirstDay = endTimeFirstDay;
		this.startTimeSecondDay = startTimeSecondDay;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			Template template = dutyColumn.getDuty().getTemplate();
			if (template.equals(firstDayTemplate) && dutyColumn.getDuty().getEndTime() >= endTimeFirstDay)
			{
				return true;
			}
			if (template.equals(secondDayTemplate) && dutyColumn.getDuty().getStartTime() <= startTimeSecondDay)
			{
				return true;
			}
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
			for (DirectedGraphArc<Task, Connection> sinkArc : pricingGraph.getInArcs(pricingGraph.getSinkNode()))
			{
				if (ArcTime.getEndTime(sinkArc) >= endTimeFirstDay)
				{
					sinkArc.addDual(dual, 1);
				}
			}
		}
		if (pricingProblem.getTemplate().equals(secondDayTemplate))
		{
			for (DirectedGraphArc<Task, Connection> sourceArc : pricingGraph.getOutArcs(pricingGraph.getSourceNode()))
			{
				if (ArcTime.getStartTime(sourceArc) <= startTimeSecondDay)
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
		if (!(o instanceof CliqueConstraint))
		{
			return false;
		}
		CliqueConstraint constraint = (CliqueConstraint) o;
		return constraint.firstDayTemplate.equals(firstDayTemplate)
				&& constraint.secondDayTemplate.equals(secondDayTemplate)
				&& constraint.endTimeFirstDay == endTimeFirstDay && constraint.startTimeSecondDay == startTimeSecondDay;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 17;
		result = prime * result + ((firstDayTemplate == null) ? 0 : firstDayTemplate.hashCode());
		result = prime * result + ((secondDayTemplate == null) ? 0 : secondDayTemplate.hashCode());
		result = prime * result + endTimeFirstDay;
		result = prime * result + startTimeSecondDay;
		return result;
	}
}
