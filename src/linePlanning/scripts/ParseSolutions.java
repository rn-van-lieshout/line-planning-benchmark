package linePlanning.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import linePlanning.data.Reader;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.instance.ODPair;
import util.Configuration;

/**
 * Parses all solution files in a folder and writes a CSV summary. Usage: set
 * solutionsFolder and outputFile below, then run.
 */
public class ParseSolutions
{
	private static final int    TOTAL_TASKS        = 2592;
	private static final int    CPU_TIME_SECONDS   = 3600;
	private static final String INSTANCE_FOLDER    = "instances/Switzerland";
	private static final String EXPERIMENT_FOLDER  = "Experiments/Exp6";
	private static final boolean GENERATE_MISSING  = false;
	private static final String[] SOLVER_NAMES     = {
		"PTNSolver", "PTNSolver-Karbstein", "PTN-BendersSolver", "DPTNSolver-MIP", "DPTNSolver-BAP", "CGNSolver"
	};
	private static final String[] BUDGET_NAMES     = { "small", "medium", "large" };
	private static final String[] TRANSFER_NAMES   = { "small", "medium", "large" };
	private static final String[] LINE_NAMES       = { "small", "medium", "large" };
	private static final String[] FIXED_COST_NAMES = { "low",   "medium", "high"  };
	private static final String[] OD_NAMES         = { "0",     "3-false"          };
	private static final String[] ROUTE_NAMES      = { "small", "medium", "large" };
	private static final String[][] SI_NAMES       = { {"false"}, {"true"} };

	// Test experiment (test_instances.sh): 6 instances × 6 solver combos
	private static final int      TOTAL_TASKS_TEST    = 36;
	private static final String[] TEST_INSTANCE_NAMES = {
		"Randstad-IC", "Randstad-IC-SPR", "Nederland-IC", "Nederland-IC-SPR", "SmallSwitzerland", "Switzerland"
	};

	// Settings and Results keys to extract, in CSV column order
	private static final String[] COLUMNS =
	{ "File",
			// Settings
			"Instance", "ODMatrix", "TotalVolume", "TransferPenalty", "RouteGeneration", "LineGeneration", "Budget",
			"LineActivationCost", "Solver", "StrongInequalities", "PairsPerGroup", "TimeLimitSeconds",
			// Results
			"Feasible", "ObjectiveEstimate", "Cost", "ComputationTimeSeconds", "OptimalityGap", "DirectTravelersEstimatePct",
			"OneTransferEstimatePct", "TwoPlusTransfersEstimatePct", "ObjectiveActual", "UncoveredDemandActualPct",
			"DirectTravelersActualPct", "OneTransferActualPct", "TwoPlusTransfersActualPct", "Warnings" };

	public static void main(String[] args) throws IOException
	{
		String experiment       = "factorial"; // "factorial" or "test"
		String solutionsFolder  = EXPERIMENT_FOLDER + "/solutions";
		String logsFolder       = EXPERIMENT_FOLDER + "/logs";

		List<File> files = new ArrayList<>();
		String outputFile;
		int totalTasks;

		if (experiment.equals("test"))
		{
			File dir = new File(solutionsFolder);
			File[] fs = dir.listFiles((d, n) -> n.endsWith(".txt"));
			if (fs != null)
				for (File f : fs)
					files.add(f);
			outputFile = EXPERIMENT_FOLDER + "/results.csv";
			totalTasks = TOTAL_TASKS_TEST;
		}
		else
		{
			File folder = new File(solutionsFolder);
			File[] arr = folder.listFiles((d, n) -> n.endsWith(".txt"));
			if (arr != null)
				for (File f : arr)
					files.add(f);
			outputFile = EXPERIMENT_FOLDER + "/results.csv";
			totalTasks = TOTAL_TASKS;
		}

		if (files.isEmpty())
		{
			System.out.println("No solution files found.");
			return;
		}

		List<Map<String, String>> rows = new ArrayList<>();
		for (File file : files)
		{
			Map<String, String> row = parseFile(file);
			rows.add(row);
		}

		writeCsv(outputFile, rows);
		System.out.println("Parsed " + rows.size() + " files -> " + outputFile);
		for (Map<String, String> row : rows)
		{
			String w = row.get("Warnings");
			if (w != null && !w.isEmpty())
				System.out.println("WARNING in " + row.get("File") + ": " + w);
		}

		// 1. Empty / unparseable files
		List<String> emptyFiles = new ArrayList<>();
		for (Map<String, String> row : rows)
			if (row.getOrDefault("Instance", "").isEmpty())
				emptyFiles.add(row.get("File"));
		if (emptyFiles.isEmpty())
			System.out.println("No empty/unparseable files.");
		else
		{
			System.out.println("Empty/unparseable files (" + emptyFiles.size() + "):");
			for (String f : emptyFiles)
				System.out.println("  " + f + " (task " + extractTaskId(f) + ")");
		}

		// 2. Parameter mismatches (file content vs. task ID encoded in filename)
		List<String> mismatches = new ArrayList<>();
		for (Map<String, String> row : rows)
		{
			Integer taskId = extractTaskId(row.get("File"));
			if (taskId == null) continue;
			List<String> problems = new ArrayList<>();
			if (experiment.equals("test"))
			{
				int[] d = decodeTestTaskId(taskId);
				int instanceIdx = d[0], solverCombo = d[1];
				checkField(problems, row, "Instance",          "instances/" + TEST_INSTANCE_NAMES[instanceIdx]);
				checkField(problems, row, "Solver",            SOLVER_NAMES[solverCombo]);
				checkField(problems, row, "ODMatrix",          "1-false");
				checkField(problems, row, "Budget",            "medium");
				checkField(problems, row, "TransferPenalty",   "large");
				checkField(problems, row, "LineActivationCost","medium");
				checkField(problems, row, "LineGeneration",    "small");
				if (solverCombo < 4)
					checkField(problems, row, "RouteGeneration", "medium");
				else
					checkField(problems, row, "StrongInequalities", "true");
			}
			else
			{
				int[] d = decodeTaskId(taskId);
				checkField(problems, row, "Budget",            BUDGET_NAMES[d[2]]);
				checkField(problems, row, "TransferPenalty",   TRANSFER_NAMES[d[3]]);
				checkField(problems, row, "LineGeneration",    LINE_NAMES[d[4]]);
				checkField(problems, row, "LineActivationCost",FIXED_COST_NAMES[d[5]]);
				checkField(problems, row, "ODMatrix",          OD_NAMES[d[6]]);
				if (d[0] < 4)
					checkField(problems, row, "RouteGeneration", ROUTE_NAMES[d[1]]);
				else
					checkField(problems, row, "StrongInequalities", SI_NAMES[d[1]][0]);
			}
			if (!problems.isEmpty())
				mismatches.add("  " + row.get("File") + " (task " + taskId + "): " + String.join(", ", problems));
		}
		if (mismatches.isEmpty())
			System.out.println("No parameter mismatches.");
		else
		{
			System.out.println("Parameter mismatches (" + mismatches.size() + " files):");
			mismatches.forEach(System.out::println);
		}

		// 3. Missing task IDs
		Set<Integer> foundIds = new HashSet<>();
		for (Map<String, String> row : rows)
		{
			Integer id = extractTaskId(row.get("File"));
			if (id != null) foundIds.add(id);
		}
		List<Integer> missingIds = new ArrayList<>();
		for (int i = 0; i < totalTasks; i++)
			if (!foundIds.contains(i)) missingIds.add(i);
		if (missingIds.isEmpty())
			System.out.println("All " + totalTasks + " tasks present.");
		else
		{
			System.out.println("Missing " + missingIds.size() + "/" + totalTasks + " task IDs: " + missingIds);
			for (int id : missingIds)
			{
				String diagnosis = diagnoseFromLog(id, logsFolder);
				if (!diagnosis.equals("timeout"))
					System.out.println("  Task " + id + ": " + diagnosis);
			}
			if (!experiment.equals("test"))
			{
				if (GENERATE_MISSING)
					generateNoSolutionFiles(missingIds, solutionsFolder);
			}
		}
	}

	private static String diagnoseFromLog(int taskId, String logsFolder)
	{
		File dir = new File(logsFolder);
		if (!dir.exists()) return "logs folder not found";

		File[] matches = dir.listFiles((d, n) -> n.endsWith("_" + taskId + ".out"));
		if (matches == null || matches.length == 0) return "no log file found";

		try (BufferedReader br = new BufferedReader(new FileReader(matches[0])))
		{
			String line;
			List<String> errorLines = new ArrayList<>();
			int lineCount = 0;
			while ((line = br.readLine()) != null)
			{
				lineCount++;
				String upper = line.toUpperCase();
				if (upper.contains("TIME LIMIT") || upper.contains("TIMEOUT") || upper.contains("CANCELLED"))
					return "timeout";
				if (line.contains("OutOfMemoryError") || upper.contains("OUT OF MEMORY"))
					return "OOM (Java): " + line.trim();
				// Linux OOM killer or SLURM SIGKILL — no Java stack trace
				if (line.equals("Killed") || line.startsWith("Killed ")
						|| (line.contains("Killed") && upper.contains("JAVA"))
						|| upper.contains("OOM_KILL"))
					return "OOM (killed by OS)";
				if (line.startsWith("slurmstepd") || line.startsWith("srun: error"))
					errorLines.add(0, "SLURM: " + line.trim());
				else if (line.contains("Exception") || line.contains("ERROR") || line.contains("FAILED"))
					errorLines.add(line.trim());
			}
			if (!errorLines.isEmpty()) return "error: " + errorLines.get(0);
			if (lineCount == 0) return "empty log";
			return "no error found in log (" + lineCount + " lines)";
		}
		catch (IOException e) { return "could not read log: " + e.getMessage(); }
	}

	private static void generateNoSolutionFiles(List<Integer> missingIds, String solutionsFolder) throws IOException
	{
		Configuration.initialiseConfiguration("settings.properties");

		// Group by (od, tp): objectiveActual, totalVolume, and transfer penalty only depend on those
		Map<String, List<Integer>> groups = new LinkedHashMap<>();
		for (int taskId : missingIds)
		{
			int[] d = decodeTaskId(taskId);
			groups.computeIfAbsent(d[6] + "_" + d[3], k -> new ArrayList<>()).add(taskId);
		}

		int generated = 0;
		for (Map.Entry<String, List<Integer>> entry : groups.entrySet())
		{
			int[] d = decodeTaskId(entry.getValue().get(0));
			int od = d[6], tp = d[3];
			String odMatrixFile        = INSTANCE_FOLDER + "/ODMatrix/" + OD_NAMES[od] + ".txt";
			String transferPenaltyFile = INSTANCE_FOLDER + "/transferPenalty/" + TRANSFER_NAMES[tp] + ".txt";
			String budgetFile          = INSTANCE_FOLDER + "/budget/medium.txt";
			try
			{
				LinePlanningInstance instance = new LinePlanningInstance(
					INSTANCE_FOLDER, odMatrixFile, budgetFile, transferPenaltyFile);
				double objectiveActual = 0;
				for (ODPair pair : instance.getODPairs())
					objectiveActual += pair.getVolume() * instance.getMissedDemandPenalty(pair);
				double totalVolume        = instance.getTotalVolume();
				int    transferPenaltyVal = LinePlanningParameters.TRANSFER_PENALTY;

				for (int taskId : entry.getValue())
				{
					try { generateNoSolutionFile(taskId, solutionsFolder, objectiveActual, totalVolume, transferPenaltyVal); generated++; }
					catch (Exception e) { System.out.println("  Failed task " + taskId + ": " + e.getMessage()); }
				}
			}
			catch (Exception e)
			{
				System.out.println("  Failed to load instance for OD=" + OD_NAMES[od]
					+ " transfer=" + TRANSFER_NAMES[tp] + ": " + e.getMessage());
			}
		}
		System.out.println("Generated " + generated + " no-solution files in " + solutionsFolder);
	}

	private static void generateNoSolutionFile(int taskId, String solutionsFolder,
			double objectiveActual, double totalVolume, int transferPenaltyVal) throws IOException
	{
		int[] d = decodeTaskId(taskId);
		int so = d[0], se = d[1], bu = d[2], tp = d[3], li = d[4], fc = d[5], od = d[6];

		String odVariant        = OD_NAMES[od];
		String budgetVariant    = BUDGET_NAMES[bu];
		String transferVariant  = TRANSFER_NAMES[tp];
		String lineVariant      = LINE_NAMES[li];
		String fixedCostVariant = FIXED_COST_NAMES[fc];
		String budgetFolder     = fc == 0 ? "budget-fixedCost-low" : fc == 2 ? "budget-fixedCost-high" : "budget";

		int budgetValue = Reader.readInt(INSTANCE_FOLDER + "/" + budgetFolder + "/" + budgetVariant + ".txt");

		String solverName   = SOLVER_NAMES[so];
		String routeVariant = so < 4 ? ROUTE_NAMES[se] : null;
		boolean si          = so >= 4 && se > 0;

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		String filePath  = solutionsFolder + "/" + timestamp + "_" + taskId + ".txt";

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath)))
		{
			writer.write("# Settings"); writer.newLine();
			writer.write("Instance: " + INSTANCE_FOLDER); writer.newLine();
			writer.write("ODMatrix: " + odVariant); writer.newLine();
			writer.write(String.format(Locale.ROOT, "TotalVolume: %.1f%n", totalVolume));
			writer.write("TransferPenalty: " + transferVariant + " (" + transferPenaltyVal + ")"); writer.newLine();
			writer.write("LineGeneration: " + lineVariant); writer.newLine();
			writer.write("Budget: " + budgetVariant + " (" + budgetValue + ")"); writer.newLine();
			writer.write("LineActivationCost: " + fixedCostVariant); writer.newLine();
			writer.write("Solver: " + solverName); writer.newLine();
			if (routeVariant != null) { writer.write("RouteGeneration: " + routeVariant); writer.newLine(); }
			if (so >= 4) { writer.write("StrongInequalities: " + si); writer.newLine(); }
			writer.write("TimeLimitSeconds: " + CPU_TIME_SECONDS); writer.newLine();
			writer.newLine();
			writer.write("# Results"); writer.newLine();
			writer.write("Feasible: false"); writer.newLine();
			writer.write(String.format(Locale.ROOT, "ObjectiveActual: %.4f%n", objectiveActual));
			writer.write("ComputationTimeSeconds: 0.0"); writer.newLine();
		}
		System.out.println("  Generated task " + taskId + " -> " + new File(filePath).getName());
	}

	private static Integer extractTaskId(String filename)
	{
		if (filename == null) return null;
		int lastUnderscore = filename.lastIndexOf('_');
		int dot = filename.lastIndexOf('.');
		if (lastUnderscore < 0 || dot <= lastUnderscore) return null;
		try { return Integer.parseInt(filename.substring(lastUnderscore + 1, dot)); }
		catch (NumberFormatException e) { return null; }
	}

	// Returns [instanceIdx, solverCombo]
	private static int[] decodeTestTaskId(int id)
	{
		return new int[]{ id / 6, id % 6 };
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

	private static void checkField(List<String> problems, Map<String, String> row, String key, String expected)
	{
		String actual = row.getOrDefault(key, "");
		if (!actual.isEmpty() && !actual.equals(expected))
			problems.add(key + " expected=" + expected + " actual=" + actual);
	}

	private static Map<String, String> parseFile(File file) throws IOException
	{
		Map<String, String> row = new LinkedHashMap<>();
		row.put("File", file.getName());

		String section = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("# "))
				{
					section = line.substring(2).trim();
					continue;
				}
				if (line.isBlank() || section == null)
					continue;
				if (!section.equals("Settings") && !section.equals("Results"))
					continue;

				int colon = line.indexOf(": ");
				if (colon == -1)
					continue;

				String key = line.substring(0, colon).trim();
				String value = line.substring(colon + 2).trim();

				// Strip "(value)" annotations — keep only the variant name
				if (key.equals("Budget") || key.equals("TransferPenalty"))
					value = value.replaceAll("\\s*\\(.*\\)", "").trim();

				row.put(key, value);
			}
		}
		return row;
	}

	private static void writeCsv(String outputFile, List<Map<String, String>> rows) throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{
			// Header
			writer.write(String.join(",", COLUMNS));
			writer.newLine();

			// Rows
			for (Map<String, String> row : rows)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < COLUMNS.length; i++)
				{
					if (i > 0)
						sb.append(",");
					String value = row.getOrDefault(COLUMNS[i], "");
					// Quote values that contain commas
					if (value.contains(","))
						sb.append("\"").append(value).append("\"");
					else
						sb.append(value);
				}
				writer.write(sb.toString());
				writer.newLine();
			}
		}
	}
}
