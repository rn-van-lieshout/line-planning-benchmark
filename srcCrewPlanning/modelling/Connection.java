package modelling;

import java.util.List;

/**
 * Simple class that models a connection between trips, stores meal break
 * information, and contains cost information.
 * 
 * @author Bart van Rossum
 *
 */
public class Connection
{
	public enum ConnectionType
	{
		SOURCE("SOURCE"), SINK("SINK"), CONNECTION("CONNECTION");

		public String name;

		private ConnectionType(String name)
		{
			this.name = name;
		} 
	}

	private final int index;
	private final ConnectionType connectionType;
	private final int startBlock;
	private final int endBlock;
	private final int timeBeforeBreak;
	private final int timeAfterBreak;
	private final int duration;
	private final int cost;

	private final int refWorkingTime;
	private final int typeATime;
	private final int aggressionTime;
	private final int doubleDeckTime;

	private final List<Integer> passengerTasks;

	public Connection(int index, ConnectionType connectionType, int startBlock, int endBlock, int timeBeforeBreak,
			int timeAfterBreak, int duration, int cost, int refWorkingTime, int typeATime, int aggressionTime,
			int doubleDeckTime, List<Integer> passengerTasks)
	{
		this.index = index;
		this.connectionType = connectionType;
		this.startBlock = startBlock;
		this.endBlock = endBlock;
		this.timeBeforeBreak = timeBeforeBreak;
		this.timeAfterBreak = timeAfterBreak;
		this.duration = duration;
		this.cost = cost;

		this.refWorkingTime = refWorkingTime;
		this.typeATime = typeATime;
		this.aggressionTime = aggressionTime;
		this.doubleDeckTime = doubleDeckTime;

		this.passengerTasks = passengerTasks;
	}

	public int getIndex()
	{
		return index;
	}

	public ConnectionType getConnectionType()
	{
		return connectionType;
	}

	public int getStartBlock()
	{
		return startBlock;
	}

	public int getEndBlock()
	{
		return endBlock;
	}

	public int getDuration()
	{
		return duration;
	}

	public boolean hasBreak()
	{
		return timeBeforeBreak >= 0;
	}

	public int getTimeBeforeBreak()
	{
		return timeBeforeBreak;
	}

	public int getTimeAfterBreak()
	{
		return timeAfterBreak;
	}

	public int getCost()
	{
		return cost;
	}

	public int getRefWorkingTime()
	{
		return refWorkingTime;
	}

	public int getTypeATime()
	{
		return typeATime;
	}

	public int getAggressionTime()
	{
		return aggressionTime;
	}

	public int getDoubleDeckTime()
	{
		return doubleDeckTime;
	}

	public List<Integer> getPassengerTasks()
	{
		return passengerTasks;
	}

	@Override
	public String toString()
	{
		String string = index + " " + connectionType.name() + " " + startBlock + " " + endBlock + " "
				+ timeBeforeBreak + " " + timeAfterBreak + " " + duration + " " + cost + " " + refWorkingTime
				+ " " + typeATime + " " + aggressionTime + " " + doubleDeckTime;
		for (int passengerTask : passengerTasks)
		{
			string += " " + passengerTask;
		}
		return string;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + aggressionTime;
		result = prime * result + ((connectionType == null) ? 0 : connectionType.hashCode());
		result = prime * result + cost;
		result = prime * result + doubleDeckTime;
		result = prime * result + duration;
		result = prime * result + endBlock;
		result = prime * result + index;
		result = prime * result + ((passengerTasks == null) ? 0 : passengerTasks.hashCode());
		result = prime * result + refWorkingTime;
		result = prime * result + startBlock;
		result = prime * result + timeAfterBreak;
		result = prime * result + timeBeforeBreak;
		result = prime * result + typeATime;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Connection other = (Connection) obj;
		if (aggressionTime != other.aggressionTime) return false;
		if (connectionType != other.connectionType) return false;
		if (cost != other.cost) return false;
		if (doubleDeckTime != other.doubleDeckTime) return false;
		if (duration != other.duration) return false;
		if (endBlock != other.endBlock) return false;
		if (index != other.index) return false;
		if (passengerTasks == null)
		{
			if (other.passengerTasks != null) return false;
		}
		else if (!passengerTasks.equals(other.passengerTasks)) return false;
		if (refWorkingTime != other.refWorkingTime) return false;
		if (startBlock != other.startBlock) return false;
		if (timeAfterBreak != other.timeAfterBreak) return false;
		if (timeBeforeBreak != other.timeBeforeBreak) return false;
		if (typeATime != other.typeATime) return false;
		return true;
	}
}