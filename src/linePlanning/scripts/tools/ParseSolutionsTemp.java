package linePlanning.scripts.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ilog.concert.IloException;
import linePlanning.data.Reader;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.solvers.LinePlanningSolution;
import util.Configuration;

/**
 * Like ParseSolutions, but also derives the fixed cost setting (low/medium/high)
 * by matching the numeric budget value embedded in each solution file against the
 * budget folders in the instance directory.
 *
 * Before parsing, it generates synthetic infeasible solution files for any task
 * IDs (0–2267) that are missing from the solutions folder.
 */
public class ParseSolutionsTemp
{
	private static final String[] COLUMNS =
	{ "File",
			// Settings
			"Instance", "ODMatrix", "TotalVolume", "TransferPenalty", "RouteGeneration", "LineGeneration",
			"Budget", "FixedCost",
			"Solver", "TimeLimitSeconds",
			// Results
			"Feasible", "ObjectiveEstimate", "Cost", "ComputationTimeSeconds", "OptimalityGap",
			"DirectTravelersEstimatePct", "OneTransferEstimatePct", "TwoPlusTransfersEstimatePct",
			"ObjectiveActual", "UncoveredDemandActualPct", "DirectTravelersActualPct",
			"OneTransferActualPct", "TwoPlusTransfersActualPct", "Warnings" };

	private static final Pattern BUDGET_NUMBER_PATTERN = Pattern.compile("\\((\\d+(?:\\.\\d+)?)\\)");
	private static final Pattern TASK_ID_PATTERN = Pattern.compile("_(\\d+)\\.txt$");

	// Maps budget folder name -> fixed cost variant label
	private static final String[][] FIXED_COST_FOLDERS =
	{
			{ "budget-fixedCost-low", "low" },
			{ "budget", "medium" },
			{ "budget-fixedCost-high", "high" }
	};
	private static final String[] BUDGET_SIZES = { "small", "medium", "large" };

	// Experiment parameters matching factorial.sh
	private static final int TOTAL_TASKS = 2268; // 3*3*3*14*3*2
	private static final int CPU_TIME_SECONDS = 3600;

	public static void main(String[] args) throws IOException, IloException
	{
		String solutionsFolder = "Experiments/Exp7 - Randstad-IC/solutions";
		String outputFile = "Experiments/Exp7 - Randstad-IC/results.csv";

		generateMissingFiles(solutionsFolder);

		File folder = new File(solutionsFolder);
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
		if (files == null || files.length == 0)
		{
			System.out.println("No solution files found in " + solutionsFolder);
			return;
		}

		Map<String, Map<String, String>> budgetLookupCache = new HashMap<>();

		List<Map<String, String>> rows = new ArrayList<>();
		for (File file : files)
		{
			Map<String, String> row = parseFile(file, budgetLookupCache);
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
	}

	// ── Missing-file generation ───────────────────────────────────────────────

	private static void generateMissingFiles(String solutionsFolder) throws IOException, IloException
	{
		Set<Integer> presentIds = getPresentTaskIds(solutionsFolder);
		List<Integer> missingIds = new ArrayList<>();
		for (int i = 0; i < TOTAL_TASKS; i++)
			if (!presentIds.contains(i))
				missingIds.add(i);

		if (missingIds.isEmpty())
			return;

		System.out.println("Generating " + missingIds.size() + " missing solution files...");
		Configuration.initialiseConfiguration("settings.properties");

		for (int taskId : missingIds)
			generateInfeasibleFile(taskId, solutionsFolder);
	}

	private static Set<Integer> getPresentTaskIds(String solutionsFolder)
	{
		Set<Integer> ids = new HashSet<>();
		File folder = new File(solutionsFolder);
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
		if (files == null)
			return ids;
		for (File f : files)
		{
			Matcher m = TASK_ID_PATTERN.matcher(f.getName());
			if (m.find())
				ids.add(Integer.parseInt(m.group(1)));
		}
		return ids;
	}

	private static void generateInfeasibleFile(int taskId, String solutionsFolder) throws IOException, IloException
	{
		// Decode task ID using the same factorial ordering as factorial.sh:
		// budgetIndex(3) x transferPenaltyIndex(3) x lineIndex(3) x routeSolverIndex(14) x fixedCostIndex(3) x odIndex(2)
		int id = taskId;
		int budgetIndex          = id % 3; id /= 3;
		int transferPenaltyIndex = id % 3; id /= 3;
		int lineIndex            = id % 3; id /= 3;
		int routeSolverIndex     = id % 14; id /= 14;
		int fixedCostIndex       = id % 3; id /= 3;
		int odIndex              = id % 2;
		int odMatrixIndex        = odIndex == 0 ? 0 : 5;

		int solverIndex, routeIndex;
		if (routeSolverIndex < 12)
		{
			solverIndex = routeSolverIndex / 3;
			routeIndex  = routeSolverIndex % 3;
		}
		else
		{
			solverIndex = routeSolverIndex - 12 + 4;
			routeIndex  = 0;
		}

		String instanceFolder = "instances/Randstad-IC";

		String odMatrixVariant = switch (odMatrixIndex)
		{
		case 0 -> "0";
		case 5 -> "3-false";
		default -> throw new IllegalArgumentException("Unexpected OD matrix index: " + odMatrixIndex);
		};
		String budgetVariant = switch (budgetIndex)
		{
		case 0 -> "small"; case 1 -> "medium"; case 2 -> "large";
		default -> throw new IllegalArgumentException();
		};
		String budgetFolder = switch (fixedCostIndex)
		{
		case 0 -> "budget-fixedCost-low"; case 1 -> "budget"; case 2 -> "budget-fixedCost-high";
		default -> throw new IllegalArgumentException();
		};
		String transferPenaltyVariant = switch (transferPenaltyIndex)
		{
		case 0 -> "small"; case 1 -> "medium"; case 2 -> "large";
		default -> throw new IllegalArgumentException();
		};
		String lineVariant = switch (lineIndex)
		{
		case 0 -> "small"; case 1 -> "medium"; case 2 -> "large";
		default -> throw new IllegalArgumentException();
		};
		String routeVariant = switch (routeIndex)
		{
		case 0 -> "small"; case 1 -> "medium"; case 2 -> "large";
		default -> throw new IllegalArgumentException();
		};
		String fixedCostVariant = switch (fixedCostIndex)
		{
		case 0 -> "low"; case 1 -> "medium"; case 2 -> "high";
		default -> throw new IllegalArgumentException();
		};
		String solverName = switch (solverIndex)
		{
		case 0 -> "PTNSolver";
		case 1 -> "PTNSolver-Karbstein";
		case 2 -> "PTN-BendersSolver";
		case 3 -> "DPTNSolver-MIP";
		case 4 -> "DPTNSolver-BAP";
		case 5 -> "CGNSolver";
		default -> throw new IllegalArgumentException();
		};

		String odMatrixFile        = instanceFolder + "/ODMatrix/" + odMatrixVariant + ".txt";
		String budgetFile          = instanceFolder + "/" + budgetFolder + "/" + budgetVariant + ".txt";
		String transferPenaltyFile = instanceFolder + "/transferPenalty/" + transferPenaltyVariant + ".txt";
		String fixedCostFile       = instanceFolder + "/fixedCost/" + fixedCostVariant + ".txt";
		String lineGenerationFile  = instanceFolder + "/LineGeneration/" + lineVariant + ".txt";

		LinePlanningParameters.LINE_ACTIVATION_COST = Reader.readInt(fixedCostFile);

		LinePlanningInstance instance = new LinePlanningInstance(instanceFolder, odMatrixFile, budgetFile,
				transferPenaltyFile);
		instance.setLineGenerationFile(lineGenerationFile);
		// Route generation is only set for PTN/DPTN solvers (solverIndex < 4)
		if (solverIndex < 4)
			instance.setRouteGenerationFile(instanceFolder + "/RouteGeneration/" + routeVariant + ".txt");

		LinePlanningSolution solution = LinePlanningSolution.noSolution(instance);
		solution.evaluateTrueObjective();

		String filePath = solutionsFolder + "/synthetic_" + taskId + ".txt";
		writeInfeasibleFile(instance, solverName, solution, filePath);
		System.out.println("  Generated " + filePath);
	}

	private static void writeInfeasibleFile(LinePlanningInstance instance, String solverName,
			LinePlanningSolution solution, String filePath) throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath)))
		{
			writer.write("# Settings");
			writer.newLine();
			writer.write("Instance: " + instance.getFolder());
			writer.newLine();
			writer.write("ODMatrix: " + new File(instance.getOdMatrixFile()).getName().replace(".txt", ""));
			writer.newLine();
			writer.write(String.format(Locale.ROOT, "TotalVolume: %.1f", instance.getTotalVolume()));
			writer.newLine();
			writer.write("TransferPenalty: "
					+ new File(instance.getTransferPenaltyFile()).getName().replace(".txt", "")
					+ " (" + LinePlanningParameters.TRANSFER_PENALTY + ")");
			writer.newLine();
			String rgFile = instance.getRouteGenerationFile();
			if (rgFile != null)
			{
				writer.write("RouteGeneration: " + new File(rgFile).getName().replace(".txt", ""));
				writer.newLine();
			}
			writer.write("LineGeneration: " + new File(instance.getLineGenerationFile()).getName().replace(".txt", ""));
			writer.newLine();
			writer.write("Budget: " + new File(instance.getBudgetFile()).getName().replace(".txt", "")
					+ " (" + instance.getBudget() + ")");
			writer.newLine();
			writer.write("Solver: " + solverName);
			writer.newLine();
			writer.write("TimeLimitSeconds: " + CPU_TIME_SECONDS);
			writer.newLine();
			writer.newLine();

			writer.write("# Results");
			writer.newLine();
			writer.write("Feasible: false");
			writer.newLine();
			writer.write(String.format(Locale.ROOT, "ObjectiveActual: %.4f", solution.getObjectiveActual()));
			writer.newLine();
			writer.write(String.format(Locale.ROOT, "ComputationTimeSeconds: %.1f", solution.getComputationTimeSeconds()));
			writer.newLine();
		}
	}

	// ── Budget-to-FixedCost lookup ────────────────────────────────────────────

	private static Map<String, String> buildBudgetLookup(String instanceFolder) throws IOException
	{
		Map<String, String> lookup = new HashMap<>();
		for (String[] fcEntry : FIXED_COST_FOLDERS)
		{
			String budgetFolder = fcEntry[0];
			String fixedCostLabel = fcEntry[1];
			for (String budgetSize : BUDGET_SIZES)
			{
				File f = new File(instanceFolder + "/" + budgetFolder + "/" + budgetSize + ".txt");
				if (!f.exists())
					continue;
				try (BufferedReader r = new BufferedReader(new FileReader(f)))
				{
					String line = r.readLine();
					if (line != null && !line.isBlank())
						lookup.put(line.trim(), fixedCostLabel);
				}
			}
		}
		return lookup;
	}

	// ── File parsing ──────────────────────────────────────────────────────────

	private static Map<String, String> parseFile(File file, Map<String, Map<String, String>> budgetLookupCache)
			throws IOException
	{
		Map<String, String> row = new LinkedHashMap<>();
		row.put("File", file.getName());

		String section = null;
		String instanceFolder = null;
		String budgetRaw = null;

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

				if (key.equals("Budget"))
				{
					budgetRaw = value;
					value = value.replaceAll("\\s*\\(.*\\)", "").trim();
				}

				row.put(key, value);
				if (key.equals("Instance"))
					instanceFolder = value;
			}
		}

		// Derive FixedCost by matching the numeric budget value against the instance's budget folders
		List<String> warnings = new ArrayList<>();
		String fixedCost = "";
		if (instanceFolder != null && budgetRaw != null)
		{
			Matcher m = BUDGET_NUMBER_PATTERN.matcher(budgetRaw);
			if (m.find())
			{
				String budgetNum = m.group(1);
				Map<String, String> lookup = budgetLookupCache.computeIfAbsent(instanceFolder, k -> {
					try
					{
						return buildBudgetLookup(k);
					}
					catch (IOException e)
					{
						return new HashMap<>();
					}
				});
				fixedCost = lookup.getOrDefault(budgetNum, "");
				if (fixedCost.isEmpty())
					warnings.add("Could not resolve FixedCost for budget value " + budgetNum);
			}
			else
			{
				warnings.add("No numeric value in Budget field: " + budgetRaw);
			}
		}

		String existingWarnings = row.getOrDefault("Warnings", "");
		if (!existingWarnings.isEmpty())
			warnings.add(0, existingWarnings);
		row.put("FixedCost", fixedCost);
		row.put("Warnings", String.join("; ", warnings));

		return row;
	}

	// ── CSV output ────────────────────────────────────────────────────────────

	private static void writeCsv(String outputFile, List<Map<String, String>> rows) throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{
			writer.write(String.join(",", COLUMNS));
			writer.newLine();

			for (Map<String, String> row : rows)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < COLUMNS.length; i++)
				{
					if (i > 0)
						sb.append(",");
					String value = row.getOrDefault(COLUMNS[i], "");
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
