package linePlanning.data;

import java.util.LinkedHashSet;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import linePlanning.instance.BoardingArc;
import linePlanning.instance.CGN;
import linePlanning.instance.CGNArc;
import linePlanning.instance.DwellArc;
import linePlanning.instance.Line;
import linePlanning.instance.StationLineNode;
import linePlanning.instance.StationNode;
import linePlanning.instance.StationODNode;
import linePlanning.instance.Track;
import linePlanning.instance.TransferArc;
import linePlanning.instance.TravelArc;
import modelling.Station;

public class GenerateCNGNetwork
{
	public static CGN generateCGNNetwork(DirectedGraph<StationNode, Track> network,
			Set<Line> linePool, int dwellTime, int transferTime, int transferPenalty)
	{
		CGN CGN = new CGN();

		// Retrieve set of large stations.
		Set<Station> largeStations = new LinkedHashSet<>();
		for (Line line : linePool)
		{
			largeStations.add(line.getStartStation());
			largeStations.add(line.getEndStation());
		}

		// Add station OD nodes.
		for (StationNode stationNode : network.getNodes())
		{
			CGN.addOriginNode(new StationODNode(stationNode.getStation(), true));
			CGN.addDestinationNode(new StationODNode(stationNode.getStation(), false));
		}

		// Add nodes and travel arcs for all lines.
		for (Line line : linePool)
		{
			// Keep track of previous node.
			StationLineNode previous = null;

			// Add arrival and departure nodes for each combination of line and station.
			for (int i = 0; i < line.getLineSegments().size(); i++)
			{
				Track lineSegment = line.getLineSegments().get(i);
				Station to = lineSegment.getTo();

				// Add a node.
				if (previous == null)
				{
					StationLineNode startDepartureNode = new StationLineNode(lineSegment.getFrom(), line, false);
					CGN.addStationlineNode(startDepartureNode);
					previous = startDepartureNode;

					// Add boarding arc from origin node to departure node.
					CGN.addArc(CGN.getOriginNode(startDepartureNode.getStation()), startDepartureNode,
							new BoardingArc(), 0);
				}

				// Add arrival node for end of segment.
				StationLineNode endArrivalNode = new StationLineNode(to, line, true);
				CGN.addStationlineNode(endArrivalNode);

				// Add boarding arc to destination node.
				if (!line.isInterCity() || largeStations.contains(to))
				{
					CGN.addArc(endArrivalNode, CGN.getDestinationNode(endArrivalNode.getStation()), new BoardingArc(),
							0);
				}

				// Add travel arcs for this segment.
				TravelArc travelArc = new TravelArc(line, previous.getStation(), to, lineSegment.getDuration());
				CGN.addArc(previous, endArrivalNode, travelArc, travelArc.getDuration());

				// Add departure node for end of segment if not last segment of line.
				if (i < line.getLineSegments().size() - 1)
				{
					StationLineNode endDepartureNode = new StationLineNode(lineSegment.getTo(), line, false);
					CGN.addStationlineNode(endDepartureNode);

					// Add dwell arc.
					if (!line.isInterCity() || largeStations.contains(to))
					{
						DwellArc dwellArc = new DwellArc(line, to, dwellTime);
						CGN.addArc(endArrivalNode, endDepartureNode, dwellArc, dwellArc.getDuration());
					}
					else
					{
						TravelArc artificialTravelArc = new TravelArc(line, to, to, 0);
						CGN.addArc(endArrivalNode, endDepartureNode, artificialTravelArc, 0);

					}

					// Add boarding arc from origin node.
					if (!line.isInterCity() || largeStations.contains(to))
					{
						CGN.addArc(CGN.getOriginNode(endArrivalNode.getStation()), endDepartureNode, new BoardingArc(),
								0);
					}

					// Update last node.
					previous = endDepartureNode;
				}
			}
		}

		// Add transfer arcs.
		for (StationLineNode firstNode : CGN.getStationLineNodes())
		{
			secondNodeLoop: for (StationLineNode secondNode : CGN.getStationLineNodes())
			{
				// Retrieve lines.
				Line firstLine = firstNode.getLine();
				Line secondLine = secondNode.getLine();

				// Perform necessary checks.
				if (!firstNode.getStation().equals(secondNode.getStation()))
				{
					continue;
				}
				if (firstNode.equals(secondNode))
				{
					continue;
				}
				if (firstLine.equals(secondLine))
				{
					continue;
				}

				// Never allow transfers between line and its reverse.
				if (firstLine.isReverseLine()
						&& firstLine.getReverseLine().equals(secondLine))
				{
					continue;
				}
				if (secondLine.isReverseLine()
						&& secondLine.getReverseLine().equals(firstLine))
				{
					continue;
				}

				// Do not allow transfers from a departure node or to an arrival node.
				if (!firstNode.isArrival())
				{
					continue;
				}
				if (secondNode.isArrival())
				{
					continue;
				}

				// Remove redundant transfers.
				for (int i = 0; i < firstLine.getLineSegments().size(); i++)
				{
					if (!firstLine.getLineSegments().get(i).getTo().equals(firstNode.getStation()))
					{
						continue;
					}
					for (int j = 0; j < secondLine.getLineSegments().size(); j++)
					{
						if (!secondLine.getLineSegments().get(j).getFrom().equals(secondNode.getStation()))
						{
							continue;
						}
						Station nextSecondStation = secondLine.getLineSegments().get(j).getTo();

						// Do not include transfers between lines that move in the same direction.
						if (i + 1 < firstLine.getLineSegments().size())
						{
							Station nextFirstStation = firstLine.getLineSegments().get(i + 1).getTo();
							if (nextFirstStation.equals(nextSecondStation))
							{
								continue secondNodeLoop;
							}
						}

						// Do not include transfers to lines that move in the reverse direction.
						Station previousFirstStation = firstLine.getLineSegments().get(i).getFrom();
						if (nextSecondStation.equals(previousFirstStation))
						{
							continue secondNodeLoop;
						}
					}
				}

				// We only allow transfers at large stations.
				if (!largeStations.contains(firstNode.getStation()) || !largeStations.contains(secondNode.getStation()))
				{
					continue;
				}

				// Add a transfer arc.
				TransferArc transferArc = new TransferArc(firstLine, secondLine,
						firstNode.getStation(), transferTime, transferPenalty);
				CGN.addTransferArc(firstNode, secondNode, transferArc);
			}
		}

		int travelArc = 0;
		int dwellArc = 0;
		int transferArc = 0;
		for (DirectedGraphArc<StationNode, CGNArc> arc : CGN.getArcs())
		{
			if (arc.getData() instanceof TravelArc)
			{
				travelArc++;
			}
			if (arc.getData() instanceof DwellArc)
			{
				dwellArc++;
			}
			if (arc.getData() instanceof TransferArc)
			{
				transferArc++;
			}
		}
		System.out.println("Travel: " + travelArc);
		System.out.println("Dwell: " + dwellArc);
		System.out.println("Transfer: " + transferArc);

		// Return network.
		return CGN;
	}
}
