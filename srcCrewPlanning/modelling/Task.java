package modelling;

import java.time.LocalDate;

import graph.structures.digraph.DirectedGraphNodeIndex;

public class Task extends DirectedGraphNodeIndex
{
	private final int index;
	private boolean cover;
	private final LocalDate day;
	private final int startTime;
	private final int endTime;
	private final Station startStation;
	private final Station endStation;

	private final int refWorkingTime;
	private final int typeATime;
	private final int aggressionTime;
	private final int doubleDeckTime;

	public Task(int index, boolean cover, LocalDate day, int startTime, int endTime, Station startStation,
			Station endStation, int refWorkingTime, int typeATime, int aggressionTime, int doubleDeckTime)
	{
		this.index = index;
		this.cover = cover;
		this.day = day;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startStation = startStation;
		this.endStation = endStation;

		this.refWorkingTime = refWorkingTime;
		this.typeATime = typeATime;
		this.aggressionTime = aggressionTime;
		this.doubleDeckTime = doubleDeckTime;
	}

	public int getIndex()
	{
		return index;
	}

	public boolean cover()
	{
		return cover;
	}

	public void setCover(boolean cover)
	{
		this.cover = cover;
	}

	public LocalDate getDay()
	{
		return day;
	}

	public int getStartTime()
	{
		return startTime;
	}

	public int getEndTime()
	{
		return endTime;
	}

	public Station getStartStation()
	{
		return startStation;
	}

	public Station getEndStation()
	{
		return endStation;
	}

	public int getDuration()
	{
		return endTime - startTime;
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

	@Override
	public String toString()
	{
		String string = index + " " + cover + " " + day + " " + startTime + " " + endTime + " "
				+ startStation.getIndex() + " " + endStation.getIndex() + " " + refWorkingTime + " " + typeATime
				+ " " + aggressionTime + " " + doubleDeckTime + " ";
		return string;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + aggressionTime;
		result = prime * result + (cover ? 1231 : 1237);
		result = prime * result + ((day == null) ? 0 : day.hashCode());
		result = prime * result + doubleDeckTime;
		result = prime * result + ((endStation == null) ? 0 : endStation.hashCode());
		result = prime * result + endTime;
		result = prime * result + index;
		result = prime * result + refWorkingTime;
		result = prime * result + ((startStation == null) ? 0 : startStation.hashCode());
		result = prime * result + startTime;
		result = prime * result + typeATime;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Task other = (Task) obj;
		if (aggressionTime != other.aggressionTime) return false;
		if (cover != other.cover) return false;
		if (day == null)
		{
			if (other.day != null) return false;
		}
		else if (!day.equals(other.day)) return false;
		if (doubleDeckTime != other.doubleDeckTime) return false;
		if (endStation == null)
		{
			if (other.endStation != null) return false;
		}
		else if (!endStation.equals(other.endStation)) return false;
		if (endTime != other.endTime) return false;
		if (index != other.index) return false;
		if (refWorkingTime != other.refWorkingTime) return false;
		if (startStation == null)
		{
			if (other.startStation != null) return false;
		}
		else if (!startStation.equals(other.startStation)) return false;
		if (startTime != other.startTime) return false;
		if (typeATime != other.typeATime) return false;
		return true;
	}
}
