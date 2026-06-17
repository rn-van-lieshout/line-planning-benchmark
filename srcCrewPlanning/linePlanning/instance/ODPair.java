package linePlanning.instance;

public class ODPair
{
	private final StationODNode origin;
	private final StationODNode destination;
	private final double volume;

	public ODPair(StationODNode origin, StationODNode destination, double volume)
	{
		this.origin = origin;
		this.destination = destination;
		this.volume = volume;
	}

	public StationODNode getOrigin()
	{
		return origin;
	}

	public StationODNode getDestination()
	{
		return destination;
	}

	public double getVolume()
	{
		return volume;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + ((origin == null) ? 0 : origin.hashCode());
		long temp;
		temp = Double.doubleToLongBits(volume);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ODPair other = (ODPair) obj;
		if (destination == null)
		{
			if (other.destination != null) return false;
		}
		else if (!destination.equals(other.destination)) return false;
		if (origin == null)
		{
			if (other.origin != null) return false;
		}
		else if (!origin.equals(other.origin)) return false;
		if (Double.doubleToLongBits(volume) != Double.doubleToLongBits(other.volume)) return false;
		return true;
	}
}
