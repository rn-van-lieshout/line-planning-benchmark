package linePlanning.scripts.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prints demand statistics for an OD matrix file.
 * Run from the project root (where instances/ lives).
 */
public class ODMatrixStats
{
	private static final String DATA_FOLDER = "instances";

	public static void main(String[] args) throws IOException
	{
		// 0 = Randstad-IC, 1 = Randstad-IC-SPR, 2 = Nederland-IC, 3 = Nederland-IC-SPR,
		// 4 = SmallSwitzerland, 5 = Switzerland
		int instanceIndex = 3;
		String instanceFolder = DATA_FOLDER + "/" + switch (instanceIndex)
		{
		case 0 -> "Randstad-IC";
		case 1 -> "Randstad-IC-SPR";
		case 2 -> "Nederland-IC";
		case 3 -> "Nederland-IC-SPR";
		case 4 -> "SmallSwitzerland";
		case 5 -> "Switzerland";
		default -> throw new IllegalArgumentException("Unknown instance index: " + instanceIndex);
		};

		// 0 = raw, 1 = strategy1-split, 2 = strategy1-agg, 3 = strategy2-split, 4 =
		// strategy2-agg, 5 = strategy3-split, 6 = strategy3-agg
		int odMatrixIndex = 0;
		String odMatrixVariant = switch (odMatrixIndex)
		{
		case 0 -> "gravity";
		case 1 -> "1-false";
		case 2 -> "1-true";
		case 3 -> "2-false";
		case 4 -> "2-true";
		case 5 -> "3-false";
		case 6 -> "3-true";
		default -> throw new IllegalArgumentException("Unknown OD matrix index: " + odMatrixIndex);
		};
		String odMatrixFile = instanceFolder + "/ODMatrix/" + odMatrixVariant + ".txt";

		System.out.println("Instance : " + instanceFolder);
		System.out.println("OD file  : " + odMatrixFile);
		System.out.println();

		printStats(odMatrixFile);
	}

	public static void printStats(String file) throws IOException
	{
		List<String[]> rawPairs = readRawPairs(file);

		List<Double> volumes = new ArrayList<>(rawPairs.size());
		Set<String> origins = new HashSet<>();
		Set<String> destinations = new HashSet<>();
		Set<String> allStations = new HashSet<>();

		for (String[] row : rawPairs)
		{
			String o = row[0];
			String d = row[1];
			double v = Double.parseDouble(row[2]);
			origins.add(o);
			destinations.add(d);
			allStations.add(o);
			allStations.add(d);
			volumes.add(v);
		}

		Collections.sort(volumes);

		int n = volumes.size();
		int nStations = allStations.size();
		long maxPairs = (long) nStations * (nStations - 1);
		double density = (double) n / maxPairs;

		double totalDemand = volumes.stream().mapToDouble(Double::doubleValue).sum();
		double minDemand   = volumes.get(0);
		double maxDemand   = volumes.get(n - 1);
		double mean        = totalDemand / n;
		double median      = percentile(volumes, 50);
		double p25         = percentile(volumes, 25);
		double p75         = percentile(volumes, 75);
		double p90         = percentile(volumes, 90);
		double stdDev      = stdDev(volumes, mean);
		double gini        = gini(volumes);

		System.out.printf("OD pairs          : %,d%n", n);
		System.out.printf("Unique stations   : %d  (origins: %d, destinations: %d)%n",
			nStations, origins.size(), destinations.size());
		System.out.printf("Max possible pairs: %,d%n", maxPairs);
		System.out.printf("Density           : %.4f  (%.2f%%)%n", density, density * 100);
		System.out.println();
		System.out.printf("Total demand      : %,.2f%n", totalDemand);
		System.out.printf("Mean demand       : %,.4f%n", mean);
		System.out.printf("Std deviation     : %,.4f%n", stdDev);
		System.out.printf("Median (p50)      : %,.4f%n", median);
		System.out.printf("p25               : %,.4f%n", p25);
		System.out.printf("p75               : %,.4f%n", p75);
		System.out.printf("p90               : %,.4f%n", p90);
		System.out.printf("Min               : %,.4f%n", minDemand);
		System.out.printf("Max               : %,.4f%n", maxDemand);
		System.out.printf("Gini coefficient  : %.4f%n", gini);
		System.out.println();

		// Share of total demand carried by top-k pairs
		System.out.println("Demand concentration:");
		for (int k : new int[]{ 10, 50, 100 })
		{
			if (k > n) continue;
			double topK = volumes.subList(n - k, n).stream().mapToDouble(Double::doubleValue).sum();
			System.out.printf("  Top %3d pairs: %,.2f  (%.1f%% of total)%n", k, topK, 100.0 * topK / totalDemand);
		}
		System.out.println();

		// Demand histogram (8 equal-width bins)
		System.out.println("Demand histogram (8 bins):");
		printHistogram(volumes, minDemand, maxDemand, 8);
		System.out.println();

		// Top-10 OD pairs
		System.out.println("Top-10 OD pairs by demand:");
		List<String[]> sortedPairs = new ArrayList<>(rawPairs);
		sortedPairs.sort((a, b) -> Double.compare(Double.parseDouble(b[2]), Double.parseDouble(a[2])));
		System.out.printf("  %-6s  %-6s  %s%n", "Origin", "Dest", "Volume");
		for (int i = 0; i < Math.min(10, sortedPairs.size()); i++)
		{
			String[] r = sortedPairs.get(i);
			System.out.printf("  %-6s  %-6s  %,.4f%n", r[0], r[1], Double.parseDouble(r[2]));
		}
	}

	private static List<String[]> readRawPairs(String file) throws IOException
	{
		List<String[]> result = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] parts = line.trim().split("\\s+");
				if (parts.length >= 3)
					result.add(new String[]{ parts[0], parts[1], parts[2] });
			}
		}
		return result;
	}

	private static double percentile(List<Double> sorted, double p)
	{
		if (sorted.isEmpty()) return 0;
		double rank = p / 100.0 * (sorted.size() - 1);
		int lo = (int) rank;
		int hi = Math.min(lo + 1, sorted.size() - 1);
		return sorted.get(lo) + (rank - lo) * (sorted.get(hi) - sorted.get(lo));
	}

	private static double stdDev(List<Double> values, double mean)
	{
		double sum = 0;
		for (double v : values)
			sum += (v - mean) * (v - mean);
		return Math.sqrt(sum / values.size());
	}

	/** Gini coefficient of an already-sorted list (ascending). */
	private static double gini(List<Double> sorted)
	{
		int n = sorted.size();
		if (n == 0) return 0;
		double sumNumerator = 0;
		for (int i = 0; i < n; i++)
			sumNumerator += (2.0 * (i + 1) - n - 1) * sorted.get(i);
		double total = sorted.stream().mapToDouble(Double::doubleValue).sum();
		if (total == 0) return 0;
		return sumNumerator / ((double) n * total);
	}

	private static void printHistogram(List<Double> sorted, double min, double max, int bins)
	{
		double width = (max - min) / bins;
		int[] counts = new int[bins];
		double[] sums = new double[bins];
		for (double v : sorted)
		{
			int b = Math.min((int) ((v - min) / width), bins - 1);
			counts[b]++;
			sums[b] += v;
		}
		int maxCount = 0;
		for (int c : counts) maxCount = Math.max(maxCount, c);
		int barWidth = 30;
		for (int b = 0; b < bins; b++)
		{
			double lo = min + b * width;
			double hi = lo + width;
			int bar = maxCount > 0 ? (int) Math.round((double) counts[b] / maxCount * barWidth) : 0;
			System.out.printf("  [%8.2f, %8.2f)  %5d  |%-" + barWidth + "s|%n",
				lo, hi, counts[b], "#".repeat(bar));
		}
	}
}
