package linePlanning.instance;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import modelling.Station;
import optimisation.columnGeneration.AbstractInstance;

public class LinePlanningInstance extends AbstractInstance
{
	private final DirectedGraph<StationNode, Track> network;
	private final List<ODPair> ODPairs;
	private final double totNrPax;
	private final Set<Line> linePool;
	private final CGN CGN;
	private final int period;
	private final int transferPenalty;

	public LinePlanningInstance(DirectedGraph<StationNode, Track> network, List<ODPair> ODPairs, Set<Line> linePool,
			CGN CGN, int period, int transferPenalty)
	{
		this.network = network;
		this.ODPairs = ODPairs;
		this.linePool = linePool;
		this.CGN = CGN;
		this.period = period;
		this.transferPenalty = transferPenalty;
		double paxCounter = 0;
		for (ODPair k : ODPairs)
		{
			paxCounter += k.getVolume();
		}
		this.totNrPax = paxCounter;
	}

	public double getNrPax()
	{
		return totNrPax;
	}

	public DirectedGraph<StationNode, Track> getNetwork()
	{
		return network;
	}

	public List<ODPair> getODPairs()
	{
		return ODPairs;
	}

	public Set<Line> getLinePool()
	{
		return linePool;
	}

	public Set<Station> getStations()
	{
		Set<Station> stations = new LinkedHashSet<>();
		for (StationNode node : network.getNodes())
		{
			stations.add(node.getStation());
		}
		return stations;
	}

	public CGN getCGN()
	{
		return CGN;
	}

	public int getPeriod()
	{
		return period;
	}

	public int getTransferPenalty()
	{
		return transferPenalty;
	}
}