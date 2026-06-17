package linePlanning.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Station;
import timeTabling.Evaluator;

public class CGN extends DirectedGraph<StationNode, CGNArc>
{
	private Map<Station, StationODNode> originNodeMap;
	private Map<Station, StationODNode> destinationNodeMap;
	private List<StationLineNode> stationLineNodes;

	private Map<TransferArc, DirectedGraphArc<StationNode, CGNArc>> transferArcMap;

	public CGN()
	{
		super();

		this.originNodeMap = new LinkedHashMap<>();
		this.destinationNodeMap = new LinkedHashMap<>();
		this.stationLineNodes = new ArrayList<>();

		this.transferArcMap = new LinkedHashMap<>();
	}

	public void addOriginNode(StationODNode node)
	{
		addNode(node);
		originNodeMap.put(node.getStation(), node);
	}

	public void addDestinationNode(StationODNode node)
	{
		addNode(node);
		destinationNodeMap.put(node.getStation(), node);
	}

	public void addStationlineNode(StationLineNode node)
	{
		addNode(node);
		stationLineNodes.add(node);
	}

	public StationODNode getOriginNode(Station station)
	{
		return originNodeMap.get(station);
	}

	public Collection<StationODNode> getOriginNodes()
	{
		return originNodeMap.values();
	}

	public Collection<StationODNode> getDestinationNodes()
	{
		return destinationNodeMap.values();
	}

	public StationODNode getDestinationNode(Station station)
	{
		return destinationNodeMap.get(station);
	}

	public List<StationLineNode> getStationLineNodes()
	{
		return stationLineNodes;
	}

	public void addTransferArc(StationNode from, StationNode to, TransferArc transferArc)
	{
		if (!inArcs.containsKey(from) || !outArcs.containsKey(to))
		{
			throw new IllegalArgumentException("Unable to add arcs between nodes not in the graph");
		}
		DirectedGraphArc<StationNode, CGNArc> a = new DirectedGraphArc<>(from, to, transferArc,
				transferArc.getDuration());
		outArcs.get(from).add(a);
		inArcs.get(to).add(a);
		arcs.add(a);
		transferArcMap.put(transferArc, a);
	}

	public DirectedGraphArc<StationNode, CGNArc> getTransferArc(TransferArc transferArc)
	{
		return transferArcMap.get(transferArc);
	}

	public void resetDuals()
	{
		for (DirectedGraphArc<StationNode, CGNArc> arc : arcs)
		{
			arc.setCost(arc.getData().getDuration(), 0);
			arc.setCost(0, 1);

			arc.setDual(0, 0);
			arc.setDual(0, 1);
		}
	}

	public CGN filter(Set<Line> lines)
	{
		CGN cgn = new CGN();
		for (StationNode node : this.getNodes())
		{
			if (node instanceof StationODNode)
			{
				StationODNode stationODNode = (StationODNode) node;
				if (stationODNode.isOrigin())
				{
					cgn.addOriginNode(stationODNode);
				}
				else
				{
					cgn.addDestinationNode(stationODNode);
				}
			}
			if (node instanceof StationLineNode)
			{
				StationLineNode stationLineNode = (StationLineNode) node;
				if (lines.contains(stationLineNode.getLine()))
				{
					cgn.addStationlineNode(stationLineNode);
				}
			}
		}
		for (DirectedGraphArc<StationNode, CGNArc> arc : this.getArcs())
		{
			if (cgn.getNodes().contains(arc.getFrom()) && cgn.getNodes().contains(arc.getTo()))
			{
				cgn.addArc(arc.getFrom(), arc.getTo(), arc.getData(), arc.getData().getDuration());
			}
		}
		return cgn;
	}

	/**
	 * Method that returns a copy of the CGN assocated to the input lines, and
	 * updates dwell and transfer times based on the evaluator
	 */
	public CGN filterAndUpdate(Set<Line> lines, Evaluator eval)
	{
		CGN cgn = new CGN();
		for (StationNode node : this.getNodes())
		{
			if (node instanceof StationODNode)
			{
				StationODNode stationODNode = (StationODNode) node;
				if (stationODNode.isOrigin())
				{
					cgn.addOriginNode(stationODNode);
				}
				else
				{
					cgn.addDestinationNode(stationODNode);
				}
			}
			if (node instanceof StationLineNode)
			{
				StationLineNode stationLineNode = (StationLineNode) node;
				if (lines.contains(stationLineNode.getLine()))
				{
					cgn.addStationlineNode(stationLineNode);
				}
			}
		}
		for (DirectedGraphArc<StationNode, CGNArc> arc : this.getArcs())
		{
			if (cgn.getNodes().contains(arc.getFrom()) && cgn.getNodes().contains(arc.getTo()))
			{
				CGNArc cArc = arc.getData();
				int duration = cArc.getDuration();
				if (cArc instanceof DwellArc)
				{
					duration = eval.getActualTime(cArc);
					((DwellArc) cArc).setDwellTime(duration);
				}
				else if (cArc instanceof TransferArc)
				{
					duration = eval.getActualTime(cArc);
					((TransferArc) cArc).setTransferTime(duration);
				}
				cgn.addArc(arc.getFrom(), arc.getTo(), cArc, duration);
			}
		}
		return cgn;
	}
}
