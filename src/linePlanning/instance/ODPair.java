package linePlanning.instance;

import java.util.Objects;

public class ODPair
{
	private final Station origin;
	private final Station destination;
	private final double volume;

	private StationODNode originNode;
	private StationODNode destinationNode;

	public ODPair(Station origin, Station destination, double volume)
	{
		this.origin = origin;
		this.destination = destination;
		this.volume = volume;
		if (origin.equals(destination))
		{
			throw new IllegalArgumentException("Origin should be different from destination");
		}
	}

	public Station getOrigin()
	{
		return origin;
	}

	public Station getDestination()
	{
		return destination;
	}

	public StationODNode getOriginNode()
	{
		return originNode;
	}

	public void setOriginNode(StationODNode originNode)
	{
		this.originNode = originNode;
	}

	public StationODNode getDestinationNode()
	{
		return destinationNode;
	}

	public void setDestinationNode(StationODNode destinationNode)
	{
		this.destinationNode = destinationNode;
	}

	public double getVolume()
	{
		return volume;
	}

	@Override
	public String toString()
	{
		String result = origin.getName() + " " + destination.getName() + " " + volume;
		return result;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(destination, origin, volume);
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
		ODPair other = (ODPair) obj;
		return Objects.equals(destination, other.destination) && Objects.equals(origin, other.origin)
				&& Double.doubleToLongBits(volume) == Double.doubleToLongBits(other.volume);
	}

}
