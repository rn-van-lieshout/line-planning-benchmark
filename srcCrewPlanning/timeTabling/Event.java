package timeTabling;

import graph.structures.digraph.DirectedGraphNodeIndex;
import linePlanning.instance.Line;
import modelling.Station;

public class Event extends DirectedGraphNodeIndex
{
	private final Line line;
	private final Station station;
	private final boolean isArrival; // if false, it is a departure
	private final int interval; // the event occurs every interval time periods
	private final int id;

	public Event(Line l, Station s, boolean isArrival, int interval, int id)
	{
		super();
		this.line = l;
		this.station = s;
		this.isArrival = isArrival;
		this.interval = interval;
		this.id = id;
	}

	public Line getLine()
	{
		return line;
	}

	public Station getStation()
	{
		return station;
	}

	public boolean isArrival()
	{
		return isArrival;
	}

	public int getInterval()
	{
		return interval;
	}

	@Override
	public String toString()
	{
		return "Event " + id;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + interval;
		result = prime * result + (isArrival ? 1231 : 1237);
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		result = prime * result + ((station == null) ? 0 : station.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Event other = (Event) obj;
		if (id != other.id) return false;
		if (interval != other.interval) return false;
		if (isArrival != other.isArrival) return false;
		if (line == null)
		{
			if (other.line != null) return false;
		}
		else if (!line.equals(other.line)) return false;
		if (station == null)
		{
			if (other.station != null) return false;
		}
		else if (!station.equals(other.station)) return false;
		return true;
	}
}
