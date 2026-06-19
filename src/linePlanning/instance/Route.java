package linePlanning.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import linePlanning.CGN.CGNTransferArc;
import linePlanning.columnGeneration.pricing.NetworkArc;

public class Route
{
	private final Station origin;
	private final Station destination;

	protected final List<NetworkArc> arcs;
	protected final double duration;

	public Route(Station origin, Station destination, List<? extends NetworkArc> arcs, double duration)
	{
		super();

		this.origin = origin;
		this.destination = destination;
		this.arcs = new ArrayList<>(arcs);
		this.duration = duration;
	}

	public Station getOrigin()
	{
		return origin;
	}

	public Station getDestination()
	{
		return destination;
	}

	public List<NetworkArc> getArcs()
	{
		return arcs;
	}

	

	@Override
	public String toString() {
		return "Route [arcs=" + arcs + ", duration=" + duration + "]";
	}

	public int getNrTransfers() {
		int transfers = 0;
		for(NetworkArc arc: arcs) {
			if(arc instanceof CGNTransferArc) {
				transfers++;
			}
		}
		return transfers / 2;
	}

	public double getDuration()
	{
		return duration;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(arcs, destination, duration, origin);
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
		Route other = (Route) obj;
		return Objects.equals(arcs, other.arcs) && Objects.equals(destination, other.destination)
				&& Double.doubleToLongBits(duration) == Double.doubleToLongBits(other.duration)
				&& Objects.equals(origin, other.origin);
	}
}
