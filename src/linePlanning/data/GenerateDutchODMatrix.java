package linePlanning.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graph.algorithms.shortestPath.Dijkstra;
import linePlanning.DPTN.DPTN;
import linePlanning.DPTN.DPTNArc;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.instance.ODPair;
import linePlanning.instance.Station;
import linePlanning.instance.StationNode;
import linePlanning.instance.StationODNode;
import util.Configuration;

/**
 * Generates gravity-model OD matrices for all four Dutch instances.
 * Impedance is minimum travel time over the Nederland-IC-SPR DPTN (one
 * Dijkstra per origin). The IC and Randstad variants are produced by
 * filtering the full Nederland-IC-SPR pairs (same normalization/demand).
 *
 * Population data: dataDutch/stationsWithPopulations.txt
 * Output: instances/Nederland-IC-SPR/ODMatrix/gravity.txt
 *         instances/Randstad-IC-SPR/ODMatrix/gravity.txt
 *         instances/Nederland-IC/ODMatrix/gravity.txt
 *         instances/Randstad-IC/ODMatrix/gravity.txt
 */
public class GenerateDutchODMatrix
{
	// --- Tunable parameters ---
	/** Fraction of total station population that becomes train demand.
	 *  Scaled up from 0.0677 to compensate for demand cut by threshold. */
	private static final double TARGET_DEMAND_FACTOR    = 0.05;
	/** Gravity exponent for population (both origin and destination). */
	private static final double POPULATION_EXPONENT     = 1.0;
	/**
	 * Squared Lorentzian deterrence: f(t) = t² / (t² + PEAK²)².
	 * Peaks at t = PEAK, rises as t² near zero (strong suppression of very
	 * short trips), decays as 1/t² for large t.
	 *
	 * DPTN Dijkstra distances are in MINUTES (driving arcs store
	 * connection.getDuration() directly; transfer arcs store the raw penalty).
	 *
	 * At t=3   (3 min, adjacent stations): f ≈ 0.001% of peak (strongly suppressed).
	 * At t=30  (30 min, peak):             f = peak.
	 * At t=55  (55 min, Ams→Rtd+transfer): f ≈ 71% of peak.
	 * At t=120 (120 min, long trip):       f ≈ 22% of peak.
	 * At t=300 (300 min, very long trip):  f ≈  4% of peak.
	 */
	private static final double TRAVEL_TIME_PEAK        = 30.0;  // peak at 30 min
	/** OD pairs with scaled demand below this are dropped (controls density). */
	private static final double DEMAND_LOWER_THRESHOLD  = 3.5;
	// --------------------------

	private static final String INSTANCE_FOLDER             = "instances/Nederland-IC-SPR";
	private static final String RANDSTAD_INSTANCE_FOLDER    = "instances/Randstad-IC-SPR";
	private static final String IC_INSTANCE_FOLDER          = "instances/Nederland-IC";
	private static final String RANDSTAD_IC_INSTANCE_FOLDER = "instances/Randstad-IC";
	private static final String POPULATION_FILE             = "dataDutch/stationsWithPopulations.txt";
	private static final String OUTPUT_FILE                 = INSTANCE_FOLDER + "/ODMatrix/gravity.txt";
	private static final String RANDSTAD_OUTPUT_FILE        = RANDSTAD_INSTANCE_FOLDER + "/ODMatrix/gravity.txt";
	private static final String IC_OUTPUT_FILE              = IC_INSTANCE_FOLDER + "/ODMatrix/gravity.txt";
	private static final String RANDSTAD_IC_OUTPUT_FILE     = RANDSTAD_IC_INSTANCE_FOLDER + "/ODMatrix/gravity.txt";

	public static void main(String[] args) throws IOException
	{
		Configuration.initialiseConfiguration("settings.properties");
		generateODMatrix(INSTANCE_FOLDER, POPULATION_FILE, OUTPUT_FILE);
	}

	public static void generateODMatrix(String instanceFolder, String populationFile,
			String outputFile) throws IOException
	{
		// Load instance so we get the DPTN with real network travel times.
		// The existing 0.txt OD matrix is only used internally for lower-bound
		// precomputation; it does not affect the DPTN structure.
		System.out.println("Loading instance...");
		LinePlanningInstance instance = new LinePlanningInstance(
				instanceFolder,
				instanceFolder + "/ODMatrix/old/0.txt",
				instanceFolder + "/budget/medium.txt",
				instanceFolder + "/transferPenalty/medium.txt");

		List<Station> stations = instance.getStations();
		DPTN dptn = instance.getDPTN();

		// Map populations from file to instance stations by abbreviation code.
		Map<String, Long> popByCode = readPopulations(populationFile);
		Map<Station, Long> populations = new LinkedHashMap<>();
		int missing = 0;
		for (Station s : stations)
		{
			Long pop = popByCode.get(s.getName());
			if (pop == null)
			{
				System.out.printf("  WARNING: no population for station %s, defaulting to 10000%n",
						s.getName());
				pop = 10_000L;
				missing++;
			}
			populations.put(s, pop);
		}
		if (missing == 0)
			System.out.printf("Populations loaded for all %d stations%n", stations.size());

		// Compute all-pairs minimum travel times: one Dijkstra per origin.
		// Avoids 256*255 individual shortest-path calls.
		System.out.println("Computing all-pairs travel times over DPTN...");
		Map<Station, Map<Station, Double>> travelTimes =
				computeAllPairsTravelTimes(stations, dptn, instance);

		// Generate Nederland-IC-SPR matrix (all stations).
		System.out.println("\n--- Nederland-IC-SPR ---");
		List<ODPair> nederlandPairs = computeODPairs(stations, populations, travelTimes);
		writeODMatrix(nederlandPairs, outputFile);
		printStats(nederlandPairs, stations.size());

		// Generate Randstad-IC-SPR matrix: filter Nederland pairs to Randstad×Randstad.
		// Demand values are identical to the Nederland matrix (same normalization).
		Set<Station> randstadSet = new HashSet<>();
		for (Station s : stations)
			if (s.isInRandstad()) randstadSet.add(s);

		List<ODPair> randstadPairs = new ArrayList<>();
		for (ODPair p : nederlandPairs)
			if (randstadSet.contains(p.getOrigin()) && randstadSet.contains(p.getDestination()))
				randstadPairs.add(p);

		System.out.printf("%nRandstad stations: %d / %d%n", randstadSet.size(), stations.size());
		System.out.println("--- Randstad-IC-SPR ---");
		writeODMatrix(randstadPairs, RANDSTAD_OUTPUT_FILE);
		printStats(randstadPairs, randstadSet.size());

		// Generate Nederland-IC matrix: filter to IC stations (type == INTERCITY).
		Set<Station> icSet = new HashSet<>();
		for (Station s : stations)
			if (s.getType() == LinePlanningParameters.INTERCITY) icSet.add(s);

		List<ODPair> icPairs = new ArrayList<>();
		for (ODPair p : nederlandPairs)
			if (icSet.contains(p.getOrigin()) && icSet.contains(p.getDestination()))
				icPairs.add(p);

		System.out.printf("%nIC stations: %d / %d%n", icSet.size(), stations.size());
		System.out.println("--- Nederland-IC ---");
		writeODMatrix(icPairs, IC_OUTPUT_FILE);
		printStats(icPairs, icSet.size());

		// Generate Randstad-IC matrix: IC stations that are also in the Randstad.
		List<ODPair> randstadIcPairs = new ArrayList<>();
		for (ODPair p : icPairs)
			if (randstadSet.contains(p.getOrigin()) && randstadSet.contains(p.getDestination()))
				randstadIcPairs.add(p);

		long randstadIcCount = icSet.stream().filter(randstadSet::contains).count();
		System.out.printf("%nRandstad-IC stations: %d%n", randstadIcCount);
		System.out.println("--- Randstad-IC ---");
		writeODMatrix(randstadIcPairs, RANDSTAD_IC_OUTPUT_FILE);
		printStats(randstadIcPairs, (int) randstadIcCount);
	}

	private static List<ODPair> computeODPairs(List<Station> stations, Map<Station, Long> populations,
			Map<Station, Map<Station, Double>> travelTimes)
	{
		double targetDemand = 0;
		for (Station s : stations)
			targetDemand += populations.get(s);
		targetDemand *= TARGET_DEMAND_FACTOR;

		// First pass: sum raw gravity scores (for scaling).
		double rawTotal = 0;
		for (Station o : stations)
		{
			Map<Station, Double> ttFromO = travelTimes.get(o);
			for (Station d : stations)
			{
				if (o == d) continue;
				Double tt = ttFromO.get(d);
				if (tt == null) continue;
				rawTotal += gravityRaw(populations.get(o), populations.get(d), tt);
			}
		}

		// Second pass: scale, filter, collect.
		List<ODPair> odPairs = new ArrayList<>();
		for (Station o : stations)
		{
			Map<Station, Double> ttFromO = travelTimes.get(o);
			for (Station d : stations)
			{
				if (o == d) continue;
				Double tt = ttFromO.get(d);
				if (tt == null) continue;
				double demand = gravityRaw(populations.get(o), populations.get(d), tt)
						/ rawTotal * targetDemand;
				if (demand < DEMAND_LOWER_THRESHOLD) continue;
				odPairs.add(new ODPair(o, d, demand));
			}
		}
		return odPairs;
	}

	private static void printStats(List<ODPair> odPairs, int nStations)
	{
		int n = odPairs.size();
		long maxPossible = (long) nStations * (nStations - 1);
		double sumDemand = 0, minDemand = Double.MAX_VALUE, maxDemand = 0;
		for (ODPair p : odPairs)
		{
			double d = p.getVolume();
			sumDemand += d;
			if (d < minDemand) minDemand = d;
			if (d > maxDemand) maxDemand = d;
		}
		System.out.printf("OD pairs written : %,d  (density %.2f%%)%n",
				n, 100.0 * n / maxPossible);
		System.out.printf("Total demand     : %,.2f%n", sumDemand);
		System.out.printf("Mean demand      : %,.4f%n", n > 0 ? sumDemand / n : 0);
		System.out.printf("Min demand       : %,.4f%n", n > 0 ? minDemand : 0);
		System.out.printf("Max demand       : %,.4f%n", maxDemand);
	}

	/** Raw (unscaled) gravity score between one OD pair. */
	private static double gravityRaw(long popO, long popD, double travelTime)
	{
		double t2 = travelTime * travelTime;
		double a2 = TRAVEL_TIME_PEAK * TRAVEL_TIME_PEAK;
		double denom = t2 + a2;
		double lorentz = t2 / (denom * denom);
		return Math.pow(popO, POPULATION_EXPONENT)
			 * Math.pow(popD, POPULATION_EXPONENT)
			 * lorentz;
	}

	/**
	 * Runs one single-source Dijkstra per origin over the DPTN and collects the
	 * minimum travel time to every reachable destination arrival OD node.
	 * Unreachable destinations are absent from the inner map.
	 */
	private static Map<Station, Map<Station, Double>> computeAllPairsTravelTimes(
			List<Station> stations, DPTN dptn, LinePlanningInstance instance)
	{
		Map<Station, Map<Station, Double>> result = new LinkedHashMap<>();
		int done = 0;
		for (Station o : stations)
		{
			StationODNode depNode = instance.getDepartureODNode(o);
			Dijkstra<StationNode, DPTNArc> dijkstra = new Dijkstra<>(dptn, depNode);
			dijkstra.computeDistances(); // full single-source, no early stop

			Map<Station, Double> ttFromO = new LinkedHashMap<>();
			for (Station d : stations)
			{
				if (o == d) continue;
				StationODNode arrNode = instance.getArrivalODNode(d);
				if (dijkstra.containsPath(arrNode))
					ttFromO.put(d, dijkstra.getDistance(arrNode));
			}
			result.put(o, ttFromO);

			done++;
			if (done % 32 == 0)
				System.out.printf("  %d / %d origins processed%n", done, stations.size());
		}
		return result;
	}

	private static Map<String, Long> readPopulations(String file) throws IOException
	{
		Map<String, Long> result = new LinkedHashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			br.readLine(); // skip header
			String line;
			while ((line = br.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty()) continue;
				String[] p = line.split(";");
				if (p.length < 3) continue;
				result.put(p[0].trim(), Long.parseLong(p[2].trim()));
			}
		}
		return result;
	}

	private static void writeODMatrix(List<ODPair> pairs, String outputFile) throws IOException
	{
		try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile)))
		{
			for (ODPair p : pairs)
				pw.printf(java.util.Locale.ROOT, "%s %s %.2f%n",
						p.getOrigin().getName(), p.getDestination().getName(), p.getVolume());
		}
		System.out.println("Written to " + outputFile);
	}
}
