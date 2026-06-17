package linePlanning.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import util.Pair;
import util.Triple;
import util.UnorderedPair;

public class ReadNSInstance
{
	public static LinePlanningInstance readNSInstance(String stationFile, String trackFile, String connectionsFileIC,
			String forbiddenTurnFile, String typeChangeFile, String odFile, boolean randstad, boolean allowIC,
			boolean allowSPR) throws IOException
	{
		String name = "Nederland";
		if (randstad)
		{
			name = "Randstad";
		}
		if (allowIC)
		{
			name = name + "-IC";
		}
		if (allowSPR)
		{
			name = name + "-SPR";
		}
		// Read in stations.
		List<Station> stations = readStations(stationFile, randstad);
		Map<String, Station> stationMap = new LinkedHashMap<>();
		for (Station station : stations)
		{
			stationMap.put(station.getName(), station);
		}

		// Read tracks.
		List<Track> tracks = readTracks(trackFile, stationMap);
		List<Track> tracksBothDirections = new ArrayList<>();
		Map<Track, Track> oppositeTrack = new HashMap<>();
		tracksBothDirections.addAll(tracks);

		// Generate networks
		DirectedGraph<StationNode, Track> trackNetwork = new DirectedGraph<>();
		DirectedGraph<StationNode, Connection> connectionNetwork = new DirectedGraph<>();

		for (Station station : stationMap.values())
		{
			trackNetwork.addNode(new StationNode(station));
			connectionNetwork.addNode(new StationNode(station));
		}
		for (Track track : tracks)
		{
			// Note: we add the track in both directions.
			StationNode from = null;
			StationNode to = null;
			for (StationNode node : trackNetwork.getNodes())
			{
				if (node.getStation().equals(track.getFrom()))
				{
					from = node;
				}
				if (node.getStation().equals(track.getTo()))
				{
					to = node;
				}
			}
			trackNetwork.addArc(from, to, track, track.getDuration());

			Track trackBackward = new Track(Index.getTrackIndex(), track.getTo(), track.getFrom(), track.getLength(),
					track.getSpeed(), track.getDuration(), track.getNrOfTracks());
			trackNetwork.addArc(to, from, trackBackward, track.getDuration());
			tracksBothDirections.add(trackBackward);
			oppositeTrack.put(track, trackBackward);
			oppositeTrack.put(trackBackward, track);

			if (!allowSPR)
			{
				continue;
			}
			// also add connections for the sprinter arcs, unless HSL
			if (from.getStation().getName().equals("Bdpb") && to.getStation().getName().equals("Rtd"))
			{
				continue;
			}
			if (from.getStation().getName().equals("Rtd") && to.getStation().getName().equals("Shl"))
			{
				continue;
			}

			int maxFreq = track.getMaxFrequency(LinePlanningParameters.SPRINTER, allowIC);
			List<Track> forwardTracks = new ArrayList<>();
			forwardTracks.add(track);
			Connection forwardCon = new Connection(Index.getConnectionIndex(), track.getFrom(), track.getTo(),
					track.getLength(), track.getDuration(), forwardTracks, maxFreq, LinePlanningParameters.SPRINTER);
			connectionNetwork.addArc(from, to, forwardCon, forwardCon.getDuration());

			List<Track> backwardTracks = new ArrayList<>();
			backwardTracks.add(trackBackward);
			Connection backwardCon = new Connection(Index.getConnectionIndex(), trackBackward.getFrom(),
					trackBackward.getTo(), trackBackward.getLength(), trackBackward.getDuration(), backwardTracks,
					maxFreq, LinePlanningParameters.SPRINTER);
			connectionNetwork.addArc(to, from, backwardCon, backwardCon.getDuration());
			forwardCon.setReverse(backwardCon);
			backwardCon.setReverse(forwardCon);
		}

		// Read in IC-connections.
		List<Pair<Station, Station>> ICConnections = new ArrayList<>();
		if (allowIC)
		{
			ICConnections = readConnections(connectionsFileIC, stationMap);
		}

		// Generate IC-tracks by computing shortest paths over track network
		for (Pair<Station, Station> pair : ICConnections)
		{
			StationNode from = null;
			StationNode to = null;
			for (StationNode stationNode : trackNetwork.getNodes())
			{
				if (stationNode.getStation().equals(pair.getKey()))
				{
					from = stationNode;
				}
				if (stationNode.getStation().equals(pair.getValue()))
				{
					to = stationNode;
				}
			}
			Connection forwardCon = getConnection(from, to, trackNetwork, LinePlanningParameters.INTERCITY, allowSPR);
			connectionNetwork.addArc(from, to, forwardCon, forwardCon.getDuration());

			Connection backwardCon = getConnection(to, from, trackNetwork, LinePlanningParameters.INTERCITY, allowSPR);
			connectionNetwork.addArc(to, from, backwardCon, backwardCon.getDuration());

			forwardCon.setReverse(backwardCon);
			backwardCon.setReverse(forwardCon);
		}

		// Read in OD pairs.
		System.out.println("Read ODs");
		List<ODPair> ODPairs = readODPairs(odFile, stationMap, allowSPR);
		System.out.println("Number of ODs before filtering: " + ODPairs.size());
		// Filter ODPairs to limit instance size.
		// ODPairs = ODPairs.stream().limit(maxNumODPairs).collect(Collectors.toList());

		for (ODPair od : ODPairs)
		{
			if (od.getOrigin().getName().equals("Zd") || od.getDestination().getName().equals("Zd"))
			{
				System.out.println("Added " + od);
			}
		}

		// Construct a list of tracks and connections.
		Set<Track> uniqueTracks = new LinkedHashSet<>();
		for (DirectedGraphArc<StationNode, Track> arc : trackNetwork.getArcs())
		{
			uniqueTracks.add(arc.getData());
		}
		tracks = new ArrayList<>(uniqueTracks);
		Set<Connection> uniqueConnections = new LinkedHashSet<>();
		for (DirectedGraphArc<StationNode, Connection> arc : connectionNetwork.getArcs())
		{
			uniqueConnections.add(arc.getData());
		}
		List<Connection> connections = new ArrayList<>(uniqueConnections);

		// Read in forbidden turns and allowed type changes
		List<UnorderedPair<Track, Track>> forbiddenTurns = readForbiddenTurns(forbiddenTurnFile, stationMap,
				tracksBothDirections, oppositeTrack);
		List<Triple<Station, Station, Station>> allowedTypeChanges = readAllowedTypeChanges(typeChangeFile, stationMap);

		return new LinePlanningInstance(stations, tracks, connections, trackNetwork, connectionNetwork, ODPairs,
				forbiddenTurns, allowedTypeChanges, 100, allowIC, allowSPR, name);
	}

	private static List<Triple<Station, Station, Station>> readAllowedTypeChanges(String file,
			Map<String, Station> stationMap) throws IOException
	{
		List<Triple<Station, Station, Station>> result = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			String line = reader.readLine(); // skip header

			while ((line = reader.readLine()) != null)
			{
				if (line.trim().isEmpty())
					continue;

				String[] parts = line.split(";");
				if (parts.length < 3)
					continue;

				String fromName = parts[0].trim();
				String viaName = parts[1].trim();
				String toName = parts[2].trim();

				Station from = stationMap.get(fromName);
				Station via = stationMap.get(viaName);
				Station to = stationMap.get(toName);

				if (from == null || via == null || to == null)
				{
					System.out.println("Skipping allowed type change (unknown station): " + fromName + " -> " + viaName
							+ " -> " + toName);
					continue;
				}

				result.add(new Triple<>(from, via, to));
			}
		}
		return result;
	}

	private static Connection getConnection(StationNode from, StationNode to,
			DirectedGraph<StationNode, Track> trackNetwork, int typeIC, boolean allowSPR)
	{
		Dijkstra<StationNode, Track> dijkstra = new Dijkstra<>(trackNetwork, from);
		dijkstra.setDestination(to);
		dijkstra.computeDistances();
		double duration = dijkstra.getDistance(to);
		double length = 0;
		int maxFreq = Integer.MAX_VALUE;
		List<Track> trackPath = new ArrayList<>();
		for (DirectedGraphArc<StationNode, Track> arcInPath : dijkstra.getPath(to))
		{
			Track track = arcInPath.getData();
			trackPath.add(track);
			length += track.getLength();
			int maxFreqTrack = track.getMaxFrequency(typeIC, allowSPR);
			if (maxFreqTrack < maxFreq)
			{
				maxFreq = maxFreqTrack;
			}
		}
		return new Connection(Index.getConnectionIndex(), from.getStation(), to.getStation(), length, duration,
				trackPath, maxFreq, typeIC);
	}

	private static List<UnorderedPair<Track, Track>> readForbiddenTurns(String forbiddenTurnFile,
			Map<String, Station> stationMap, List<Track> tracks, Map<Track, Track> oppositeTrack) throws IOException
	{
		List<UnorderedPair<Track, Track>> forbiddenTurns = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(forbiddenTurnFile)))
		{

			// Skip header line "Van;Door;Naar"
			String line = br.readLine();
			if (line == null)
				return forbiddenTurns;

			while ((line = br.readLine()) != null)
			{
				if (line.isBlank())
					continue;

				String[] parts = line.split(";");
				if (parts.length < 3)
					continue;

				String van = parts[0].trim();
				String door = parts[1].trim();
				String naar = parts[2].trim();

				Station sVan = stationMap.get(van);
				Station sDoor = stationMap.get(door);
				Station sNaar = stationMap.get(naar);

				if (sVan == null || sDoor == null || sNaar == null)
				{
					System.err.println("Unknown station in forbidden turn: " + line);
					continue;
				}

				// Find matching tracks from dataset
				Track incoming = null;
				Track outgoing = null;

				for (Track t : tracks)
				{
					if (t.getFrom().equals(sVan) && t.getTo().equals(sDoor))
					{
						incoming = t;
					}
					if (t.getFrom().equals(sDoor) && t.getTo().equals(sNaar))
					{
						outgoing = t;
					}
					if (incoming != null && outgoing != null)
						break;
				}

				if (incoming == null || outgoing == null)
				{
					System.err.println("Warning: could not match tracks for forbidden turn: " + line);
					continue;
				}
				forbiddenTurns.add(new UnorderedPair<>(incoming, outgoing));
				forbiddenTurns.add(new UnorderedPair<>(oppositeTrack.get(outgoing), oppositeTrack.get(incoming)));
				// addToForbiddendSuccessorMap(incoming,outgoing,forbiddenSuccessors,oppositeTrack);
			}
		}
		return forbiddenTurns;
	}

	private static List<Pair<Station, Station>> readConnections(String connectionsFile, Map<String, Station> stationMap)
			throws IOException
	{
		List<Pair<Station, Station>> connections = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(connectionsFile)))
		{
			String line = reader.readLine(); // header skip

			while ((line = reader.readLine()) != null)
			{
				if (line.trim().isEmpty())
					continue;

				String[] parts = line.split(";");
				if (parts.length < 2)
					continue;

				String fromName = parts[0].trim();
				String toName = parts[1].trim();

				Station from = stationMap.get(fromName);
				Station to = stationMap.get(toName);

				if (from == null || to == null)
				{
					System.out.println("Skipping connection (unknown station): " + fromName + " -> " + toName);
					continue;
				}
				connections.add(new Pair<>(from, to));
			}
		}
		return connections;
	}

	private static List<Station> readStations(String stationsFile, boolean randstad) throws IOException
	{
		List<Station> stations = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(stationsFile)))
		{
			String line = reader.readLine(); // Skip header

			while ((line = reader.readLine()) != null)
			{
				if (line.trim().isEmpty())
					continue; // skip empty lines

				String[] parts = line.split(";");
				if (parts.length < 6)
					continue; // skip invalid rows

				String name = parts[0].trim();
				String nameFull = parts[1].trim();

				boolean terminalIC = parts[2].trim().equalsIgnoreCase("j");
				boolean terminalSPR = parts[3].trim().equalsIgnoreCase("j");

				String typeStr = parts[4].trim().toLowerCase();
				int type;
				switch (typeStr)
				{
				case "j":
					type = LinePlanningParameters.INTERCITY;
					break;
				case "twijfel":
				case "t":
					type = LinePlanningParameters.TWIJFEL;
					break;
				default:
					type = LinePlanningParameters.SPRINTER;
					break;
				}

				boolean inRandstad = parts[5].trim().equalsIgnoreCase("j");

				double x = Double.parseDouble(parts[6]);
				double y = Double.parseDouble(parts[7]);

				if (!randstad || inRandstad)
				{
					Station station = new Station(Index.getStationIndex(), name, nameFull, terminalIC, terminalSPR,
							type, inRandstad, x, y);
					stations.add(station);
				}
			}
		}
		return stations;
	}

	private static List<Track> readTracks(String trackFile, Map<String, Station> stationMap) throws IOException
	{
		List<Track> tracks = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(trackFile)))
		{
			String line = reader.readLine(); // skip header

			while ((line = reader.readLine()) != null)
			{
				if (line.trim().isEmpty())
					continue;

				String[] parts = line.split(";");
				if (parts.length < 5)
					continue; // invalid row, skip

				String fromName = parts[0].trim();
				String toName = parts[1].trim();

				Station from = stationMap.get(fromName);
				Station to = stationMap.get(toName);

				// Skip tracks referencing unknown stations
				if (from == null || to == null)
				{
					// System.out.println("Skipping track because station not found: " + fromName +
					// " - " + toName);
					continue;
				}

				double hectometers = Double.parseDouble(parts[2].trim());
				int speed = Integer.parseInt(parts[3].trim());
				int nrOfTracks = Integer.parseInt(parts[4].trim());

				double lengthKm = hectometers / 10.0;
				double durationMinutes = (lengthKm / speed) * 60.0;

				Track t = new Track(Index.getTrackIndex(), from, to, lengthKm, speed, durationMinutes, nrOfTracks);
				tracks.add(t);
			}
		}
		return tracks;
	}

	private static List<ODPair> readODPairs(String ODFile, Map<String, Station> stationMap, boolean allowSPR)
			throws IOException
	{
		boolean onlyIC = !allowSPR;
		List<ODPair> ODPairs = new ArrayList<>();

		// Construct OD mapping.
		List<String> stations = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(ODFile));

		// Read in headers.
		String line = bufferedReader.readLine();
		String[] data = line.split(";");
		for (int i = 1; i < data.length; i++)
		{
			stations.add(data[i]);
		}
		// System.out.println("Reading ODs for "+stationMap.size()+" stations.
		// stationList in OD file hase "+stations.size()+" stations");
		line = bufferedReader.readLine();
		while (line != null)
		{
			data = line.split(";");
			// System.out.println(line);
			String from = data[0];

			if (stationMap.get(from) == null)
			{
				line = bufferedReader.readLine();
				// System.out.println("Skipping "+from);
				continue;
			}
			if (onlyIC && stationMap.get(from).getType() != LinePlanningParameters.INTERCITY)
			{
				line = bufferedReader.readLine();
				continue;
			}
			// System.out.println("Checking "+from);
			for (int i = 1; i < data.length; i++)
			{
				String to = stations.get(i - 1);
				if (stationMap.get(to) == null)
				{
					continue;
				}
				if (onlyIC && stationMap.get(to).getType() != LinePlanningParameters.INTERCITY)
				{
					continue;
				}
				double volume = Double.valueOf(data[i]);

				if (volume > 0)
				{
					ODPairs.add(new ODPair(stationMap.get(from), stationMap.get(to), volume));
				}
				else
				{
				}
			}
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		return ODPairs;
	}
}
