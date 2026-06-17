package linePlanning.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import graph.algorithms.shortestPath.Dijkstra;
import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import linePlanning.PTN.Connection;
import linePlanning.instance.Index;
import linePlanning.instance.ODPair;
import linePlanning.instance.Station;
import linePlanning.instance.StationNode;
import linePlanning.instance.Track;

public class PrepareSwissInstance
{
	private static final int AVERAGE_SPEED_KMH = 80;
	private static final Map<String, String> STATION_ALIASES = Map.of("ZMUS", "ZUE", "ZLOE", "ZUE");
	private static final double MAX_DEMAND_TRAVEL_DISTANCE = 300; // km
	private static final double DEMAND_LOWER_THRESHOLD = 10.0;
	private static final double DISTANCE_EXPONENT = 1.1; // higher = shorter distances penalized more
	private static final double POPULATION_EXPONENT = 0.25; // higher = larger cities get disproportionately more demand
	private static final double MIN_TURN_ANGLE_DEG = 90.0; // turns sharper than this are marked illegal

	public static void main(String[] args) throws IOException
	{
		boolean small = true;

		// determineMajorStations("dataSwiss/perrons.txt",
		// "dataSwiss/processedStations.txt");
		String stationsFile = "dataSwiss/stationsWithPopulations.txt";
		if (small)
		{
			stationsFile = "dataSwiss/stationsWithPopulationsSmall.txt";
		}
		Map<String, Station> knownStations = readStationsFile(stationsFile);
		System.out.println("Read " + knownStations.size() + " stations from " + stationsFile);
		readTracksWithGeoshapes("dataSwiss/tracks-with-geoshapes.csv", knownStations);
		System.out.println("After supplementing from geoshapes: " + knownStations.size() + " stations total");

		DirectedGraph<StationNode, Track> trackNetwork = buildGraphFromFile("dataSwiss/tracks-with-geoshapes.csv",
				knownStations);
		System.out.println("Track network with " + trackNetwork.getNumberOfNodes() + " stations and "
				+ trackNetwork.getNumberOfArcs() + " tracks");
		DirectedGraph<StationNode, Connection> connectionNetwork = buildInitialConnectionNetwork(trackNetwork,
				stationsFile);
		filterRedundantConnections(connectionNetwork);
		ensureSymmetry(connectionNetwork);
		printNetwork(connectionNetwork);

		String connectionsFile = "dataSwiss/connections.txt";
		if (small)
		{
			connectionsFile = "dataSwiss/connectionsSmall.txt";
		}
		writeConnections(connectionNetwork, connectionsFile);
		writeIllegalTurns(trackNetwork, knownStations, "dataSwiss/IllegalTurns.txt");

		String odFile = "dataSwiss/od.txt";
		if (small)
		{
			odFile = "dataSwiss/odSmall.txt";
		}
		List<ODPair> odPairs = generateODPairs(stationsFile, odFile);
		// odPairs.stream()
		// .sorted((a, b) -> Double.compare(b.getVolume(), a.getVolume()))
		// .limit(100)
		// .forEach(p -> System.out.printf("%-6s -> %-6s : %.1f%n",
		// p.getOrigin().getNameFull(), p.getDestination().getNameFull(),
		// p.getVolume()));
	}

	private static void determineMajorStations(String perronsFile, String outputFile) throws IOException
	{
		List<String> majorStationAbbrs = getLargeStations(perronsFile, 4);
		majorStationAbbrs = majorStationAbbrs.stream().map(abbr -> STATION_ALIASES.getOrDefault(abbr, abbr)).distinct()
				.collect(Collectors.toList());
		System.out.println(majorStationAbbrs);
		System.out.println("There are " + majorStationAbbrs.size() + " stations with at least 4 platform tracks");
		writeStationPopulations(majorStationAbbrs, perronsFile, outputFile);
	}

	private static List<String> readMajorStationAbbrs(String processedStationsFile) throws IOException
	{
		List<String> abbrs = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(processedStationsFile)))
		{
			reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;
				String[] parts = line.split(";");
				if (parts.length >= 1)
					abbrs.add(parts[0].trim());
			}
		}
		return abbrs;
	}

	private static DirectedGraph<StationNode, Connection> buildInitialConnectionNetwork(
			DirectedGraph<StationNode, Track> trackNetwork, String processedStationsFile) throws IOException
	{
		List<String> majorStationAbbrs = readMajorStationAbbrs(processedStationsFile);
		Set<StationNode> majorStationNodes = createMajorStationNodes(majorStationAbbrs, trackNetwork);
		DirectedGraph<StationNode, Connection> connectionNetwork = new DirectedGraph<>();
		for (StationNode sNode : majorStationNodes)
		{
			connectionNetwork.addNode(sNode);
		}
		createMajorStationConnections(trackNetwork, majorStationNodes, 2, false, connectionNetwork);
		return connectionNetwork;
	}

	private static void filterRedundantConnections(DirectedGraph<StationNode, Connection> connectionNetwork)
	{
		System.out.println("There are " + connectionNetwork.getNumberOfArcs() + " arcs before filtering");
		List<DirectedGraphArc<StationNode, Connection>> consCopy = new ArrayList<>(connectionNetwork.getArcs());
		Set<DirectedGraphArc<StationNode, Connection>> removedArcs = new HashSet<>();
		for (DirectedGraphArc<StationNode, Connection> c : consCopy)
		{
			if (removedArcs.contains(c))
				continue;
			// check if removing c really affects the travel time between these nodes
			connectionNetwork.removeArc(c);
			Dijkstra<StationNode, Connection> dijkstra = new Dijkstra<>(connectionNetwork, c.getFrom());
			dijkstra.setDestination(c.getTo());
			dijkstra.computeDistances();
			double timeWithoutC = dijkstra.getDistance(c.getTo());
			if (timeWithoutC < c.getData().getDuration() + 8)
			{
				// Find the connection in the detour path that shares tracks with c
				Set<Track> cTracks = new HashSet<>(c.getData().getTrackPath());
				DirectedGraphArc<StationNode, Connection> otherCArc = null;
				for (DirectedGraphArc<StationNode, Connection> pathArc : dijkstra.getPath(c.getTo()))
				{
					for (Track t : pathArc.getData().getTrackPath())
					{
						if (cTracks.contains(t))
						{
							otherCArc = pathArc;
							break;
						}
					}
					if (otherCArc != null)
						break;
				}
				if (otherCArc == null)
				{
					// No overlap found — keep both
					connectionNetwork.addArc(c.getFrom(), c.getTo(), c.getData(), c.getData().getDuration());
				}
				else
					if (otherCArc.getData().getDuration() > c.getData().getDuration())
					{
						// c is shorter — keep c, remove otherC
						connectionNetwork.addArc(c.getFrom(), c.getTo(), c.getData(), c.getData().getDuration());
						connectionNetwork.removeArc(otherCArc);
						removedArcs.add(otherCArc);
					}
				// else: otherC is shorter or equal — keep otherC, remove c (don't add back)
			}
			else
			{
				connectionNetwork.addArc(c.getFrom(), c.getTo(), c.getData(), c.getData().getDuration());
			}
		}
		System.out.println("There are " + connectionNetwork.getNumberOfArcs() + " arcs after filtering");
	}

	private static void ensureSymmetry(DirectedGraph<StationNode, Connection> connectionNetwork)
	{
		for (DirectedGraphArc<StationNode, Connection> arc : new ArrayList<>(connectionNetwork.getArcs()))
		{
			StationNode from = arc.getFrom();
			StationNode to = arc.getTo();
			boolean hasReverse = connectionNetwork.getOutArcs(to).stream().anyMatch(a -> a.getTo().equals(from));
			if (!hasReverse)
			{
				Connection fwd = arc.getData();
				List<Track> fwdPath = fwd.getTrackPath();
				List<Track> revPath = new ArrayList<>();
				for (int i = fwdPath.size() - 1; i >= 0; i--)
				{
					Track t = fwdPath.get(i);
					revPath.add(new Track(Index.getTrackIndex(), t.getTo(), t.getFrom(), t.getLength(), t.getSpeed(),
							t.getDuration(), t.getNrOfTracks()));
				}
				Connection reverse = new Connection(Index.getConnectionIndex(), fwd.getTo(), fwd.getFrom(),
						fwd.getLength(), fwd.getDuration(), revPath, fwd.getMaxFreq(), fwd.getType());
				connectionNetwork.addArc(to, from, reverse, reverse.getDuration());
			}
		}
	}

	private static void printNetwork(DirectedGraph<StationNode, Connection> connectionNetwork)
	{
		for (StationNode node : connectionNetwork.getNodes())
		{
			List<String> neighbors = new ArrayList<>();
			for (DirectedGraphArc<StationNode, Connection> arc : connectionNetwork.getOutArcs(node))
			{
				neighbors.add(arc.getTo().getStation().getNameFull());
			}
			System.out.println(node.getStation().getNameFull() + " -> " + neighbors);
		}
	}

	public static List<ODPair> generateODPairs(String stationsFile, String outputFile) throws IOException
	{
		// Read stations with populations
		List<Station> stations = new ArrayList<>();
		Map<Station, Long> populations = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(stationsFile)))
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
				long population = Long.parseLong(parts[3].trim().replaceAll("[^\\d]", ""));
				boolean terminal = "Y".equals(parts[4].trim());
				double lat = Double.parseDouble(parts[5].trim());
				double lon = Double.parseDouble(parts[6].trim());
				Station station = new Station(Index.getStationIndex(), abbr, stopName, terminal, false, 2, false, lat,
						lon);
				stations.add(station);
				populations.put(station, population);
			}
		}

		// First pass: compute total demand
		double totalDemand = 0;
		double targetDemand = 0;
		for (Station origin : stations)
		{
			targetDemand += populations.get(origin)*0.05;
			for (Station destination : stations)
			{
				if (origin.equals(destination))
					continue;
				double dist = haversineDistanceKm(origin.getLatitude(), origin.getLongitude(),
						destination.getLatitude(), destination.getLongitude());
				if (dist > MAX_DEMAND_TRAVEL_DISTANCE)
					continue;
				totalDemand += Math.pow(populations.get(origin), POPULATION_EXPONENT)
						* Math.pow(populations.get(destination), POPULATION_EXPONENT)
						/ Math.pow(dist, DISTANCE_EXPONENT);
			}
		}

		// Second pass: compute scaled demand
		List<ODPair> odPairs = new ArrayList<>();
		for (Station origin : stations)
		{
			for (Station destination : stations)
			{
				if (origin.equals(destination))
					continue;
				double dist = haversineDistanceKm(origin.getLatitude(), origin.getLongitude(),
						destination.getLatitude(), destination.getLongitude());
				if (dist > MAX_DEMAND_TRAVEL_DISTANCE)
					continue;
				double demand = Math.pow(populations.get(origin), POPULATION_EXPONENT)
						* Math.pow(populations.get(destination), POPULATION_EXPONENT)
						/ Math.pow(dist, DISTANCE_EXPONENT);
				demand = (demand / totalDemand) * targetDemand;
				if (demand < DEMAND_LOWER_THRESHOLD)
					continue;
				odPairs.add(new ODPair(origin, destination, demand));
			}
		}

		writeODMatrix(odPairs, outputFile);
		return odPairs;
	}

	private static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2)
	{
		double R = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	private static void writeODMatrix(List<ODPair> odPairs, String outputFile) throws IOException
	{
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile)))
		{
			for (ODPair pair : odPairs)
				writer.printf(java.util.Locale.ROOT, "%s %s %.2f%n", pair.getOrigin().getName(),
						pair.getDestination().getName(), pair.getVolume());
		}
		System.out.println("OD pairs written to " + outputFile);
	}

	public static void writeConnections(DirectedGraph<StationNode, Connection> connectionNetwork, String outputFile)
			throws IOException
	{
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile)))
		{
			writer.println("FromAbbr;ToAbbr;Length_m;Duration_min;MaxFreq");
			List<DirectedGraphArc<StationNode, Connection>> arcs = new ArrayList<>(connectionNetwork.getArcs());
			arcs.sort((a, b) -> {
				int cmp = a.getFrom().getStation().getName().compareTo(b.getFrom().getStation().getName());
				return cmp != 0 ? cmp : a.getTo().getStation().getName().compareTo(b.getTo().getStation().getName());
			});
			for (DirectedGraphArc<StationNode, Connection> arc : arcs)
			{
				Connection c = arc.getData();
				writer.printf("%s;%s;%.0f;%.2f;%d%n", c.getFrom().getName(), c.getTo().getName(), c.getLength(),
						c.getDuration(), c.getMaxFreq());
			}
		}
		System.out.println("Connections written to " + outputFile);
	}

	private static Set<StationNode> createMajorStationNodes(List<String> majorStationAbbrs,
			DirectedGraph<StationNode, Track> graph)
	{
		// Identify major station nodes
		Set<StationNode> majorStations = new HashSet<>();
		for (StationNode node : graph.getNodes())
		{
			if (majorStationAbbrs.contains(node.getStation().getName()))
			{
				majorStations.add(node);
			}
		}
		return majorStations;
	}

	private static List<StationNode> findNearbyMajorStations(StationNode start, DirectedGraph<StationNode, Track> graph,
			Set<StationNode> majorStations)
	{
		List<StationNode> nearby = new ArrayList<>();
		Set<StationNode> visited = new HashSet<>();
		Deque<StationNode> stack = new ArrayDeque<>();

		stack.push(start);
		visited.add(start);

		while (!stack.isEmpty())
		{
			StationNode current = stack.pop();

			for (DirectedGraphArc<StationNode, Track> arc : graph.getOutArcs(current))
			{
				StationNode neighbor = arc.getTo();
				if (visited.contains(neighbor))
					continue;
				visited.add(neighbor);

				if (majorStations.contains(neighbor))
				{
					// Found a nearby major station — don't explore further from here
					nearby.add(neighbor);
				}
				else
				{
					stack.push(neighbor);
				}
			}
		}
		// System.out.println("Nearby of " + start + " is " + nearby);
		return nearby;
	}

	private static Connection getConnection(StationNode from, StationNode to,
			DirectedGraph<StationNode, Track> trackNetwork, int typeIC, boolean allowSPR,
			Set<StationNode> majorStations)
	{
		Dijkstra<StationNode, Track> dijkstra = new Dijkstra<>(trackNetwork, from);
		dijkstra.setDestination(to);
		dijkstra.computeDistances();
		double duration = dijkstra.getDistance(to);
		double length = 0;
		int maxFreq = 8; // default is 4 trains
		List<Track> trackPath = new ArrayList<>();

		for (DirectedGraphArc<StationNode, Track> arcInPath : dijkstra.getPath(to))
		{
			Track track = arcInPath.getData();
			trackPath.add(track);
			length += track.getLength();

			// If an intermediate station (i.e. the 'from' of this arc, excluding the
			// origin)
			// is a major station, this connection passes through another major station ->
			// reject
			if (!track.getFrom().equals(from.getStation()) && majorStations.contains(arcInPath.getFrom()))
			{
				return null;
			}
		}

		return new Connection(Index.getConnectionIndex(), from.getStation(), to.getStation(), length, duration,
				trackPath, maxFreq, typeIC);
	}

	public static void createMajorStationConnections(DirectedGraph<StationNode, Track> graph,
			Set<StationNode> majorStations, int typeIC, boolean allowSPR,
			DirectedGraph<StationNode, Connection> connectionNetwork)
	{

		List<Connection> connections = new ArrayList<>();

		for (StationNode major : majorStations)
		{
			List<StationNode> nearbyMajors = findNearbyMajorStations(major, graph, majorStations);
			for (StationNode nearby : nearbyMajors)
			{

				Connection connection = getConnection(major, nearby, graph, typeIC, allowSPR, majorStations);
				if (connection != null)
				{
					connections.add(connection);
					connectionNetwork.addArc(major, nearby, connection, connection.getDuration());
				}
			}
		}
	}

	private static StationNode getOrAddNode(DirectedGraph<StationNode, Track> graph, String abbr, String fullName,
			Map<String, Station> knownStations)
	{
		String resolvedAbbr = STATION_ALIASES.getOrDefault(abbr, abbr);
		for (StationNode node : graph.getNodes())
		{
			if (node.getStation().getName().equals(resolvedAbbr))
				return node;
		}
		Station station = knownStations.get(resolvedAbbr);
		if (station == null)
		{
			System.out.println("Warning: no coordinates found for station " + resolvedAbbr + " (" + fullName + ")");
			station = new Station(Index.getStationIndex(), resolvedAbbr, fullName, false, false, 0, false, 0.0, 0.0);
		}
		StationNode newNode = new StationNode(station);
		graph.addNode(newNode);
		return newNode;
	}

	/**
	 * Reads stationsWithPopulations.txt and returns a map from abbreviation to
	 * Station.
	 */
	private static Map<String, Station> readStationsFile(String filePath) throws IOException
	{
		Map<String, Station> stations = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
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
				String abbr = STATION_ALIASES.getOrDefault(parts[0].trim(), parts[0].trim());
				String stopName = parts[1].trim();
				boolean terminal = "Y".equals(parts[4].trim());
				double lat = Double.parseDouble(parts[5].trim());
				double lon = Double.parseDouble(parts[6].trim());
				stations.put(abbr,
						new Station(Index.getStationIndex(), abbr, stopName, terminal, false, 2, false, lat, lon));
			}
		}
		return stations;
	}

	/**
	 * Supplements the given stations map with entries from
	 * tracks-with-geoshapes.csv, using putIfAbsent so existing entries are not
	 * overwritten. GeoJSON stores coordinates as [lon, lat] (x=lon, y=lat).
	 */
	static void readTracksWithGeoshapes(String filePath, Map<String, Station> stations) throws IOException
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;
				String[] parts = parseTracksLine(line);
				if (parts == null)
					continue;
				String startAbbr = STATION_ALIASES.getOrDefault(parts[5].trim(), parts[5].trim());
				String startName = parts[6].trim();
				String endAbbr = STATION_ALIASES.getOrDefault(parts[7].trim(), parts[7].trim());
				String endName = parts[8].trim();
				double[][] coords = extractEndpointCoords(parts[1]);
				if (coords == null)
					continue;
				stations.putIfAbsent(startAbbr, new Station(Index.getStationIndex(), startAbbr, startName, false, false,
						0, false, coords[0][0], coords[0][1]));
				stations.putIfAbsent(endAbbr, new Station(Index.getStationIndex(), endAbbr, endName, false, false, 0,
						false, coords[1][0], coords[1][1]));
			}
		}
	}

	/**
	 * Parses one line of tracks-with-geoshapes.csv. Format:
	 * geoPoint;"geoShapeJson";trackGauge;kmStart;kmEnd;startAbbr;startName;endAbbr;endName;linie;line
	 * Returns array indexed as: 0=geoPoint, 1=geoShapeJson, 2=trackGauge,
	 * 3=kmStart, 4=kmEnd, 5=startAbbr, 6=startName, 7=endAbbr, 8=endName, 9=linie,
	 * 10=line
	 */
	private static String[] parseTracksLine(String line)
	{
		int firstSemi = line.indexOf(';');
		if (firstSemi < 0)
			return null;
		String geoPoint = line.substring(0, firstSemi);
		// geo shape field is quoted: ;"..."; — find closing ";
		int geoShapeStart = firstSemi + 2; // skip ';"'
		int geoShapeEnd = line.indexOf("\";", geoShapeStart);
		if (geoShapeEnd < 0)
			return null;
		String geoShapeJson = line.substring(geoShapeStart, geoShapeEnd).replace("\"\"", "\"");
		String[] rest = line.substring(geoShapeEnd + 2).split(";", -1);
		if (rest.length < 7)
			return null;
		String[] result = new String[11];
		result[0] = geoPoint;
		result[1] = geoShapeJson;
		for (int i = 0; i < rest.length && i + 2 < result.length; i++)
			result[i + 2] = rest[i];
		return result;
	}

	/**
	 * Extracts the first and last coordinate pairs from a GeoJSON LineString.
	 * GeoJSON uses [lon, lat] (x=lon, y=lat), so we return {lat, lon} pairs.
	 * Returns double[2][2]: result[0]={lat,lon} of start, result[1]={lat,lon} of
	 * end.
	 */
	private static double[][] extractEndpointCoords(String geoShapeJson)
	{
		Pattern p = Pattern.compile("\\[(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)\\]");
		Matcher m = p.matcher(geoShapeJson);
		double[] first = null, last = null;
		while (m.find())
		{
			double lon = Double.parseDouble(m.group(1)); // x = lon
			double lat = Double.parseDouble(m.group(2)); // y = lat
			if (first == null)
				first = new double[]
				{ lat, lon };
			last = new double[]
			{ lat, lon };
		}
		return (first == null) ? null : new double[][]
		{ first, last };
	}

	public static DirectedGraph<StationNode, Track> buildGraphFromFile(String filePath) throws IOException
	{
		return buildGraphFromFile(filePath, new HashMap<>());
	}

	public static DirectedGraph<StationNode, Track> buildGraphFromFile(String filePath,
			Map<String, Station> knownStations) throws IOException
	{
		DirectedGraph<StationNode, Track> graph = new DirectedGraph<>();
		List<Track> tracks = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;
				String[] parts = parseTracksLine(line);
				if (parts == null)
					continue;

				String fromAbbr = parts[5].trim();
				String fromName = parts[6].trim();
				String toAbbr = parts[7].trim();
				String toName = parts[8].trim();
				double kmStart = Double.parseDouble(parts[3].trim());
				double kmEnd = Double.parseDouble(parts[4].trim());
				double length = Math.abs(kmEnd - kmStart); // in meters
				double durationMin = (length / 1000.0) / AVERAGE_SPEED_KMH * 60.0;

				StationNode fromNode = getOrAddNode(graph, fromAbbr, fromName, knownStations);
				StationNode toNode = getOrAddNode(graph, toAbbr, toName, knownStations);

				if (fromNode == toNode)
					continue; // skip internal tracks between merged stations
				tracks.add(new Track(Index.getTrackIndex(), fromNode.getStation(), toNode.getStation(), length,
						AVERAGE_SPEED_KMH, durationMin, 1));
			}
		}

		for (Track track : tracks)
		{
			StationNode from = null;
			StationNode to = null;

			for (StationNode node : graph.getNodes())
			{
				if (node.getStation().equals(track.getFrom()))
					from = node;
				if (node.getStation().equals(track.getTo()))
					to = node;
			}

			graph.addArc(from, to, track, track.getDuration());

			Track backward = new Track(Index.getTrackIndex(), track.getTo(), track.getFrom(), track.getLength(),
					track.getSpeed(), track.getDuration(), track.getNrOfTracks());
			graph.addArc(to, from, backward, track.getDuration());
		}

		return graph;
	}

	public static List<StationNode> getStationsWithDegreGreaterThan2(DirectedGraph<StationNode, Track> graph)
	{
		List<StationNode> result = new ArrayList<>();

		for (StationNode node : graph.getNodes())
		{
			if (graph.getOutDegree(node) > 2)
			{
				result.add(node);
			}
		}

		return result;
	}

	public static void writeStationPopulations(List<String> majorStationAbbrs, String perronsFile, String outputFile)
			throws IOException
	{
		// Read perrons file: get stop name and average lat/lon per major station
		Set<String> majorSet = new HashSet<>(majorStationAbbrs);
		Map<String, String> majorStations = new HashMap<>();
		Map<String, double[]> latLonSum = new HashMap<>();
		Map<String, Integer> latLonCount = new HashMap<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(perronsFile)))
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
				String abbreviation = parts[0].trim();
				String stopName = parts[1].trim();
				if (!majorSet.contains(abbreviation))
					continue;
				majorStations.putIfAbsent(abbreviation, stopName);
				String[] geoparts = parts[4].trim().split(",");
				if (geoparts.length >= 2)
				{
					double lat = Double.parseDouble(geoparts[0].trim());
					double lon = Double.parseDouble(geoparts[1].trim());
					latLonSum.merge(abbreviation, new double[]
					{ lat, lon }, (a, b) -> new double[]
					{ a[0] + b[0], a[1] + b[1] });
					latLonCount.merge(abbreviation, 1, Integer::sum);
				}
			}
		}

		// Write output
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile)))
		{
			writer.println("Abbreviation;StopName;City;Population;Terminal;Latitude;Longitude");
			List<String> sortedAbbrevs = new ArrayList<>(majorStations.keySet());
			sortedAbbrevs.sort(String::compareTo);
			for (String abbr : sortedAbbrevs)
			{
				String stopName = majorStations.get(abbr);
				String cityName = extractCityName(stopName);
				int count = latLonCount.getOrDefault(abbr, 0);
				double[] sums = latLonSum.getOrDefault(abbr, new double[]
				{ 0.0, 0.0 });
				double lat = count > 0 ? sums[0] / count : 0.0;
				double lon = count > 0 ? sums[1] / count : 0.0;
				writer.printf("%s;%s;%s;0;N;%.8f;%.8f%n", abbr, stopName, cityName, lat, lon);
			}
		}
		System.out.println("Processed stations written to " + outputFile);
	}

	private static String extractCityName(String stopName)
	{
		String[] words = stopName.trim().split("\\s+");
		// If first word ends with "." (e.g. "St."), include the next word too
		if (words.length > 1 && words[0].endsWith("."))
			return words[0] + " " + words[1];
		return words[0];
	}

	/**
	 * Writes IllegalTurns.txt: turns at a station where the angle between the
	 * incoming and outgoing track is less than MIN_TURN_ANGLE_DEG degrees. Stations
	 * without known coordinates are skipped.
	 */
	public static void writeIllegalTurns(DirectedGraph<StationNode, Track> trackNetwork,
			Map<String, Station> knownStations, String outputFile) throws IOException
	{
		Map<String, double[]> coords = new HashMap<>();
		for (Map.Entry<String, Station> e : knownStations.entrySet())
			coords.put(e.getKey(), new double[]
			{ e.getValue().getLatitude(), e.getValue().getLongitude() });

		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile)))
		{
			writer.println("Van;Door;Naar");
			for (StationNode door : trackNetwork.getNodes())
			{
				double[] doorCoords = coords.get(door.getStation().getName());
				if (doorCoords == null)
					continue;
				List<DirectedGraphArc<StationNode, Track>> outArcs = new ArrayList<>(trackNetwork.getOutArcs(door));

				// The track network is symmetric: outgoing arcs from 'door' give all its
				// neighbours. A turn Van→Door→Naar exists whenever Van and Naar are both
				// neighbours of Door (and Van ≠ Naar).
				for (DirectedGraphArc<StationNode, Track> vanArc : outArcs)
				{
					StationNode van = vanArc.getTo();
					double[] vanCoords = coords.get(van.getStation().getName());
					if (vanCoords == null)
						continue;

					for (DirectedGraphArc<StationNode, Track> naarArc : outArcs)
					{
						StationNode naar = naarArc.getTo();
						if (naar.equals(van))
							continue; // skip U-turns (same neighbour)

						double[] naarCoords = coords.get(naar.getStation().getName());
						if (naarCoords == null)
							continue;

						double angle = turnAngleDegrees(doorCoords, vanCoords, naarCoords);
						if (angle < MIN_TURN_ANGLE_DEG)
							writer.printf("%s;%s;%s%n", van.getStation().getName(), door.getStation().getName(),
									naar.getStation().getName());
					}
				}
			}
		}
		System.out.println("Illegal turns written to " + outputFile);
	}

	/**
	 * Computes the angle at vertex B (door) between rays B→A (van) and B→C (naar),
	 * using an Euclidean approximation with longitude scaled by cos(lat).
	 */
	private static double turnAngleDegrees(double[] door, double[] van, double[] naar)
	{
		double cosLat = Math.cos(Math.toRadians(door[0]));
		double bax = van[0] - door[0];
		double bay = (van[1] - door[1]) * cosLat;
		double bcx = naar[0] - door[0];
		double bcy = (naar[1] - door[1]) * cosLat;
		double dot = bax * bcx + bay * bcy;
		double magBA = Math.sqrt(bax * bax + bay * bay);
		double magBC = Math.sqrt(bcx * bcx + bcy * bcy);
		if (magBA < 1e-9 || magBC < 1e-9)
			return 180.0;
		double cosAngle = Math.max(-1.0, Math.min(1.0, dot / (magBA * magBC)));
		return Math.toDegrees(Math.acos(cosAngle));
	}

	public static List<String> getLargeStations(String filePath, int x) throws IOException
	{
		Map<String, Set<String>> stationTracks = new HashMap<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
		{
			String line = reader.readLine(); // Skip header

			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank())
					continue;

				String[] parts = line.split(";");
				if (parts.length < 4)
					continue;

				String abbreviation = parts[0].trim();
				String[] trackNumbers = parts[3].trim().split("/");

				Set<String> tracks = stationTracks.computeIfAbsent(abbreviation, k -> new HashSet<>());
				for (String track : trackNumbers)
				{
					tracks.add(track.trim());
				}
			}
		}

		return stationTracks.entrySet().stream().filter(entry -> entry.getValue().size() >= x).map(Map.Entry::getKey)
				.sorted().collect(Collectors.toList());
	}

}
