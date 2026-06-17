package linePlanning.instance;

import modelling.Station;

public class TravelArc extends CGNArc
{
	private final Line line;
	private final Station from;
	private final Station to;
	private final int travelTime;

	public TravelArc(Line line, Station from, Station to, int travelTime)
	{
		this.line = line;
		this.from = from;
		this.to = to;
		this.travelTime = travelTime;
	}

	@Override
	public int getDuration()
	{
		return travelTime;
	}

	public double getCapacity()
	{
		return line.getCapacity();
	}

	public Line getLine()
	{
		return line;
	}

	public Station getFrom()
	{
		return from;
	}
	
	public Station getTo()
	{
		return to;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + travelTime;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TravelArc other = (TravelArc) obj;
		if (from == null)
		{
			if (other.from != null) return false;
		}
		else if (!from.equals(other.from)) return false;
		if (line == null)
		{
			if (other.line != null) return false;
		}
		else if (!line.equals(other.line)) return false;
		if (to == null)
		{
			if (other.to != null) return false;
		}
		else if (!to.equals(other.to)) return false;
		if (travelTime != other.travelTime) return false;
		return true;
	}
}
