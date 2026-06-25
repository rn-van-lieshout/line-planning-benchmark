package linePlanning.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraphArc;
import linePlanning.DPTN.DPTN;
import linePlanning.DPTN.DPTNRoute;
import linePlanning.routeGeneration.RouteGenerator;

public class ODPairPreprocessor
{
	private final LinePlanningInstance instance;
	private final int splittingStrategy; // see explanation below
	private final RouteGenerator routeGenerator;
	private DPTN linePlanningNetwork;
	private double transferPenaltySplittedODs;
	private List<ODPair> splittedODs;
	private List<ODPair> aggregatedODs;

	public ODPairPreprocessor(LinePlanningInstance instance, int splittingStrategy, RouteGenerator routeGenerator)
	{
		this.linePlanningNetwork = instance.getDPTN();
		this.instance = instance;
		this.routeGenerator = routeGenerator;
		this.splittingStrategy = splittingStrategy;
		splittedODs = splitODPairs(instance.getODPairs());
		System.out.println(
				"After splitting we have " + splittedODs.size() + " ods instead of " + instance.getODPairs().size());

		aggregatedODs = aggregateODPairs(splittedODs);
		System.out.println("After aggregating we have " + aggregatedODs.size() + " ods instead of "
				+ instance.getODPairs().size());
	}

	/**
	 * This method splits an OD pair if it must transfer at some inbetween station
	 * Strategy = 1: Only split if all routes transfer at the same station (least
	 * aggressive) Strategy = 2: Retain the fastest route per physical path, and
	 * split if those routes all transfer at the same station Strategy = 3: Split
	 * based on the shortest path (most aggressive)
	 * 
	 * @param originalODs
	 * @return
	 */
	private List<ODPair> splitODPairs(List<ODPair> originalODs)
	{
		transferPenaltySplittedODs = 0;
		Map<Station, Map<Station, Double>> newMap = new HashMap<>();
		for (ODPair od : originalODs)
		{
			Station origin = od.getOrigin();
			Station destination = od.getDestination();
			boolean print = false;//(origin.getName().equals("Alm")&&destination.getName().equals("Dt"));
			List<DPTNRoute> odRoutes = new ArrayList<>();
			if (splittingStrategy == 3)
			{
				// only add shortest path
				odRoutes.add(linePlanningNetwork.getShortestPath(origin, destination));
			}
			else
			{
				odRoutes = routeGenerator.getRoutes(od,false);
				if (splittingStrategy == 2 && sameTrackPath(odRoutes))
				{
					odRoutes = onlyKeepShortest(odRoutes);
				}
			}
			if (print)
			{
				for (DPTNRoute r : odRoutes)
				{
					System.out.println(r);
				}
			}

			List<Station> commonTransferStations = getCommonTransferStations(odRoutes);
			if (commonTransferStations == null)
			{
				// keep the old od
				addToODPairMap(newMap, od);
				continue;
			}
			// there is a common transfer station, so split the OD
			Station from = origin;
			for (Station transfer : commonTransferStations)
			{
				addToODPairMap(newMap, new ODPair(from, transfer, od.getVolume()));
				from = transfer;
			}
			// add the final one
			addToODPairMap(newMap, new ODPair(from, destination, od.getVolume()));
			transferPenaltySplittedODs = getTransferPenaltySplittedODs()
					+ od.getVolume() * commonTransferStations.size();
		}
		return fromMapToODPairs(newMap);
	}

	private List<DPTNRoute> onlyKeepShortest(List<DPTNRoute> routes)
	{
		DPTNRoute shortest = null;
		for (DPTNRoute r : routes)
		{
			if (shortest == null)
			{
				shortest = r;
			}
			else
				if (r.getDuration() < shortest.getDuration())
				{
					shortest = r;
				}
		}
		List<DPTNRoute> onlyShortest = new ArrayList<>();
		onlyShortest.add(shortest);
		return onlyShortest;
	}

	private boolean sameTrackPath(List<DPTNRoute> routes)
	{
		List<Track> trackPath = null;
		for (DPTNRoute r : routes)
		{
			if (trackPath == null)
			{
				trackPath = r.getTrackPath();
			}
			else
				if (!trackPath.equals(r.getTrackPath()))
				{
					return false;
				}
		}
		return true;
	}

	private List<ODPair> fromMapToODPairs(Map<Station, Map<Station, Double>> odMatrixMap)
	{
		List<ODPair> ods = new ArrayList<>();
		for (Station origin : odMatrixMap.keySet())
		{
			Map<Station, Double> innerMap = odMatrixMap.get(origin);
			for (Station destination : innerMap.keySet())
			{
				ods.add(new ODPair(origin, destination, innerMap.get(destination)));
			}
		}
		return ods;
	}

	private void addToODPairMap(Map<Station, Map<Station, Double>> outerMap, ODPair od)
	{
		Station origin = od.getOrigin();
		Station destination = od.getDestination();
		if (!outerMap.containsKey(origin))
		{
			Map<Station, Double> innerMap = new HashMap<>();
			outerMap.put(origin, innerMap);
		}
		Map<Station, Double> innerMap = outerMap.get(origin);
		if (!innerMap.containsKey(destination))
		{
			innerMap.put(destination, od.getVolume());
		}
		else
		{
			innerMap.put(destination, innerMap.get(destination) + od.getVolume());
		}
	}

	public List<Station> getCommonTransferStations(List<DPTNRoute> routes)
	{
		// an OD can be split if all routes have a transfer at the same station
		List<Station> commonTransferStations = null;
		boolean print = false;
		for (DPTNRoute r : routes)
		{
			/*
			 * if(r.getOrigin().getName().equals("Lls")&&r.getDestination().getName().equals
			 * ("Brn")) { System.out.println("\n Route from Lls to Brn: "+r);
			 * System.out.println(r.getArcPath()); print = true; }
			 */
			if (r.isDirect())
			{
				// OD cannot be split
				if (print)
				{
					System.out.println("Route is direct");
				}
				return null;
			}
			List<Station> routeTransferStations = r.getTransferStations();
			if (commonTransferStations == null)
			{
				commonTransferStations = routeTransferStations;
			}
			else
			{
				List<Station> newCommonStations = new ArrayList<>();
				for (Station s : commonTransferStations)
				{
					if (routeTransferStations.contains(s))
					{
						newCommonStations.add(s);
					}
				}
				if (newCommonStations.isEmpty())
				{
					// OD cannot be split
					return null;
				}
				commonTransferStations = newCommonStations;
			}
		}
		if (print)
		{
			System.out.println("Returning at the end: " + commonTransferStations);
		}
		return commonTransferStations;

	}

	public double getTransferPenaltySplittedODs()
	{
		return transferPenaltySplittedODs;
	}

	/**
	 * Method that aggregates OD pairs of sprinter stations that are located on the
	 * same segment if possible
	 */
	private List<ODPair> aggregateODPairs(List<ODPair> ods)
	{
		List<StationNode> toScan = new ArrayList<>(instance.getTrackNetwork().getNodes());
		Map<Station, Station> origToAggStation = new HashMap<>();
		while (!toScan.isEmpty())
		{
			StationNode node = toScan.get(0);
			List<StationNode> toAggregate = scan(node);
			toScan.removeAll(toAggregate);
			StationNode target = toAggregate.get(toAggregate.size() / 2);
			for (StationNode sNode : toAggregate)
			{
				origToAggStation.put(sNode.getStation(), target.getStation());
			}
		}

		Map<Station, Map<Station, Double>> newMap = new HashMap<>();
		for (ODPair od : ods)
		{
			Station newOrigin = origToAggStation.get(od.getOrigin());
			Station newDestination = origToAggStation.get(od.getDestination());
			if(!newOrigin.equals(newDestination)) 
			{
				addToODPairMap(newMap, new ODPair(newOrigin, newDestination, od.getVolume()));
			}
		}

		return fromMapToODPairs(newMap);
	}

	private List<StationNode> scan(StationNode node)
	{
		List<StationNode> toAggregate = new ArrayList<>();
		if (!canBeAggregated(node))
		{
			// nodeToRepresentative.put(node, node.getStation());
			toAggregate.add(node);
			return toAggregate;
		}
		// the out-degree should be two
		Set<DirectedGraphArc<StationNode, Track>> outTracks = instance.getTrackNetwork().getOutArcs(node);
		if (outTracks.size() != 2)
		{
			throw new Error();
		}

		// continue in both directions until you hit a node that can't be aggregated
		boolean first = true;
		for (DirectedGraphArc<StationNode, Track> out : outTracks)
		{
			List<StationNode> toAggregatePart = traceTrack(out, node, new ArrayList<>());
			if (first)
			{
				toAggregate.addAll(toAggregatePart.reversed());
				toAggregate.add(node);
			}
			else
			{
				toAggregate.addAll(toAggregatePart);
			}
			first = false;
		}
		if (toAggregate.size() > 1)
		{
			// System.out.println("Should aggregate: "+toAggregate);
		}
		return toAggregate;
	}

	private List<StationNode> traceTrack(DirectedGraphArc<StationNode, Track> arc, StationNode curNode,
			List<StationNode> curList)
	{
		StationNode newStation = arc.getTo();
		if (canBeAggregated(newStation))
		{
			curList.add(newStation);
			for (DirectedGraphArc<StationNode, Track> out : instance.getTrackNetwork().getOutArcs(newStation))
			{
				if (!out.getTo().equals(curNode))
				{
					return traceTrack(out, newStation, curList);
				}
			}
		}
		return curList;
	}

	/**
	 * A station can be aggregated if it (1) is a sprinter station (2) is not a
	 * terminal station (3) Its degree is 2
	 * 
	 * @return
	 */
	private boolean canBeAggregated(StationNode node)
	{
		return (node.getStation().getType() == 0) && !node.getStation().isTerminalSPR()
				&& (instance.getTrackNetwork().getOutDegree(node) == 2);
	}

	public List<ODPair> getSplittedODs()
	{
		return splittedODs;
	}

	public List<ODPair> getAggregatedODs()
	{
		return aggregatedODs;
	}

}
