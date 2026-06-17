package linePlanning.instance;

import modelling.Station;

public class Track
{
	private final Station from;
	private final Station to;
	private final int duration; 

	public Track(Station from, Station to, int duration)
	{
		this.from = from;
		this.to = to;
		this.duration = duration;
	}

	public Station getFrom()
	{
		return from;
	}

	public Station getTo()
	{
		return to;
	}

	public int getDuration()
	{
		return duration;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + duration;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Track other = (Track) obj;
		if (duration != other.duration) return false;
		if (from == null)
		{
			if (other.from != null) return false;
		}
		else if (!from.equals(other.from)) return false;
		if (to == null)
		{
			if (other.to != null) return false;
		}
		else if (!to.equals(other.to)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Track [" + from + "-" + to + "]";
	}
}