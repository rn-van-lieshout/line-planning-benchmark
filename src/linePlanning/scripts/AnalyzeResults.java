package linePlanning.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads results.csv produced by ParseSolutions and determines, for each parameter
 * setting, which solver+setting achieved the best (lowest) ObjectiveActual.
 * Writes best_solvers.csv and prints a win-count summary to stdout.
 */
public class AnalyzeResults
{
	private static final String[] PARAM_KEYS = {
		"Instance", "ODMatrix", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost"
	};

	private static final String[] BUDGET_NAMES      = { "small", "medium", "large" };
	private static final String[] TRANSFER_NAMES    = { "small", "medium", "large" };
	private static final String[] LINE_NAMES        = { "small", "medium", "large" };
	private static final String[] FIXED_COST_NAMES  = { "low",   "medium", "high"  };
	private static final String[] OD_NAMES          = { "0",     "3-false"          };
	private static final String[] OUT_COLUMNS = {
		"Instance", "ODMatrix", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost",
		"NrFeasible", "BestSolver", "BestSetting", "BestObjectiveActual"
	};

	public static void main(String[] args) throws IOException
	{
		String inputFile      = "Experiments/Exp5/results.csv";
		String outputFile     = "Experiments/Exp5/best_solvers.csv";
		// Set to any value from PARAM_KEYS to break down wins by that parameter, or null to skip
		String breakdownParam = "LineActivationCost";

		List<Map<String, String>> rows = readCsv(inputFile);

		// Group by parameter setting; skip rows with no recognisable parameters (old-format files)
		Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
		int skipped = 0;
		for (Map<String, String> row : rows)
		{
			if (Arrays.stream(PARAM_KEYS).allMatch(k -> row.getOrDefault(k, "").isEmpty()))
			{ skipped++; continue; }
			groups.computeIfAbsent(paramKey(row), k -> new ArrayList<>()).add(row);
		}
		if (skipped > 0)
			System.out.println("Skipped " + skipped + " rows with no parameter fields (old-format files).");

		List<Map<String, String>> outputRows = new ArrayList<>();
		Map<String, Integer> winCounts = new LinkedHashMap<>();

		for (List<Map<String, String>> group : groups.values())
		{
			Map<String, String> best = null;
			double bestObj = Double.MAX_VALUE;
			int nrFeasible = 0;

			for (Map<String, String> row : group)
			{
				if (!"true".equalsIgnoreCase(row.get("Feasible")))
					continue;
				nrFeasible++;
				String objStr = row.get("ObjectiveActual");
				if (objStr == null || objStr.isEmpty())
					continue;
				try
				{
					double obj = Double.parseDouble(objStr);
					if (obj < bestObj) { bestObj = obj; best = row; }
				}
				catch (NumberFormatException ignored) {}
			}

			Map<String, String> out = new LinkedHashMap<>();
			Map<String, String> first = group.get(0);
			for (String k : PARAM_KEYS)
				out.put(k, first.getOrDefault(k, ""));
			out.put("NrFeasible", String.valueOf(nrFeasible));

			if (best != null)
			{
				String setting = formatSetting(best);
				out.put("BestSolver",          best.getOrDefault("Solver", ""));
				out.put("BestSetting",          setting);
				out.put("BestObjectiveActual",  best.getOrDefault("ObjectiveActual", ""));
				winCounts.merge(best.getOrDefault("Solver", "") + " [" + setting + "]", 1, Integer::sum);
			}
			else
			{
				out.put("BestSolver", ""); out.put("BestSetting", ""); out.put("BestObjectiveActual", "");
			}
			outputRows.add(out);
		}

		writeCsv(outputFile, outputRows);
		System.out.println("Analyzed " + groups.size() + " parameter settings -> " + outputFile);

		// Flag groups with unexpected size (expected: 4 solvers x 3 route + 2 solvers x 2 SI = 16)
		for (String gKey : groups.keySet())
		{
			int gSize = groups.get(gKey).size();
			if (gSize != 16)
			{
				List<String> fileNames = new ArrayList<>();
				groups.get(gKey).forEach(r -> fileNames.add(r.get("File")));
				System.out.println("  Unexpected group size " + gSize
						+ " for: " + gKey + "  (files: " + fileNames + ")");
			}
		}

		System.out.println("\nWins per solver+setting (descending):");
		winCounts.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.forEach(e -> System.out.printf("  %-55s %d%n", e.getKey(), e.getValue()));

		// Average ObjectiveActual per solver+setting, two passes: feasible-only, then all rows
		Map<String, List<Double>> objFeasible = new LinkedHashMap<>();
		Map<String, List<Double>> objAll      = new LinkedHashMap<>();
		for (Map<String, String> row : rows)
		{
			if (Arrays.stream(PARAM_KEYS).allMatch(k -> row.getOrDefault(k, "").isEmpty())) continue;
			String objStr = row.get("ObjectiveActual");
			if (objStr == null || objStr.isEmpty()) continue;
			try {
				double obj = Double.parseDouble(objStr);
				String key = row.getOrDefault("Solver", "?") + " [" + formatSetting(row) + "]";
				if ("true".equalsIgnoreCase(row.get("Feasible")))
					objFeasible.computeIfAbsent(key, k -> new ArrayList<>()).add(obj);
				objAll.computeIfAbsent(key, k -> new ArrayList<>()).add(obj);
			}
			catch (NumberFormatException ignored) {}
		}
		printAvgObjective("\nAverage ObjectiveActual per solver+setting — feasible only (ascending, best first):", objFeasible, null);
		printAvgObjective("\nAverage ObjectiveActual per solver+setting — including infeasible (ascending, best first):", objAll, objFeasible);

		if (breakdownParam != null)
		{
			Map<String, Map<String, Integer>> winsByValue = new LinkedHashMap<>();
			for (Map<String, String> outRow : outputRows)
			{
				String paramValue = outRow.getOrDefault(breakdownParam, "");
				String solver  = outRow.getOrDefault("BestSolver", "");
				String setting = outRow.getOrDefault("BestSetting", "");
				if (solver.isEmpty()) continue;
				winsByValue.computeIfAbsent(paramValue, k -> new LinkedHashMap<>())
					.merge(solver + " [" + setting + "]", 1, Integer::sum);
			}
			System.out.println("\nWins per solver+setting by " + breakdownParam + ":");
			for (String paramValue2 : winsByValue.keySet())
			{
				System.out.println("  " + breakdownParam + " = " + paramValue2 + ":");
				Map<String, Integer> winsForValue = winsByValue.get(paramValue2);
				List<String> wKeys = new ArrayList<>(winsForValue.keySet());
				wKeys.sort((a, b) -> winsForValue.get(b) - winsForValue.get(a));
				for (String wKey : wKeys)
					System.out.printf("    %-55s %d%n", wKey, winsForValue.get(wKey));
			}
		}

		reportMissingTasks(rows);
	}

	private static void printAvgObjective(String header, Map<String, List<Double>> data, Map<String, List<Double>> feasibleData)
	{
		System.out.println(header);
		List<String> keys = new ArrayList<>(data.keySet());
		keys.sort((a, b) -> {
			double avgA = data.get(a).stream().mapToDouble(Double::doubleValue).average().orElse(0);
			double avgB = data.get(b).stream().mapToDouble(Double::doubleValue).average().orElse(0);
			return Double.compare(avgA, avgB);
		});
		for (String key : keys)
		{
			List<Double> vals = data.get(key);
			double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
			String label = feasibleData == null
				? " feasible"
				: ", " + feasibleData.getOrDefault(key, List.of()).size() + " feasible";
			System.out.printf("  %-55s %.4f  (%d%s)%n", key, avg, vals.size(), label);
		}
	}

	private static void reportMissingTasks(List<Map<String, String>> rows)
	{
		// Map solver+setting string -> set of present task IDs (extracted from filename)
		Map<String, Set<Integer>> presentIds = new LinkedHashMap<>();
		for (Map<String, String> row : rows)
		{
			if (Arrays.stream(PARAM_KEYS).allMatch(k -> row.getOrDefault(k, "").isEmpty())) continue;
			Integer taskId = extractTaskId(row.getOrDefault("File", ""));
			if (taskId == null) continue;
			String ss = row.getOrDefault("Solver", "?") + " [" + formatSetting(row) + "]";
			presentIds.computeIfAbsent(ss, k -> new HashSet<>()).add(taskId);
		}

		System.out.println("\nMissing parameter settings per solver+setting:");
		boolean anyMissing = false;
		for (Map.Entry<String, Set<Integer>> entry : presentIds.entrySet())
		{
			String ss = entry.getKey();
			Set<Integer> present = entry.getValue();
			if (present.isEmpty()) continue;

			// Infer (solverIdx, settingIdx) from any present task ID
			int[] sample = decodeTaskId(present.iterator().next());
			int solverIdx  = sample[0];
			int settingIdx = sample[1];

			List<Integer> missing = new ArrayList<>();
			for (int b = 0; b < 3; b++)
				for (int tp = 0; tp < 3; tp++)
					for (int l = 0; l < 3; l++)
						for (int fc = 0; fc < 3; fc++)
							for (int od = 0; od < 2; od++)
							{
								int id = encodeTaskId(solverIdx, settingIdx, b, tp, l, fc, od);
								if (!present.contains(id)) missing.add(id);
							}

			if (!missing.isEmpty())
			{
				anyMissing = true;
				System.out.println("  " + ss + ":");
				for (int id : missing)
				{
					int[] d = decodeTaskId(id);
					System.out.printf("    task %4d: budget=%-6s transfer=%-6s line=%-6s fixedCost=%-6s OD=%s%n",
							id, BUDGET_NAMES[d[2]], TRANSFER_NAMES[d[3]], LINE_NAMES[d[4]],
							FIXED_COST_NAMES[d[5]], OD_NAMES[d[6]]);
				}
			}
		}
		if (!anyMissing) System.out.println("  None.");
	}

	private static Integer extractTaskId(String filename)
	{
		int lastUnderscore = filename.lastIndexOf('_');
		int dot = filename.lastIndexOf('.');
		if (lastUnderscore < 0 || dot <= lastUnderscore) return null;
		try { return Integer.parseInt(filename.substring(lastUnderscore + 1, dot)); }
		catch (NumberFormatException e) { return null; }
	}

	// Returns [solverIdx, settingIdx, budgetIdx, transferIdx, lineIdx, fixedCostIdx, odIdx]
	private static int[] decodeTaskId(int id)
	{
		boolean cg = id >= 1944;
		if (cg) id -= 1944;
		int od = id % 2; id /= 2;
		int fc = id % 3; id /= 3;
		int li = id % 3; id /= 3;
		int tp = id % 3; id /= 3;
		int bu = id % 3; id /= 3;
		int se, so;
		if (cg) { se = id % 2; id /= 2; so = 4 + id; }
		else    { se = id % 3; id /= 3; so = id; }
		return new int[]{ so, se, bu, tp, li, fc, od };
	}

	private static int encodeTaskId(int so, int se, int bu, int tp, int li, int fc, int od)
	{
		int seCount = so < 4 ? 3 : 2;
		int local = ((((so % 4) * seCount + se) * 3 + bu) * 3 + tp) * 3;
		local = (local + li) * 3 + fc;
		local = local * 2 + od;
		return so < 4 ? local : 1944 + local;
	}

	private static String paramKey(Map<String, String> row)
	{
		StringBuilder sb = new StringBuilder();
		for (String k : PARAM_KEYS)
			sb.append(row.getOrDefault(k, "")).append("|");
		return sb.toString();
	}

	private static String formatSetting(Map<String, String> row)
	{
		String rg = row.getOrDefault("RouteGeneration", "");
		if (!rg.isEmpty())
			return "route=" + rg;
		String si = row.getOrDefault("StrongInequalities", "");
		return "SI=" + si;
	}

	// ── CSV I/O ───────────────────────────────────────────────────────────────

	private static List<Map<String, String>> readCsv(String path) throws IOException
	{
		List<Map<String, String>> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(path)))
		{
			String headerLine = reader.readLine();
			if (headerLine == null) return rows;
			String[] headers = splitLine(headerLine);

			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] values = splitLine(line);
				Map<String, String> row = new LinkedHashMap<>();
				for (int i = 0; i < headers.length; i++)
					row.put(headers[i].trim(), i < values.length ? values[i].trim() : "");
				rows.add(row);
			}
		}
		return rows;
	}

	private static String[] splitLine(String line)
	{
		List<String> fields = new ArrayList<>();
		int i = 0;
		while (i <= line.length())
		{
			if (i < line.length() && line.charAt(i) == '"')
			{
				int end = line.indexOf('"', i + 1);
				fields.add(end >= 0 ? line.substring(i + 1, end) : line.substring(i + 1));
				i = end >= 0 ? end + 2 : line.length() + 1;
			}
			else
			{
				int comma = line.indexOf(',', i);
				if (comma >= 0) { fields.add(line.substring(i, comma)); i = comma + 1; }
				else            { fields.add(line.substring(i));        i = line.length() + 1; }
			}
		}
		return fields.toArray(new String[0]);
	}

	private static void writeCsv(String path, List<Map<String, String>> rows) throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path)))
		{
			writer.write(String.join(",", OUT_COLUMNS));
			writer.newLine();
			for (Map<String, String> row : rows)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < OUT_COLUMNS.length; i++)
				{
					if (i > 0) sb.append(",");
					String v = row.getOrDefault(OUT_COLUMNS[i], "");
					if (v.contains(",")) sb.append('"').append(v).append('"');
					else sb.append(v);
				}
				writer.write(sb.toString());
				writer.newLine();
			}
		}
	}
}
