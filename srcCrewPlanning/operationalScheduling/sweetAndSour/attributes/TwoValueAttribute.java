package operationalScheduling.sweetAndSour.attributes;

import java.util.function.Function;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Duty;
import modelling.Task;
import operationalScheduling.columnGeneration.OperationalPricingProblem;
import util.Configuration;

public abstract class TwoValueAttribute extends Attribute
{
	protected final Function<Task, Integer> taskFunction;
	protected final Function<Connection, Integer> connectionFunction;

	public TwoValueAttribute(String name, boolean isUpperBound, double bound, Function<Task, Integer> taskFunction,
			Function<Connection, Integer> connectionFunction)
	{
		super(name, 0.0, 1.0, isUpperBound, bound);

		this.taskFunction = taskFunction;
		this.connectionFunction = connectionFunction;
	}

	@Override
	public double evaluateDuty(Duty duty)
	{
		int refWorkingTime = 0;
		int attributeTime = 0;
		for (Task task : duty.getTasks())
		{
			refWorkingTime += task.getRefWorkingTime();
			attributeTime += taskFunction.apply(task);
		}
//		for (Connection connection : duty.getConnections())
//		{
//			refWorkingTime += connection.getRefWorkingTime();
//			attributeTime += connectionFunction.apply(connection);
//		}
		return (refWorkingTime > 0) ? (double) attributeTime / refWorkingTime : 0;
	}

	@Override
	public int getAttributeVectorLength()
	{
		return 2;
	}

	@Override
	public void addDuty(Duty duty, AttributeVector attributeVector)
	{
		attributeVector.increaseNumDuties();

		int refWorkingTime = 0;
		int attributeTime = 0;
		for (Task task : duty.getTasks())
		{
			refWorkingTime += task.getRefWorkingTime();
			attributeTime += taskFunction.apply(task);
		}
//		for (Connection connection : duty.getConnections())
//		{
//			refWorkingTime += connection.getRefWorkingTime();
//			attributeTime += connectionFunction.apply(connection);
//		}
		attributeVector.increaseValue(0, refWorkingTime);
		attributeVector.increaseValue(1, attributeTime);
	}

	@Override
	public void removeDuty(Duty duty, AttributeVector attributeVector)
	{
		attributeVector.decreaseNumDuties();

		int refWorkingTime = 0;
		int attributeTime = 0;
		for (Task task : duty.getTasks())
		{
			refWorkingTime += task.getRefWorkingTime();
			attributeTime += taskFunction.apply(task);
		}
//		for (Connection connection : duty.getConnections())
//		{
//			refWorkingTime += connection.getRefWorkingTime();
//			attributeTime += connectionFunction.apply(connection);
//		}
		attributeVector.decreaseValue(0, refWorkingTime);
		attributeVector.decreaseValue(1, attributeTime);
	}

	@Override
	public double evaluateAttributeVector(AttributeVector attributeVector)
	{
		if (attributeVector.getValue(0) == 0)
		{
			return isUpperBound ? 0.0 : 1.0;
		}
		return (double) attributeVector.getValue(1) / attributeVector.getValue(0);
	}

	@Override
	public void modifyPricingProblem(OperationalPricingProblem pricingProblem, double score)
	{
		// TODO: Also place penalties on connections?
		PricingGraph graph = pricingProblem.getPricingGraph();
		for (Task task : graph.getNodes())
		{
			if (task.getRefWorkingTime() <= 0)
			{
				continue;
			}
			double weight = (double) taskFunction.apply(task) / task.getRefWorkingTime();
			weight = isUpperBound ? weight : 1.0 - weight;
			weight *= (double) task.getDuration()
					/ Configuration.getConfiguration().getIntProperty("MAX_AVERAGE_DUTY_LENGTH");
			double penalty = weight * score;
			for (DirectedGraphArc<Task, Connection> inArc : graph.getInArcs(task))
			{
				inArc.addCost(penalty, 1);
			}
		}
	}
}
