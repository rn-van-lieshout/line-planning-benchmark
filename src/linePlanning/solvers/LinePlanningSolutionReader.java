package linePlanning.solvers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graph.structures.digraph.DirectedGraphArc;
import linePlanning.DPTN.DPTN;
import linePlanning.DPTN.DPTNArc;
import linePlanning.DPTN.DPTNDrivingArc;
import linePlanning.DPTN.DPTNDwellArc;
import linePlanning.PTN.Connection;
import linePlanning.instance.Index;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.Station;
import linePlanning.instance.StationNode;
import linePlanning.lineGeneration.Line;

public class LinePlanningSolutionReader
{
	public static LinePlanningSolution read(String filePath, LinePlanningInstance instance) throws IOException
	{
		DPTN dptn = instance.getDPTN();

		// Build connection lookup by index
		Map<Integer, Connection> connectionByIndex = new HashMap<>();
		for (Connection c : instance.getConnections())
		{
			connectionByIndex.put(c.getIndex(), c);
		}

		// For each connection, find its forward driving arc (from -> to direction)
		// Also index all outgoing arcs per node to find dwell arcs between connections
		Map<Connection, DirectedGraphArc<StationNode, DPTNArc>> forwardDrivingArc = new HashMap<>();
		Map<StationNode, List<DirectedGraphArc<StationNode, DPTNArc>>> outgoingArcs = new HashMap<>();

		for (DirectedGraphArc<StationNode, DPTNArc> arc : dptn.getArcs())
		{
			outgoingArcs.computeIfAbsent(arc.getFrom(), k -> new ArrayList<>()).add(arc);
			if (arc.getData() instanceof DPTNDrivingArc drivingArc)
			{
				if (arc.getFrom().getStation().equals(drivingArc.getConnection().getFrom()))
				{
					forwardDrivingArc.put(drivingArc.getConnection(), arc);
				}
			}
		}

		// Parse file
		List<Line> linePlan = new ArrayList<>();
		Map<Line, Integer> frequencyMap = new HashMap<>();

		String section = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("# "))
				{
					section = line.substring(2).trim();
					continue;
				}
				if (line.startsWith("Feasible: false"))
					return LinePlanningSolution.noSolution(instance);
				if (line.isBlank() || !"LinePlan".equals(section))
				{
					continue;
				}

				String[] parts = line.split(" ");
				int frequency = Integer.parseInt(parts[0]);

				List<Connection> connections = new ArrayList<>();
				for (int i = 1; i < parts.length; i++)
				{
					connections.add(connectionByIndex.get(Integer.parseInt(parts[i])));
				}

				// Reconstruct the DPTN arc path: driving arcs with dwell arcs in between
				List<DirectedGraphArc<StationNode, DPTNArc>> arcPath = new ArrayList<>();
				for (int i = 0; i < connections.size(); i++)
				{
					DirectedGraphArc<StationNode, DPTNArc> drivingArc = forwardDrivingArc.get(connections.get(i));
					if (i > 0)
					{
						StationNode prevEnd = arcPath.get(arcPath.size() - 1).getTo();
						StationNode nextStart = drivingArc.getFrom();
						DirectedGraphArc<StationNode, DPTNArc> dwellArc = outgoingArcs.get(prevEnd).stream()
								.filter(a -> a.getData() instanceof DPTNDwellArc && a.getTo().equals(nextStart))
								.findFirst().orElseThrow(() -> new RuntimeException(
										"No dwell arc found between " + prevEnd + " and " + nextStart));
						arcPath.add(dwellArc);
					}
					arcPath.add(drivingArc);
				}

				Station origin = connections.get(0).getFrom();
				Station destination = connections.get(connections.size() - 1).getTo();
				Line builtLine = new Line(Index.getLineIndex(), origin, destination, arcPath, dptn, instance);
				linePlan.add(builtLine);
				frequencyMap.put(builtLine, frequency);
			}
		}
		return new LinePlanningSolution(instance, linePlan, frequencyMap);
	}
}
