package linePlanning.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ilog.concert.IloException;
import linePlanning.data.Reader;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.solvers.LinePlanningSolution;
import linePlanning.solvers.LinePlanningSolutionReader;
import util.Configuration;

/**
 * For each unique (Instance, Budget, TransferPenalty, LineGeneration, LineActivationCost)
 * combination, takes the best feasible solution found with ODMatrix=3-false and re-evaluates
 * it against ODMatrix=0. Compares the resulting ObjectiveActual with the best ObjectiveActual
 * found for ODMatrix=0.
 *
 * Reads:  Experiments/combinedResults.csv
 * Scans:  Experiments/Exp* /solutions/ for solution files
 * Writes: Experiments/crossEvaluation.csv
 */
public class CrossEvaluate
{
	private static final String COMBINED_FILE    = "Experiments/combinedResults.csv";
	private static final String OUTPUT_FILE      = "Experiments/crossEvaluation.csv";
	private static final String EXPERIMENTS_DIR  = "Experiments";

	private static final String[] NON_OD_KEYS = {
		"Instance", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost"
	};

	private static final String[] OUTPUT_COLUMNS = {
		"Instance", "Budget", "TransferPenalty", "LineGeneration", "LineActivationCost",
		"OD0Feasible", "OD3fFeasible",
		"OD0BestObjective", "OD3fBestObjective",
		"CrossEvalObjective",
		"Difference",       // CrossEvalObjective - OD0BestObjective
		"RelDifferencePct", // (CrossEvalObjective - OD0BestObjective) / OD0BestObjective * 100
		"OD3fBestSolver", "OD3fBestFile"
	};

	public static void main(String[] args) throws IOException, IloException
	{
		Configuration.initialiseConfiguration("settings.properties");

		List<Map<String, String>> rows = readCsv(COMBINED_FILE);
		System.out.println("Read " + rows.size() + " rows from " + COMBINED_FILE);

		Map<String, String> filePathByName = buildFileLookup();
		System.out.println("Indexed " + filePathByName.size() + " solution files across Exp* directories");

		// Group: nonOdKey -> odVariant -> rows
		Map<String, Map<String, List<Map<String, String>>>> grouped = new LinkedHashMap<>();
		for (Map<String, String> row : rows)
		{
			String od = row.getOrDefault("ODMatrix", "");
			if (!od.equals("0") && !od.equals("3-false")) continue;
			grouped.computeIfAbsent(nonOdKey(row), k -> new LinkedHashMap<>())
			       .computeIfAbsent(od, k -> new ArrayList<>())
			       .add(row);
		}
		System.out.println("Found " + grouped.size() + " unique non-OD settings");

		List<Map<String, String>> outRows = new ArrayList<>();
		int evaluated = 0, skipped = 0;

		for (Map.Entry<String, Map<String, List<Map<String, String>>>> entry : grouped.entrySet())
		{
			Map<String, String> fields = parseNonOdKey(entry.getKey());
			Map<String, List<Map<String, String>>> byOD = entry.getValue();

			Map<String, String> best0  = findBestFeasible(byOD.getOrDefault("0",       Collections.emptyList()));
			Map<String, String> best3f = findBestFeasible(byOD.getOrDefault("3-false", Collections.emptyList()));

			Map<String, String> out = new LinkedHashMap<>();
			for (String k : NON_OD_KEYS) out.put(k, fields.getOrDefault(k, ""));
			out.put("OD0Feasible",       best0  != null ? "true" : "false");
			out.put("OD3fFeasible",      best3f != null ? "true" : "false");
			out.put("OD0BestObjective",  best0  != null ? best0.getOrDefault("ObjectiveActual", "")  : "");
			out.put("OD3fBestObjective", best3f != null ? best3f.getOrDefault("ObjectiveActual", "") : "");
			out.put("OD3fBestSolver",    best3f != null ? best3f.getOrDefault("Solver", "")          : "");
			out.put("OD3fBestFile",      best3f != null ? best3f.getOrDefault("File", "")            : "");

			if (best3f == null || best0 == null)
			{
				out.put("CrossEvalObjective", "");
				out.put("Difference", "");
				out.put("RelDifferencePct", "");
				skipped++;
				outRows.add(out);
				continue;
			}

			String fileName = best3f.getOrDefault("File", "");
			String filePath = filePathByName.get(fileName);
			if (filePath == null)
			{
				System.out.println("Solution file not found: " + fileName);
				out.put("CrossEvalObjective", "");
				out.put("Difference", "");
				out.put("RelDifferencePct", "");
				skipped++;
				outRows.add(out);
				continue;
			}

			String instanceFolder       = fields.get("Instance");
			String fixedCostVariant     = fields.get("LineActivationCost");
			String budgetFolder         = fixedCostVariant.equals("low")  ? "budget-fixedCost-low"
			                            : fixedCostVariant.equals("high") ? "budget-fixedCost-high"
			                            :                                    "budget";
			String budgetFile           = instanceFolder + "/" + budgetFolder + "/" + fields.get("Budget") + ".txt";
			String transferPenaltyFile  = instanceFolder + "/transferPenalty/" + fields.get("TransferPenalty") + ".txt";
			String fixedCostFile        = instanceFolder + "/fixedCost/" + fixedCostVariant + ".txt";
			String odMatrixFile         = instanceFolder + "/ODMatrix/0.txt";

			try
			{
				LinePlanningInstance instance = new LinePlanningInstance(
						instanceFolder, odMatrixFile, budgetFile, transferPenaltyFile);
				LinePlanningParameters.LINE_ACTIVATION_COST = Reader.readInt(fixedCostFile);

				LinePlanningSolution solution = LinePlanningSolutionReader.read(filePath, instance);
				solution.evaluateTrueObjective();

				double crossEvalObj = solution.getObjectiveActual();
				double od0BestObj   = Double.parseDouble(best0.getOrDefault("ObjectiveActual", "NaN"));
				double diff         = crossEvalObj - od0BestObj;
				double relDiff      = od0BestObj != 0 ? diff / od0BestObj * 100.0 : Double.NaN;

				out.put("CrossEvalObjective", String.format(Locale.ROOT, "%.4f", crossEvalObj));
				out.put("Difference",         String.format(Locale.ROOT, "%.4f", diff));
				out.put("RelDifferencePct",   String.format(Locale.ROOT, "%.4f", relDiff));

				System.out.printf(Locale.ROOT,
						"[%d] %s | OD0=%.1f  3f=%.1f  crossEval=%.1f  diff=%.1f (%.2f%%)%n",
						++evaluated, entry.getKey(),
						od0BestObj,
						Double.parseDouble(best3f.getOrDefault("ObjectiveActual", "0")),
						crossEvalObj, diff, relDiff);
			}
			catch (Exception e)
			{
				System.out.println("Error evaluating " + fileName + ": " + e.getMessage());
				out.put("CrossEvalObjective", "ERROR");
				out.put("Difference", "");
				out.put("RelDifferencePct", "");
				skipped++;
			}

			outRows.add(out);
		}

		writeCsv(OUTPUT_FILE, outRows);
		System.out.printf("Done: %d evaluated, %d skipped -> %s%n", evaluated, skipped, OUTPUT_FILE);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static Map<String, String> findBestFeasible(List<Map<String, String>> rows)
	{
		Map<String, String> best = null;
		double bestObj = Double.MAX_VALUE;
		for (Map<String, String> row : rows)
		{
			if (!"true".equalsIgnoreCase(row.get("Feasible"))) continue;
			double obj = parseDouble(row.get("ObjectiveActual"));
			if (obj < bestObj) { bestObj = obj; best = row; }
		}
		return best;
	}

	private static Map<String, String> buildFileLookup()
	{
		Map<String, String> lookup = new HashMap<>();
		File expDir = new File(EXPERIMENTS_DIR);
		File[] subDirs = expDir.listFiles(f -> f.isDirectory() && f.getName().matches("Exp\\d+"));
		if (subDirs == null) return lookup;
		for (File dir : subDirs)
		{
			File solutionsDir = new File(dir, "solutions");
			if (!solutionsDir.exists()) continue;
			File[] files = solutionsDir.listFiles((d, n) -> n.endsWith(".txt"));
			if (files == null) continue;
			for (File f : files) lookup.put(f.getName(), f.getPath());
		}
		return lookup;
	}

	private static String nonOdKey(Map<String, String> row)
	{
		StringBuilder sb = new StringBuilder();
		for (String k : NON_OD_KEYS) sb.append(row.getOrDefault(k, "")).append("|");
		return sb.toString();
	}

	private static Map<String, String> parseNonOdKey(String key)
	{
		String[] parts = key.split("\\|", -1);
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i < NON_OD_KEYS.length && i < parts.length; i++)
			m.put(NON_OD_KEYS[i], parts[i]);
		return m;
	}

	private static double parseDouble(String s)
	{
		if (s == null || s.isEmpty()) return Double.MAX_VALUE;
		try { return Double.parseDouble(s); }
		catch (NumberFormatException e) { return Double.MAX_VALUE; }
	}

	// ── CSV I/O ───────────────────────────────────────────────────────────────

	private static List<Map<String, String>> readCsv(String path) throws IOException
	{
		List<Map<String, String>> rows = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(path)))
		{
			String headerLine = br.readLine();
			if (headerLine == null) return rows;
			String[] headers = splitCsvLine(headerLine);
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.isBlank()) continue;
				String[] vals = splitCsvLine(line);
				Map<String, String> row = new LinkedHashMap<>();
				for (int i = 0; i < headers.length; i++)
					row.put(headers[i].trim(), i < vals.length ? vals[i].trim() : "");
				rows.add(row);
			}
		}
		return rows;
	}

	private static String[] splitCsvLine(String line)
	{
		List<String> result = new ArrayList<>();
		boolean inQuotes = false;
		StringBuilder current = new StringBuilder();
		for (char c : line.toCharArray())
		{
			if (c == '"')                   { inQuotes = !inQuotes; }
			else if (c == ',' && !inQuotes) { result.add(current.toString()); current.setLength(0); }
			else                            { current.append(c); }
		}
		result.add(current.toString());
		return result.toArray(new String[0]);
	}

	private static void writeCsv(String path, List<Map<String, String>> rows) throws IOException
	{
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(path)))
		{
			bw.write(String.join(",", OUTPUT_COLUMNS));
			bw.newLine();
			for (Map<String, String> row : rows)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < OUTPUT_COLUMNS.length; i++)
				{
					if (i > 0) sb.append(',');
					String val = row.getOrDefault(OUTPUT_COLUMNS[i], "");
					if (val.contains(",")) sb.append('"').append(val).append('"');
					else sb.append(val);
				}
				bw.write(sb.toString());
				bw.newLine();
			}
		}
	}
}
