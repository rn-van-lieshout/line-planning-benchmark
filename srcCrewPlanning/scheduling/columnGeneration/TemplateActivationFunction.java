package scheduling.columnGeneration;

import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.DirectedGraphNodeIndex;
import graph.structures.digraph.activation.ActivationFunction;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Task;
import modelling.Template;
import scheduling.ArcTime;

public class TemplateActivationFunction implements ActivationFunction
{
	private final Template template;

	public TemplateActivationFunction(Template template)
	{
		this.template = template;
	}

	public Template getTemplate()
	{
		return template;
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
		return (task.getStartTime() >= template.getStartTime() && task.getEndTime() <= template.getEndTime());
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
			if (ArcTime.getStartTime(connection) > template.getLatestStartTime())
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
			if (ArcTime.getEndTime(connection) < template.getEarliestEndTime())
			{
				return false;
			}
		}
		return (ArcTime.getStartTime(connection) >= template.getStartTime()
				&& ArcTime.getEndTime(connection) <= template.getEndTime());
	}
}
