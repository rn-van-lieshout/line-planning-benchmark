package modelling;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Duty
{
	protected final int index;
	protected final int crewID;
	protected final Template template;
	protected final int startTime;
	protected final int endTime;
	protected final int cost;
	protected final List<Connection> connections;
	protected final Set<Task> tasks;

	public Duty(int index, int crewID, Template template, int startTime, int endTime, int cost,
			List<Connection> connections, Set<Task> tasks)
	{
		this.index = index;
		this.crewID = crewID;
		this.template = template;
		this.startTime = startTime;
		this.endTime = endTime;
		this.cost = cost;
		this.connections = connections;
		this.tasks = tasks;
	}

	public int getIndex()
	{
		return index;
	}

	public int getCrewID()
	{
		return crewID;
	}

	public Template getTemplate()
	{
		return template;
	}

	public int getStartTime()
	{
		return startTime;
	}

	public int getEndTime()
	{
		return endTime;
	}

	public int getWorkload()
	{
		return endTime - startTime;
	}

	public int getWorkedTime()
	{
		int workedTime = 0;
		for (Task task : tasks)
		{
			workedTime += task.getRefWorkingTime();
		}
		return workedTime;
	}

	public int getCost()
	{
		return cost;
	}

	public List<Connection> getConnections()
	{
		return Collections.unmodifiableList(connections);
	}

	public Set<Task> getTasks()
	{
		return Collections.unmodifiableSet(tasks);
	}

	public String toTaskString()
	{
		String result = index + " " + crewID + " " + template.getIndex() + " " + startTime + " " + endTime + " " + cost;
		for (Task task : tasks)
		{
			result += " " + task.getIndex();
		}
		return result;
	}

	public String printConnections()
	{
		String result = "";
		for (int i = 0; i < connections.size(); i++)
		{
			Connection connection = connections.get(i);
			if (i == 0)
			{
				result += connection.getIndex();
			}
			else
			{
				result += " " + connection.getIndex();
			}
		}
		return result;
	}

	@Override
	public String toString()
	{
		String result = index + " " + crewID + " " + template.getIndex() + " " + startTime + " " + endTime + " " + cost;
		for (Connection connection : connections)
		{
			result += " " + connection.getIndex();
		}
		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connections == null) ? 0 : connections.hashCode());
		result = prime * result + cost;
		result = prime * result + crewID;
		result = prime * result + endTime;
		result = prime * result + index;
		result = prime * result + startTime;
		result = prime * result + ((tasks == null) ? 0 : tasks.hashCode());
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Duty other = (Duty) obj;
		if (connections == null)
		{
			if (other.connections != null)
				return false;
		}
		else
			if (!connections.equals(other.connections))
				return false;
		if (cost != other.cost)
			return false;
		if (crewID != other.crewID)
			return false;
		if (endTime != other.endTime)
			return false;
		if (index != other.index)
			return false;
		if (startTime != other.startTime)
			return false;
		if (tasks == null)
		{
			if (other.tasks != null)
				return false;
		}
		else
			if (!tasks.equals(other.tasks))
				return false;
		if (template == null)
		{
			if (other.template != null)
				return false;
		}
		else
			if (!template.equals(other.template))
				return false;
		return true;
	}
}
