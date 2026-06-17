package linePlanning.instance;

import java.util.ArrayList;
import java.util.List;

import modelling.Station;

public class Line
{
	private final int index;
	private final boolean isInterCity;
	private final List<Track> lineSegments;
	private final List<Station> stations;
	private final int cost;
	private final int maxFrequency;
	private final double capacity;
	private final Line reverseLine;

	public Line(int index, boolean isInterCity, List<Track> lineSegments, int cost, int maxFrequency, double capacity, Line reverseLine)
	{
		this.index = index;
		this.isInterCity = isInterCity;
		this.lineSegments = lineSegments;
		this.cost = cost;
		this.maxFrequency = maxFrequency;
		this.capacity = capacity;
		this.reverseLine = reverseLine;
		this.stations = new ArrayList<>();
		stations.add(lineSegments.get(0).getFrom());
		for (Track t : lineSegments)
		{
			stations.add(t.getTo());
		}
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public boolean isInterCity()
	{
		return isInterCity;
	}
	
	public List<Station> getStations()
	{
		return stations;
	}
	
	public List<Track> getLineSegments()
	{
		return lineSegments;
	}

	public int getCost()
	{
		return cost;
	}

	public int getMaxFrequency()
	{
		return maxFrequency;
	}

	public double getCapacity()
	{
		return capacity;
	}

	public boolean isReverseLine()
	{
		return reverseLine != null;
	}

	public Line getReverseLine()
	{
		return reverseLine;
	}

	public Line reverseLine(int reverseIndex)
	{
		List<Track> reverseLineSegments = new ArrayList<>();
		for (int i = lineSegments.size() - 1; i >= 0; i--)
		{
			Track track = lineSegments.get(i);
			Track reverseTrack = new Track(track.getTo(), track.getFrom(), track.getDuration());
			reverseLineSegments.add(reverseTrack);
		}
		return new Line(reverseIndex, isInterCity, reverseLineSegments, cost, maxFrequency, capacity, this);
	}

	public Station getStartStation()
	{
		return lineSegments.get(0).getFrom();
	}
	
	public Station getEndStation()
	{
		return lineSegments.get(lineSegments.size() - 1).getTo();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(capacity);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + cost;
		result = prime * result + index;
		result = prime * result + (isInterCity ? 1231 : 1237);
		result = prime * result + ((lineSegments == null) ? 0 : lineSegments.hashCode());
		result = prime * result + maxFrequency;
		result = prime * result + ((reverseLine == null) ? 0 : reverseLine.hashCode());
		result = prime * result + ((stations == null) ? 0 : stations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Line other = (Line) obj;
		if (Double.doubleToLongBits(capacity) != Double.doubleToLongBits(other.capacity)) return false;
		if (cost != other.cost) return false;
		if (index != other.index) return false;
		if (isInterCity != other.isInterCity) return false;
		if (lineSegments == null)
		{
			if (other.lineSegments != null) return false;
		}
		else if (!lineSegments.equals(other.lineSegments)) return false;
		if (maxFrequency != other.maxFrequency) return false;
		if (reverseLine == null)
		{
			if (other.reverseLine != null) return false;
		}
		else if (!reverseLine.equals(other.reverseLine)) return false;
		if (stations == null)
		{
			if (other.stations != null) return false;
		}
		else if (!stations.equals(other.stations)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "Line [IC: " + isInterCity + ", stations=" + stations + "]";
	}
}
