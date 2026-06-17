package scheduling;

import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import modelling.Connection.ConnectionType;

public class ArcTime
{
	public static int getStartTime(Task startTask, Task endTask, Connection connection)
	{
		if (connection.getConnectionType().equals(ConnectionType.SOURCE))
		{
			return endTask.getEndTime() - connection.getDuration();
		}
		return startTask.getEndTime();
	}

	public static int getStartTime(DirectedGraphArc<Task, Connection> arc)
	{
		if (arc.getData().getConnectionType().equals(ConnectionType.SOURCE))
		{
			return arc.getTo().getEndTime() - arc.getData().getDuration();
		}
		return arc.getFrom().getEndTime();
	}

	public static int getEndTime(Task startTask, Task endTask, Connection connection)
	{
		if (connection.getConnectionType().equals(ConnectionType.SINK))
		{
			return startTask.getEndTime() + connection.getDuration();
		}
		return endTask.getEndTime();
	}

	public static int getEndTime(DirectedGraphArc<Task, Connection> arc)
	{
		if (arc.getData().getConnectionType().equals(ConnectionType.SINK))
		{
			return arc.getFrom().getEndTime() + arc.getData().getDuration();
		}
		return arc.getTo().getEndTime();
	}
}
