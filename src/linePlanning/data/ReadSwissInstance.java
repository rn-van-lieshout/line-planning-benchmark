package linePlanning.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.algorithms.shortestPath.Dijkstra;
import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import linePlanning.PTN.Connection;
import linePlanning.instance.Index;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.instance.ODPair;
import linePlanning.instance.Station;
import linePlanning.instance.StationNode;
import linePlanning.instance.Track;
import util.Triple;
import util.UnorderedPair;

public class ReadSwissInstance
{
	public static LinePlanningInstance readSwissInstance(String processedStationsFile, String connectionsFile,
			String odFile, String illegalTurnsFile, String trackFile, boolean small) throws IOException
	{
		Map<String, Station> stationMap = readStations(processedStationsFile);
		PrepareSwissInstance.readTracksWithGeoshapes(trackFile, stationMap);

		DirectedGraph<StationNode, Track> trackNetwork = PrepareSwissInstance.buildGraphFromFile(trackFile, stationMap);
		DirectedGraph<StationNode, Connection> connectionNetwork = new DirectedGraph<>();
		Map<String, StationNode> nodeMap = buildNodeMap(stationMap, trackNetwork, connectionNetwork);

		System.out.println("Start reading connections");
		Map<String, Connection> connectionByKey = readConnections(connectionsFile, stationMap, nodeMap, trackNetwork,
				connectionNetwork);
		linkReverseConnections(connectionNetwork, connectionByKey);

		List<ODPair> odPairs = Reader.readODPairs(odFile, stationMap);
		List<UnorderedPair<Track, Track>> forbiddenTurns = readIllegalTurns(illegalTurnsFile, nodeMap, trackNetwork);
		List<Triple<Station, Station, Station>> allowedTypeChanges = new ArrayList<>();

		// Prepare auxiliary files.
		List<Station> stations = new ArrayList<>(stationMap.values());
		Set<Track> uniqueTracks = new LinkedHashSet<>();
		for (DirectedGraphArc<StationNode, Track> arc : trackNetwork.getArcs())
		{
			uniqueTracks.add(arc.getData());
		}
		List<Track> tracks = new ArrayList<>(uniqueTracks);
		Set<Connection> uniqueConnections = new LinkedHashSet<>();
		for (DirectedGraphArc<StationNode, Connection> arc : connectionNetwork.getArcs())
		{
			uniqueConnections.add(arc.getData());
		}
		List<Connection> connections = new ArrayList<>(uniqueConnections);

		String name = "Switzerland";
		if(small) {
			name = "SmallSwitzerland";
		}
		return new LinePlanningInstance(stations, tracks, connections, trackNetwork, connectionNetwork, odPairs,
				forbiddenTurns, allowedTypeChanges, 100, true, false, name);
	}

	private static List<UnorderedPair<Track, Track>> readIllegalTurns(String illegalTurnsFile,
			Map<String, StationNode> nodeMap, DirectedGraph<StationNode, Track> trackNetwork) throws IOException
	{
		List<UnorderedPair<Track, Track>> forbiddenTurns = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(illegalTurnsFile)))
		{
			reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;
				String[] parts = line.split(";");
				if (parts.length < 3)
					continue;
				StationNode van = nodeMap.get(parts[0].trim());
				StationNode door = nodeMap.get(parts[1].trim());
				StationNode naar = nodeMap.get(parts[2].trim());
				if (van == null || door == null || naar == null)
					continue;

				Track incoming = findTrack(trackNetwork, van, door);
				Track outgoing = findTrack(trackNetwork, door, naar);
				if (incoming == null || outgoing == null)
					continue;

				forbiddenTurns.add(new UnorderedPair<>(incoming, outgoing));
			}
		}
		System.out.println("Read " + forbiddenTurns.size() + " forbidden turns from " + illegalTurnsFile);
		return forbiddenTurns;
	}

	private static Track findTrack(DirectedGraph<StationNode, Track> trackNetwork, StationNode from, StationNode to)
	{
		for (DirectedGraphArc<StationNode, Track> arc : trackNetwork.getOutArcs(from))
			if (arc.getTo().equals(to))
				return arc.getData();
		return null;
	}

	private static Map<String, Station> readStations(String processedStationsFile) throws IOException
	{
		Map<String, Station> stationMap = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(processedStationsFile)))
		{
			reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;
				String[] parts = line.split(";");
				if (parts.length < 7)
					continue;
				String abbr = parts[0].trim();
				String stopName = parts[1].trim();
				boolean terminal = parts[4].trim().equalsIgnoreCase("Y");
				double latitude = Double.parseDouble(parts[5].trim());
				double longitude = Double.parseDouble(parts[6].trim());
				Station station = new Station(Index.getStationIndex(), abbr, stopName, terminal, terminal,
						LinePlanningParameters.INTERCITY, false, latitude, longitude);
				stationMap.put(abbr, station);
			}
		}
		return stationMap;
	}

	private static Map<String, StationNode> buildNodeMap(Map<String, Station> stationMap,
			DirectedGraph<StationNode, Track> trackNetwork, DirectedGraph<StationNode, Connection> connectionNetwork)
	{
		Map<String, StationNode> nodeMap = new HashMap<>();
		for (StationNode node : trackNetwork.getNodes())
		{
			nodeMap.put(node.getStation().getName(), node);
			if (stationMap.containsKey(node.getStation().getName()))
				connectionNetwork.addNode(node);
		}
		return nodeMap;
	}

	private static Map<String, Connection> readConnections(String connectionsFile, Map<String, Station> stationMap,
			Map<String, StationNode> nodeMap, DirectedGraph<StationNode, Track> trackNetwork,
			DirectedGraph<StationNode, Connection> connectionNetwork) throws IOException
	{
		Map<String, Connection> connectionByKey = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(connectionsFile)))
		{
			reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;
				String[] parts = line.split(";");
				if (parts.length < 5)
					continue;

				String fromAbbr = parts[0].trim();
				String toAbbr = parts[1].trim();
				double lengthM = Double.parseDouble(parts[2].trim());
				double durationMin = Double.parseDouble(parts[3].trim());
				int maxFreq = Integer.parseInt(parts[4].trim());

				Station fromStation = stationMap.get(fromAbbr);
				Station toStation = stationMap.get(toAbbr);
				if (fromStation == null || toStation == null)
				{
					System.err.println("Warning: unknown station in connection: " + fromAbbr + " -> " + toAbbr);
					continue;
				}

				StationNode fromNode = nodeMap.get(fromAbbr);
				StationNode toNode = nodeMap.get(toAbbr);

				List<Track> trackPath = getTrackPath(fromNode, toNode, trackNetwork);
				Connection connection = new Connection(Index.getConnectionIndex(), fromStation, toStation, lengthM,
						durationMin, trackPath, maxFreq, LinePlanningParameters.INTERCITY);

				System.out.println("Added " + connection);
				connectionNetwork.addArc(fromNode, toNode, connection, durationMin);

				connectionByKey.put(fromAbbr + "-" + toAbbr, connection);
			}
		}
		return connectionByKey;
	}

	private static List<Track> getTrackPath(StationNode from, StationNode to,
			DirectedGraph<StationNode, Track> trackNetwork)
	{
		Dijkstra<StationNode, Track> dijkstra = new Dijkstra<>(trackNetwork, from);
		dijkstra.setDestination(to);
		dijkstra.computeDistances();
		List<Track> trackPath = new ArrayList<>();
		for (DirectedGraphArc<StationNode, Track> arcInPath : dijkstra.getPath(to))
		{
			Track track = arcInPath.getData();
			trackPath.add(track);
		}
		return trackPath;
	}

	private static void linkReverseConnections(DirectedGraph<StationNode, Connection> connectionNetwork,
			Map<String, Connection> connectionByKey)
	{
		for (DirectedGraphArc<StationNode, Connection> arc : connectionNetwork.getArcs())
		{
			String fromAbbr = arc.getFrom().getStation().getName();
			String toAbbr = arc.getTo().getStation().getName();
			Connection fwd = arc.getData();
			Connection rev = connectionByKey.get(toAbbr + "-" + fromAbbr);
			if (rev != null)
				fwd.setReverse(rev);
		}
	}

}
