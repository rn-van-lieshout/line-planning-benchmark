package operationalScheduling.sweetAndSour.attributes;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Constants;
import modelling.Duty;
import modelling.Task;
import operationalScheduling.columnGeneration.OperationalPricingProblem;
import scheduling.ArcTime;
import util.Configuration;

public class DutyLength extends DutyAverageAttribute
{
	public DutyLength()
	{
		super("dutyLength", 0, Constants.MAXIMUM_DUTY_LENGTH, true,
				Configuration.getConfiguration().getIntProperty("MAX_AVERAGE_DUTY_LENGTH"));
	}

	@Override
	public double evaluateDuty(Duty duty)
	{
		return duty.getWorkload();
	}

	@Override
	public void modifyPricingProblem(OperationalPricingProblem pricingProblem, double score)
	{
		// Decompose the penalty on source and sink arcs, such that each path incurs a
		// penalty of (duration / bound) * score.
		PricingGraph graph = pricingProblem.getPricingGraph();
		for (DirectedGraphArc<Task, Connection> sourceArc : graph.getOutArcs(graph.getSourceNode()))
		{
			double weight = (ArcTime.getStartTime(sourceArc)) / bound;
			sourceArc.addCost(-weight * score, 1);
		}
		for (DirectedGraphArc<Task, Connection> sinkArc : graph.getInArcs(graph.getSinkNode()))
		{
			double weight = (ArcTime.getEndTime(sinkArc)) / bound;
			sinkArc.addCost(weight * score, 1);
		}
	}
}
