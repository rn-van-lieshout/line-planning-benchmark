package linePlanning.instance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import linePlanning.CGN.CGN;

import linePlanning.DPTN.DPTN;
import linePlanning.PTN.Connection;
import linePlanning.lineGeneration.Line;
import linePlanning.routeGeneration.RouteGenerator;
import optimisation.columnGeneration.AbstractInstance;
import util.Reader;
import util.Triple;
import util.UnorderedPair;
import util.Writer;

public class LinePlanningInstance extends AbstractInstance
{
	private final String name;
	private final String folder;
	private String routeGenerationFile;
	private String lineGenerationFile;
	private String fixedCostFile;
	private String budgetFile;
	private String odMatrixFile;
	private String transferPenaltyFile;

	private final List<Station> stations;
	private final List<Track> tracks;
	private final List<Connection> connections;
	private final List<ODPair> ODPairs;
	private final List<UnorderedPair<Track, Track>> forbiddenTurns;
	private final List<Triple<Station, Station, Station>> allowedTypeChanges;

	private final int budget; // RANDSTAD-IC: 105, RANDSTAD-FULL: 175, NL-IC: 220, NL-FULL:
	private final double totalVolume;
	private final boolean hasICs;
	private final boolean hasSPRs;

	// Networks.
	private final DirectedGraph<StationNode, Track> trackNetwork;
	private final DirectedGraph<StationNode, Connection> connectionNetwork;
	private final Map<Track, List<Connection>> trackToConnections;

	// Lower bound computations
	private double lowerBound;
	private Map<ODPair, Double> lowerBoundPerOD;

	// Derived line/route pools and networks
	private Map<Station, StationNode> stationNodes;
	private Map<Station, StationODNode> departureODNodes;
	private Map<Station, StationODNode> arrivalODNodes;
	private DPTN DPTN;

	private List<Line> linePool;
	private CGN cgn;

	public LinePlanningInstance(String folder, String odMatrixFile, String budgetFile) throws IOException
	{
		this(folder, odMatrixFile, budgetFile, folder + "/transferPenalty/medium.txt", 1.0);
	}

	public LinePlanningInstance(String folder, String odMatrixFile, String budgetFile, String transferPenaltyFile) throws IOException
	{
		this(folder, odMatrixFile, budgetFile, transferPenaltyFile, 1.0);
	}

	public LinePlanningInstance(String folder, String odMatrixFile, String budgetFile, String transferPenaltyFile, double demandScalar) throws IOException
	{
		// Retrieve name from folder.
		this.folder = folder;
		name = new File(folder).getName();

		// Read stations.
		stations = Reader.readStations(folder + "/stations.txt");
		Map<String, Station> stationMap = new LinkedHashMap<>();
		for (Station station : stations)
		{
			stationMap.put(station.getName(), station);
		}

		stationNodes = new LinkedHashMap<>();
		for (Station station : stations)
		{
			stationNodes.put(station, new StationNode(station));
		}

		// Read tracks.
		tracks = Reader.readTracks(folder + "/tracks.txt", stationMap);
		Map<Integer, Track> trackIndexMap = new LinkedHashMap<>();
		for (Track track : tracks)
		{
			trackIndexMap.put(track.getIndex(), track);
		}

		// Read OD-pairs.
		this.odMatrixFile = odMatrixFile;
		ODPairs = Reader.readODPairs(odMatrixFile, stationMap, demandScalar);

		// Read transfer penalty.
		this.transferPenaltyFile = transferPenaltyFile;
		LinePlanningParameters.TRANSFER_PENALTY = Reader.readInt(transferPenaltyFile);

		// Read budget.
		this.budgetFile = budgetFile;
		budget = Reader.readBudget(budgetFile);
		hasICs = Reader.readBoolean(folder + "/hasICs.txt");
		hasSPRs = Reader.readBoolean(folder + "/hasSPRs.txt");

		// Read connections.
		connections = Reader.readConnections(folder + "/connections.txt", stationMap, trackIndexMap);
		forbiddenTurns = Reader.readForbiddenTurns(folder + "/forbiddenTurns.txt", trackIndexMap);
		allowedTypeChanges = Reader.readAllowedTypeChanges(folder + "/allowedTypeChanges.txt", stationMap);

		// Initialise networks.
		trackNetwork = initialiseTrackNetwork();
		connectionNetwork = initialiseConnectionNetwork();

		// Initialise other stuff.
		trackToConnections = computeTrackToConnections();
		totalVolume = computeTotalVolume();
		initialise();
	}

	public LinePlanningInstance(List<Station> stations, List<Track> tracks, List<Connection> connections,
			DirectedGraph<StationNode, Track> trackNetwork, DirectedGraph<StationNode, Connection> connectionNetwork,
			List<ODPair> oDPairs, List<UnorderedPair<Track, Track>> forbiddenTurns,
			List<Triple<Station, Station, Station>> allowedTypeChanges, int budget, boolean hasICs, boolean hasSPRs,
			String name)
	{
		super();

		this.folder = null;
		this.name = name;
		this.budget = budget;
		this.hasICs = hasICs;
		this.hasSPRs = hasSPRs;

		this.stations = stations;
		this.tracks = tracks;
		this.connections = connections;

		stationNodes = new LinkedHashMap<>();
		for (StationNode stationNode : trackNetwork.getNodes())
		{
			stationNodes.put(stationNode.getStation(), stationNode);
		}

		this.trackNetwork = trackNetwork;
		this.connectionNetwork = connectionNetwork;
		this.ODPairs = oDPairs;
		this.forbiddenTurns = forbiddenTurns;
		this.allowedTypeChanges = allowedTypeChanges;

		trackToConnections = computeTrackToConnections();
		totalVolume = computeTotalVolume();
		initialise();
	}

	private void initialise()
	{
		departureODNodes = new LinkedHashMap<>();
		arrivalODNodes = new LinkedHashMap<>();
		for (Station station : stations)
		{
			StationODNode dep = new StationODNode(station, true);
			StationODNode arr = new StationODNode(station, false);
			departureODNodes.put(station, dep);
			arrivalODNodes.put(station, arr);
		}

		for (ODPair od : ODPairs)
		{
			od.setOriginNode(departureODNodes.get(od.getOrigin()));
			od.setDestinationNode(arrivalODNodes.get(od.getDestination()));
		}

		this.DPTN = new DPTN(this, true, true);
		computeTravelTimeBounds();
	}

	private void computeTravelTimeBounds()
	{
		lowerBound = 0;
		lowerBoundPerOD = new HashMap<>();
		for (ODPair od : ODPairs)
		{
			double bound = DPTN.getMinimumTravelTime(od.getOrigin(), od.getDestination());
			lowerBound += bound * od.getVolume();
			lowerBoundPerOD.put(od, bound);
		}
	}

	private double computeTotalVolume()
	{
		double tot = 0;
		for (ODPair od : ODPairs)
		{
			tot += od.getVolume();
		}
		return tot;
	}

	private Map<Track, List<Connection>> computeTrackToConnections()
	{
		Map<Track, List<Connection>> map = new HashMap<>();
		for (DirectedGraphArc<StationNode, Track> trackArc : trackNetwork.getArcs())
		{
			Track track = trackArc.getData();
			List<Connection> connections = new ArrayList<>();
			for (DirectedGraphArc<StationNode, Connection> connectionArc : connectionNetwork.getArcs())
			{
				Connection connection = connectionArc.getData();
				if (connection.getTrackPath().contains(track))
				{
					connections.add(connection);
				}
			}
			map.put(track, connections);
		}
		return map;
	}

	public void deriveCGN(List<Line> lines)
	{
		this.cgn = CGN.generate(this, lines);
	}

	public DirectedGraph<StationNode, Track> getTrackNetwork()
	{
		return trackNetwork;
	}

	public DirectedGraph<StationNode, Connection> getConnectionNetwork()
	{
		return connectionNetwork;
	}

	public List<ODPair> getODPairs()
	{
		return ODPairs;
	}

	public String getFolder()
	{
		return folder;
	}

	public String getRouteGenerationFile()
	{
		return routeGenerationFile;
	}

	public void setRouteGenerationFile(String routeGenerationFile)
	{
		this.routeGenerationFile = routeGenerationFile;
	}

	public String getLineGenerationFile()
	{
		return lineGenerationFile;
	}

	public void setLineGenerationFile(String lineGenerationFile)
	{
		this.lineGenerationFile = lineGenerationFile;
	}

	public String getFixedCostFile()
	{
		return fixedCostFile;
	}

	public void setFixedCostFile(String fixedCostFile)
	{
		this.fixedCostFile = fixedCostFile;
	}

	public String getBudgetFile()
	{
		return budgetFile;
	}

	public String getOdMatrixFile()
	{
		return odMatrixFile;
	}

	public String getTransferPenaltyFile()
	{
		return transferPenaltyFile;
	}

	public int getBudget()
	{
		return budget;
	}

	public List<UnorderedPair<Track, Track>> getForbiddenTurns()
	{
		return forbiddenTurns;
	}

	public List<Station> getStations()
	{
		return stations;
	}

	public List<Connection> getConnections()
	{
		return connections;
	}

	public boolean feasibleTrackPath(Track in, Track out)
	{
		return !forbiddenTurns.contains(new UnorderedPair<Track, Track>(in, out));
	}

	public boolean sensibleTransfer(Line line1, boolean forward1, Line line2, boolean forward2, Station transferStation,
			boolean strict)
	{
		// first check whether the connections themselves make sense
		Connection incoming1 = line1.getIncomingConnection(transferStation, forward1);
		Connection outgoing2 = line2.getOutgoingConnection(transferStation, forward2);

		if (!sensibleTransfer(incoming1, outgoing2))
		{
			return false;
		}

		List<Station> remainingStations1 = line1.getRemainingStations(transferStation, forward1);
		if (remainingStations1.isEmpty())
		{
			return true;
		}
		List<Station> remainingStations2 = line2.getRemainingStations(transferStation, forward2);

		if (strict)
		{
			// check if the next station is different
			if (remainingStations1.get(0).equals(remainingStations2.get(0)))
			{
				return false;
			}
		}
		// transfer only makes sense if we can reach more stations
		if (remainingStations2.size() > remainingStations1.size())
		{
			return true;
		}
		for (Station station2 : remainingStations2)
		{
			if (!remainingStations1.contains(station2))
			{
				return true;
			}
		}
		// NOTE: could make this much stronger by only allowing transfers if the next
		// station is different
		return false;
	}

	public boolean sensibleTransfer(Connection incoming, Connection outgoing)
	{
		if (incoming.getFrom().equals(outgoing.getTo()))
		{
			return false;
		}
		if (incoming.getPassedStations().contains(outgoing.getTo()))
		{
			return false;
		}
		if (outgoing.getPassedStations().contains(incoming.getFrom()))
		{
			return false;
		}
		if ((name.contains("Randstad") || name.contains("Nederland")) && incoming.getLastTrack().getFrom().equals(outgoing.getFirstTrack().getTo()))
		{
			if (!incoming.getTo().getName().equals("Ut"))
			{
				return false;
			}
		}
		return true;
	}

	public boolean feasibleDriveArcSequence(Connection incoming, Connection outgoing)
	{
		Track in = incoming.getLastTrack();
		Track out = outgoing.getFirstTrack();

		// Not allowed to turn to same station
		if (in.getFrom().equals(out.getTo()))
		{
			return false;
		}

		if (!feasibleTrackPath(in, out))
		{
			return false;
		}
		if (incoming.getType() > outgoing.getType())
		{
			// we go from Intercity to Sprinter
			if (!allowedTypeChanges.contains(new Triple<>(incoming.getFrom(), incoming.getTo(), outgoing.getTo())))
			{
				return false;
			}
		}
		if (incoming.getType() < outgoing.getType())
		{
			// we go from Sprinter to Intercity
			if (!allowedTypeChanges.contains(new Triple<>(outgoing.getTo(), incoming.getTo(), incoming.getFrom())))
			{
				return false;
			}
		}
		return true;
	}

	public List<Connection> getConnections(Track t)
	{
		return trackToConnections.get(t);
	}

	public double getLowerBound()
	{
		return this.lowerBound;
	}

	public double getTotalVolume()
	{
		return totalVolume;
	}

	public DPTN getDPTN()
	{
		return DPTN;
	}

	public CGN getCGN()
	{
		return cgn;
	}

	public void setLinePool(List<Line> list)
	{
		this.linePool = list;
	}

	public List<Line> getLinePool()
	{
		return linePool;
	}

	public StationODNode getDepartureODNode(Station s)
	{
		return departureODNodes.get(s);
	}

	public StationODNode getArrivalODNode(Station s)
	{
		return arrivalODNodes.get(s);
	}

	public double getMissedDemandPenalty(ODPair od)
	{
		return LinePlanningParameters.MISSED_DEMAND_PENALTY_FACTOR * lowerBoundPerOD.get(od);
	}

	public double getAverageTTBound()
	{
		return this.lowerBound / this.totalVolume;
	}

	public boolean hasICs()
	{
		return hasICs;
	}

	public boolean hasSPRs()
	{
		return hasSPRs;
	}

	public StationNode getStationNode(Station s)
	{
		return stationNodes.get(s);
	}

	public void writeODMatrices(String folderLocation) throws IOException {
		for(int strategy = 1; strategy<=3; strategy++)
		{
			RouteGenerator rpGen = new RouteGenerator(this, routeGenerationFile);
			ODPairPreprocessor preproc = new ODPairPreprocessor(this, strategy, rpGen);
			Writer.write(preproc.getSplittedODs(), folder + "/ODMatrix/"+strategy+"-false.txt");
			Writer.write(preproc.getAggregatedODs(), folder + "/ODMatrix/"+strategy+"-true.txt");
		}
	}

	public void writeInstance(String folderLocation) throws IOException
	{
		// Create folder.
		File folder = new File(folderLocation + "/" + name);
		folder.mkdir();
		folder.mkdirs();

		// Write data.
		Writer.write(stations, folder + "/stations.txt");
		Writer.write(tracks, folder + "/tracks.txt");
		Writer.write(connections, folder + "/connections.txt");
		new File(folder + "/ODMatrix").mkdirs();
		Writer.write(ODPairs, folder + "/ODMatrix/0.txt");
		for(int strategy = 1; strategy<=3; strategy++)
		{
			RouteGenerator rpGen = new RouteGenerator(this, routeGenerationFile);
			ODPairPreprocessor preproc = new ODPairPreprocessor(this, strategy, rpGen);
			Writer.write(preproc.getSplittedODs(), folder + "/ODMatrix/"+strategy+"-false.txt");
			Writer.write(preproc.getAggregatedODs(), folder + "/ODMatrix/"+strategy+"-true.txt");
		}
		Writer.write("" + budget, folder + "/budget.txt");
		Writer.write("" + hasICs, folder + "/hasICs.txt");
		Writer.write("" + hasSPRs, folder + "/hasSPRs.txt");

		// Write forbidden turns.
		String output = "";
		for (UnorderedPair<Track, Track> turn : forbiddenTurns)
		{
			output += turn.getFirst().getIndex() + " " + turn.getSecond().getIndex() + "\n";
		}
		Writer.write(output, folder + "/forbiddenTurns.txt");

		// Write allowed type changes.
		output = "";
		for (Triple<Station, Station, Station> triple : allowedTypeChanges)
		{
			output += triple.getFirst().getName() + " " + triple.getSecond().getName() + " " + triple.getThird() + "\n";
		}
		Writer.write(output, folder + "/allowedTypeChanges.txt");
	}

	private DirectedGraph<StationNode, Track> initialiseTrackNetwork()
	{
		DirectedGraph<StationNode, Track> trackNetwork = new DirectedGraph<>();
		for (Station station : stations)
		{
			trackNetwork.addNode(stationNodes.get(station));
		}
		for (Track track : tracks)
		{
			StationNode from = stationNodes.get(track.getFrom());
			StationNode to = stationNodes.get(track.getTo());
			trackNetwork.addArc(from, to, track, track.getDuration());
		}
		return trackNetwork;
	}

	private DirectedGraph<StationNode, Connection> initialiseConnectionNetwork()
	{
		DirectedGraph<StationNode, Connection> connectionNetwork = new DirectedGraph<>();
		Set<StationNode> added = new HashSet<>();
		for (Connection connection : connections)
		{
			StationNode from = stationNodes.get(connection.getFrom());
			StationNode to = stationNodes.get(connection.getTo());
			if (added.add(from)) connectionNetwork.addNode(from);
			if (added.add(to))   connectionNetwork.addNode(to);
			connectionNetwork.addArc(from, to, connection, connection.getDuration());
		}
		return connectionNetwork;
	}
}
