package linePlanning.instance;

import java.util.ArrayList;
import java.util.List;

import modelling.Station;

public class Path
{
	private final ODPair ODPair;
	private final List<CGNArc> arcs;
	private final double length;

	public Path(ODPair ODPair, List<CGNArc> arcs, double length)
	{
		this.ODPair = ODPair;
		this.arcs = arcs;
		this.length = length;
	}

	public ODPair getODPair()
	{
		return ODPair;
	}

	public List<CGNArc> getArcs()
	{
		return arcs;
	}

	public List<Station> getStations()
	{
		List<Station> stations = new ArrayList<>();
		for (CGNArc arc : arcs)
		{
			if (arc instanceof TravelArc)
			{
				TravelArc travelArc = (TravelArc) arc;
				if (!stations.contains(travelArc.getFrom()))
				{
					stations.add(travelArc.getFrom());
				}
				if (!stations.contains(travelArc.getTo()))
				{
					stations.add(travelArc.getTo());
				}
			}
		}
		return stations;
	}

	public int getNumberOfTransfers()
	{
		int count = 0;
		for (CGNArc arc : arcs)
		{
			if (arc instanceof TransferArc)
			{
				count++;
			}
		}
		return count;
	}

	public double getLength()
	{
		return length;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ODPair == null) ? 0 : ODPair.hashCode());
		result = prime * result + ((arcs == null) ? 0 : arcs.hashCode());
		long temp;
		temp = Double.doubleToLongBits(length);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Path other = (Path) obj;
		if (ODPair == null)
		{
			if (other.ODPair != null) return false;
		}
		else if (!ODPair.equals(other.ODPair)) return false;
		if (arcs == null)
		{
			if (other.arcs != null) return false;
		}
		else if (!arcs.equals(other.arcs)) return false;
		if (Double.doubleToLongBits(length) != Double.doubleToLongBits(other.length)) return false;
		return true;
	}
}
