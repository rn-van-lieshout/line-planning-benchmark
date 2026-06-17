package linePlanning.instance;

import java.util.Objects;

public class Track
{
	private final int index;
	private final Station from;
	private final Station to;
	private final double length;
	private final int speed;
	private final double duration;
	private final int nrOfTracks;

	public Track(int index, Station from, Station to, double length, int speed, double duration, int nrOfTracks)
	{
		this.index = index;
		this.from = from;
		this.to = to;
		this.length = length;
		this.speed = speed;
		this.duration = duration;
		this.nrOfTracks = nrOfTracks;
		if (from.equals(to))
		{
			throw new Error();
		}
	}

	public int getIndex()
	{
		return index;
	}

	public Station getFrom()
	{
		return from;
	}

	public Station getTo()
	{
		return to;
	}

	public double getLength()
	{
		return length;
	}

	public int getSpeed()
	{
		return speed;
	}

	public double getDuration()
	{
		return duration;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(index);
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
		Track other = (Track) obj;
		return Double.doubleToLongBits(duration) == Double.doubleToLongBits(other.duration)
				&& Objects.equals(from, other.from) && index == other.index
				&& Double.doubleToLongBits(length) == Double.doubleToLongBits(other.length)
				&& nrOfTracks == other.nrOfTracks && speed == other.speed && Objects.equals(to, other.to);
	}

	@Override
	public String toString()
	{
		String result = index + " " + from.getName() + " " + to.getName() + " " + length + " " + speed + " " + duration
				+ " " + nrOfTracks;
		return result;
	}

	public int getNrOfTracks()
	{
		return nrOfTracks;
	}

	public int getMaxFrequency(int type, boolean allowOtherType)
	{
		if (nrOfTracks == 1)
		{
			return 2;
		}
		if (nrOfTracks == 2)
		{
			if (!allowOtherType)
			{
				return 10;
			}
			else
			{
				return 5;
			}
		}
		if (nrOfTracks == 4)
		{
			if (!allowOtherType)
			{
				return 20;
			}
			else
			{
				return 10;
			}
		}
		if (nrOfTracks == 8)
		{
			return 20;
		}
		throw new Error("Unexpected nr of tracks");
	}
}