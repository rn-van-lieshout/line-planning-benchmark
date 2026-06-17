package linePlanning.instance;

import java.util.Objects;

import graph.structures.digraph.DirectedGraphNodeIndex;

public class StationNode extends DirectedGraphNodeIndex
{
	protected final Station station;

	public StationNode(Station station)
	{
		this.station = station;

		if (station == null)
		{
			throw new Error("Station is null");
		}
	}

	public Station getStation()
	{
		return station;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(station);
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
		StationNode other = (StationNode) obj;
		return Objects.equals(station, other.station);
	}

	@Override
	public String toString()
	{
		return "StationNode [station=" + station.getNameFull() + "]";
	}
}
