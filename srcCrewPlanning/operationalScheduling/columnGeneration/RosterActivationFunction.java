package operationalScheduling.columnGeneration;

import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.DirectedGraphNodeIndex;
import graph.structures.digraph.activation.ActivationFunction;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.Task;
import modelling.Template;
import modelling.constraints.rosterConstraints.ForwardRotation;
import modelling.constraints.rosterConstraints.MinimumRestTime;
import scheduling.ArcTime;

public class RosterActivationFunction implements ActivationFunction
{
	private final Template template;
	private int minStartTime;
	private int maxEndTime;

	public RosterActivationFunction(Template template, DutyRoster roster)
	{
		this.template = template;
		this.minStartTime = template.getStartTime();
		this.maxEndTime = template.getEndTime();

		if (!roster.hasDuty(template.getDay().minusDays(1)))
		{
			return;
		}

		// Retrieve duty of the previous day.
		Duty previousDuty = roster.getDuty(template.getDay().minusDays(1));

		// Forward rotation constraint.
		minStartTime = ForwardRotation.minimumStartTime(previousDuty, template);
		maxEndTime = ForwardRotation.maximumEndTime(previousDuty, template);

		// Rest time constraint.
		minStartTime = Math.max(minStartTime, MinimumRestTime.minimumStartTime(previousDuty, template));
	}

	@Override
	public boolean isActiveNode(DirectedGraphNodeIndex node)
	{
		Task task = (Task) node;

		// Task is a source or sink node.
		if (task.getIndex() < 0)
		{
			return true;
		}

		// Task should fall within template times.
		return (task.getStartTime() >= minStartTime && task.getEndTime() <= maxEndTime);
	}

	@Override
	public boolean isActiveArc(DirectedGraphArc<?, ?> arc)
	{
		@SuppressWarnings("unchecked")
		DirectedGraphArc<Task, Connection> connection = (DirectedGraphArc<Task, Connection>) arc;
		if (connection.getData().getConnectionType().equals(ConnectionType.SOURCE))
		{
			if (connection.getData().getStartBlock() != template.getCrewBase().getIndex())
			{
				return false;
			}
		}
		if (connection.getData().getConnectionType().equals(ConnectionType.SINK))
		{
			if (connection.getData().getEndBlock() != template.getCrewBase().getIndex())
			{
				return false;
			}
		}
		return (ArcTime.getStartTime(connection) >= minStartTime
				&& ArcTime.getEndTime(connection) <= maxEndTime);
	}
}
