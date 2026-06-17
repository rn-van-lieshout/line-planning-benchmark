package linePlanning.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1. Combines results.csv files from all Exp* subdirectories of Experiments/
 *    into Experiments/combinedResults.csv.
 * 2. Finds the best feasible solution per solver family for each instance
 *    setting and writes Experiments/bestPerFamily.csv.
 * 3. Finds the single best feasible solution across all families for each
 *    instance setting and writes Experiments/bestOverall.csv.
 */
public class CombineResults
{
	private static final String EXPERIMENTS_DIR   = "Experiments";
	private static final String COMBINED_FILE     = EXPERIMENTS_DIR + "/combinedResults.csv";
	private static final String BEST_FAMILY_FILE  = EXPERIMENTS_DIR + "/bestPerFamily.csv";
	private static final String BEST_OVERALL_FILE = EXPERIMENTS_DIR + "/bestOverall.csv";

	private static final String[] INSTANCE_KEYS = {
		"Instance", "ODMatrix", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost"
	};

	private static final String[] FAMILIES = { "PTN", "PTN+DC", "DPTN", "CGN" };

	private static final String[] BEST_FAMILY_COLUMNS = {
		"Instance", "ODMatrix", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost",
		"Family", "NrFeasible", "NrTotal",
		"BestSolver", "BestSetting",
		"BestObjectiveActual", "BestComputationTimeSeconds", "BestOptimalityGap",
		"BestDirectTravelersPct", "BestOneTransferPct", "BestTwoPlusTransfersPct",
		"BestUncoveredDemandPct"
	};

	private static final String[] BEST_OVERALL_COLUMNS = {
		"Instance", "ODMatrix", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost",
		"BestFamily", "BestSolver", "BestSetting",
		"BestObjectiveActual", "BestComputationTimeSeconds", "BestOptimalityGap",
		"BestDirectTravelersPct", "BestOneTransferPct", "BestTwoPlusTransfersPct",
		"BestUncoveredDemandPct"
	};

	// ── Main ─────────────────────────────────────────────────────────────────

	public static void main(String[] args) throws IOException
	{
		List<Map<String, String>> rows = combineResults();
		bestPerFamily(rows);
		bestOverall(rows);
	}

	// ── Step 1: combine ───────────────────────────────────────────────────────

	private static List<Map<String, String>> combineResults() throws IOException
	{
		File experimentsDir = new File(EXPERIMENTS_DIR);
		File[] expDirs = experimentsDir.listFiles(f -> f.isDirectory() && f.getName().matches("Exp\\d+"));
		if (expDirs == null || expDirs.length == 0)
		{
			System.out.println("No Exp* directories found in " + experimentsDir.getAbsolutePath());
			return Collections.emptyList();
		}
		Arrays.sort(expDirs, (a, b) -> {
			int na = Integer.parseInt(a.getName().substring(3));
			int nb = Integer.parseInt(b.getName().substring(3));
			return Integer.compare(na, nb);
		});

		String header = null;
		List<Map<String, String>> rows = new ArrayList<>();

		for (File dir : expDirs)
		{
			File csv = new File(dir, "results.csv");
			if (!csv.exists())
			{
				System.out.println("Skipping " + dir.getName() + " (no results.csv)");
				continue;
			}
			try (BufferedReader br = new BufferedReader(new FileReader(csv)))
			{
				String line = br.readLine();
				if (line == null) continue;
				String[] headers;
				if (header == null)
				{
					header = line;
					headers = splitCsvLine(header);
				}
				else
				{
					headers = splitCsvLine(header);
				}
				int count = 0;
				while ((line = br.readLine()) != null)
				{
					if (line.isBlank()) continue;
					String[] vals = splitCsvLine(line);
					Map<String, String> row = new LinkedHashMap<>();
					for (int i = 0; i < headers.length; i++)
						row.put(headers[i].trim(), i < vals.length ? vals[i].trim() : "");
					rows.add(row);
					count++;
				}
				System.out.println(dir.getName() + ": " + count + " rows");
			}
		}

		if (header == null)
		{
			System.out.println("No data found.");
			return Collections.emptyList();
		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(COMBINED_FILE)))
		{
			bw.write(header);
			bw.newLine();
			for (Map<String, String> row : rows)
			{
				bw.write(String.join(",", row.values()));
				bw.newLine();
			}
		}
		System.out.println("Written " + rows.size() + " rows to " + COMBINED_FILE);
		return rows;
	}

	// ── Step 2: best per family ───────────────────────────────────────────────

	private static void bestPerFamily(List<Map<String, String>> rows) throws IOException
	{
		Map<String, Map<String, List<Map<String, String>>>> grouped = new LinkedHashMap<>();
		int skipped = 0;
		for (Map<String, String> row : rows)
		{
			String solver = row.getOrDefault("Solver", "");
			String family = getFamily(solver);
			if (family == null) { skipped++; continue; }
			String key = instanceKey(row);
			grouped.computeIfAbsent(key, k -> new LinkedHashMap<>())
			       .computeIfAbsent(family, f -> new ArrayList<>())
			       .add(row);
		}
		if (skipped > 0)
			System.out.println("Skipped " + skipped + " rows with unrecognised solver.");

		List<Map<String, String>> outRows = new ArrayList<>();
		for (Map.Entry<String, Map<String, List<Map<String, String>>>> e1 : grouped.entrySet())
		{
			Map<String, String> instanceFields = parseInstanceKey(e1.getKey());
			for (String family : FAMILIES)
			{
				List<Map<String, String>> familyRows = e1.getValue().getOrDefault(family, Collections.emptyList());
				Map<String, String> out = new LinkedHashMap<>();

				for (String k : INSTANCE_KEYS)
					out.put(k, instanceFields.getOrDefault(k, ""));
				out.put("Family", family);

				int nrFeasible = 0;
				Map<String, String> best = null;
				for (Map<String, String> r : familyRows)
				{
					if ("true".equalsIgnoreCase(r.get("Feasible")))
						nrFeasible++;
					if (best == null || parseDouble(r.get("ObjectiveActual")) < parseDouble(best.get("ObjectiveActual")))
						best = r;
				}
				out.put("NrFeasible", String.valueOf(nrFeasible));
				out.put("NrTotal",    String.valueOf(familyRows.size()));

				if (best != null)
				{
					boolean feasible = "true".equalsIgnoreCase(best.get("Feasible"));
					out.put("BestSolver",                best.getOrDefault("Solver", ""));
					out.put("BestSetting",               getBestSetting(best));
					out.put("BestObjectiveActual",        best.getOrDefault("ObjectiveActual", ""));
					out.put("BestComputationTimeSeconds", feasible ? best.getOrDefault("ComputationTimeSeconds", "") : "3600");
					out.put("BestOptimalityGap",          best.getOrDefault("OptimalityGap", ""));
					out.put("BestDirectTravelersPct",     best.getOrDefault("DirectTravelersActualPct", ""));
					out.put("BestOneTransferPct",         best.getOrDefault("OneTransferActualPct", ""));
					out.put("BestTwoPlusTransfersPct",    best.getOrDefault("TwoPlusTransfersActualPct", ""));
					out.put("BestUncoveredDemandPct",     best.getOrDefault("UncoveredDemandActualPct", ""));
				}
				outRows.add(out);
			}
		}

		writeCsvWithColumns(BEST_FAMILY_FILE, BEST_FAMILY_COLUMNS, outRows);
		System.out.println("Written " + outRows.size() + " rows to " + BEST_FAMILY_FILE);
	}

	// ── Step 3: best overall ─────────────────────────────────────────────────

	private static void bestOverall(List<Map<String, String>> rows) throws IOException
	{
		Map<String, Map<String, String>> best = new LinkedHashMap<>();
		for (Map<String, String> row : rows)
		{
			String solver = row.getOrDefault("Solver", "");
			String family = getFamily(solver);
			if (family == null) continue;
			String key = instanceKey(row);
			Map<String, String> current = best.get(key);
			if (current == null || parseDouble(row.get("ObjectiveActual")) < parseDouble(current.get("ObjectiveActual")))
				best.put(key, row);
		}

		List<Map<String, String>> outRows = new ArrayList<>();
		for (Map.Entry<String, Map<String, String>> e : best.entrySet())
		{
			Map<String, String> row = e.getValue();
			Map<String, String> out = new LinkedHashMap<>();
			Map<String, String> instanceFields = parseInstanceKey(e.getKey());
			for (String k : INSTANCE_KEYS)
				out.put(k, instanceFields.getOrDefault(k, ""));
			boolean feasible = "true".equalsIgnoreCase(row.get("Feasible"));
			out.put("BestFamily",                getFamily(row.getOrDefault("Solver", "")));
			out.put("BestSolver",                row.getOrDefault("Solver", ""));
			out.put("BestSetting",               getBestSetting(row));
			out.put("BestObjectiveActual",        row.getOrDefault("ObjectiveActual", ""));
			out.put("BestComputationTimeSeconds", feasible ? row.getOrDefault("ComputationTimeSeconds", "") : "3600");
			out.put("BestOptimalityGap",          row.getOrDefault("OptimalityGap", ""));
			out.put("BestDirectTravelersPct",     row.getOrDefault("DirectTravelersActualPct", ""));
			out.put("BestOneTransferPct",         row.getOrDefault("OneTransferActualPct", ""));
			out.put("BestTwoPlusTransfersPct",    row.getOrDefault("TwoPlusTransfersActualPct", ""));
			out.put("BestUncoveredDemandPct",     row.getOrDefault("UncoveredDemandActualPct", ""));
			outRows.add(out);
		}

		writeCsvWithColumns(BEST_OVERALL_FILE, BEST_OVERALL_COLUMNS, outRows);
		System.out.println("Written " + outRows.size() + " rows to " + BEST_OVERALL_FILE);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static String getFamily(String solver)
	{
		switch (solver)
		{
			case "PTNSolver":              return "PTN";
			case "PTNSolver-Karbstein":
			case "PTN-BendersSolver":      return "PTN+DC";
			case "DPTNSolver-MIP":
			case "DPTNSolver-BAP":         return "DPTN";
			case "CGNSolver":              return "CGN";
			default:                       return null;
		}
	}

	private static String getBestSetting(Map<String, String> row)
	{
		String solver = row.getOrDefault("Solver", "");
		switch (solver)
		{
			case "PTNSolver":
			case "PTNSolver-Karbstein":
			case "DPTNSolver-MIP":
				return row.getOrDefault("RouteGeneration", "");
			case "PTN-BendersSolver":
				return "Benders-" + row.getOrDefault("RouteGeneration", "");
			case "DPTNSolver-BAP":
			case "CGNSolver":
				return "SI=" + row.getOrDefault("StrongInequalities", "");
			default:
				return "";
		}
	}

	private static String instanceKey(Map<String, String> row)
	{
		StringBuilder sb = new StringBuilder();
		for (String k : INSTANCE_KEYS)
			sb.append(row.getOrDefault(k, "")).append("|");
		return sb.toString();
	}

	private static Map<String, String> parseInstanceKey(String key)
	{
		String[] parts = key.split("\\|", -1);
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i < INSTANCE_KEYS.length && i < parts.length; i++)
			m.put(INSTANCE_KEYS[i], parts[i]);
		return m;
	}

	private static double parseDouble(String s)
	{
		if (s == null || s.isEmpty()) return Double.MAX_VALUE;
		try { return Double.parseDouble(s); }
		catch (NumberFormatException e) { return Double.MAX_VALUE; }
	}

	private static String[] splitCsvLine(String line)
	{
		List<String> result = new ArrayList<>();
		boolean inQuotes = false;
		StringBuilder current = new StringBuilder();
		for (char c : line.toCharArray())
		{
			if (c == '"')                { inQuotes = !inQuotes; }
			else if (c == ',' && !inQuotes) { result.add(current.toString()); current.setLength(0); }
			else                         { current.append(c); }
		}
		result.add(current.toString());
		return result.toArray(new String[0]);
	}

	private static void writeCsvWithColumns(String path, String[] columns, List<Map<String, String>> rows) throws IOException
	{
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(path)))
		{
			bw.write(String.join(",", columns));
			bw.newLine();
			for (Map<String, String> row : rows)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < columns.length; i++)
				{
					if (i > 0) sb.append(',');
					String val = row.getOrDefault(columns[i], "");
					if (val.contains(",")) sb.append('"').append(val).append('"');
					else sb.append(val);
				}
				bw.write(sb.toString());
				bw.newLine();
			}
		}
	}
}
