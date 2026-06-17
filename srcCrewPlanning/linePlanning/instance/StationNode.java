package linePlanning.instance;

import graph.structures.digraph.DirectedGraphNodeIndex;
import modelling.Station;

public class StationNode extends DirectedGraphNodeIndex
{
	private final Station station;
	
	public StationNode(Station station)
	{
		this.station = station;
	}
	
	public Station getStation()
	{
		return station;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((station == null) ? 0 : station.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		StationNode other = (StationNode) obj;
		if (station == null)
		{
			if (other.station != null) return false;
		}
		else if (!station.equals(other.station)) return false;
		return true;
	}
}
