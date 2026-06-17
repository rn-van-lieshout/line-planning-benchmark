package linePlanning.solvers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Locale;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ilog.concert.IloException;
import linePlanning.columnGeneration.columns.LineActivationColumn;
import linePlanning.columnGeneration.columns.LineFrequencyColumn;
import linePlanning.columnGeneration.columns.LinePlanningColumn;
import linePlanning.columnGeneration.columns.RouteColumn;
import linePlanning.columnGeneration.columns.RouteSlackColumn;
import linePlanning.columnGeneration.pricing.LinePlanningPricingProblem;
import linePlanning.PTN.Connection;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.instance.ODPair;
import linePlanning.instance.Route;
import linePlanning.lineGeneration.Line;
import optimisation.columnGeneration.AbstractSolution;

public class LinePlanningSolution
{
	private final LinePlanningInstance instance;
	private final boolean hasSolution;
	private final List<Line> linePlan;
	private final Map<Line, Integer> frequencyMap;
	private final Map<ODPair, Map<Route, Double>> routeMap;
	private final double cost;
	private double computationTimeSeconds = -1;
	private final List<String> warnings = new ArrayList<>();

	// stats on routing estimated by solver
	private final double uncoveredDemandEstimate;
	private final double objectiveEstimate;
	private double directTravelersEstimate;
	private double oneTransferEstimate;
	private double moreThanOneTransferEstimate;
	private double lowerBound;

	// stats on routing obtained by resolving on CGN
	private LinePlanningSolution reoptimizedSolution;
	private double objectiveActual;
	private double uncoveredDemandActual;
	private double directTravelersActual;
	private double oneTransferActual;
	private double moreThanOneTransferActual;

	public LinePlanningSolution(
			AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPricingProblem> abstractSolution,
			LinePlanningInstance instance)
	{
		this.instance = instance;
		if (abstractSolution == null)
		{
			this.hasSolution = false;
			this.linePlan = new ArrayList<>();
			this.frequencyMap = new HashMap<>();
			this.routeMap = new HashMap<>();
			this.objectiveEstimate = 0;
			this.uncoveredDemandEstimate = 0;
			this.cost = 0;
			return;
		}
		this.hasSolution = true;
		this.objectiveEstimate = abstractSolution.getObjectiveValue();
		linePlan = new ArrayList<>();
		frequencyMap = new HashMap<>();
		routeMap = new HashMap<>();
		for (ODPair od : instance.getODPairs())
			routeMap.put(od, new HashMap<>());

		double[] totals = parseColumns(abstractSolution);
		double routedDemand = totals[0], uncoveredDemandTemp = totals[1], objectiveCheck = totals[2];

		checkDemandBalance(abstractSolution, routedDemand, uncoveredDemandTemp);
		checkObjective(abstractSolution, objectiveCheck);

		this.uncoveredDemandEstimate = uncoveredDemandTemp;
		this.cost = computeCost();
		double[] routing = computeRouting(routeMap);
		directTravelersEstimate = routing[0];
		oneTransferEstimate = routing[1];
		moreThanOneTransferEstimate = routing[2];
	}

	// Returns double[] {routedDemand, uncoveredDemandTemp, objectiveCheck}.
	// Populates linePlan, frequencyMap, and routeMap as side effects.
	private double[] parseColumns(
			AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPricingProblem> abstractSolution)
	{
		double routedDemand = 0, uncoveredDemandTemp = 0, objectiveCheck = 0;
		for (Entry<LinePlanningColumn, Double> entry : abstractSolution.getColumnMap().entrySet())
		{
			if (entry.getKey() instanceof LineActivationColumn)
			{
				double val = entry.getValue();
				int freq = (int) Math.round(val);
				if (Math.abs(freq - val) > 0.01)
					throw new Error(" value of activation is " + val);
			}
			if (entry.getKey() instanceof LineFrequencyColumn)
			{
				double val = entry.getValue();
				int freq = (int) Math.round(val);
				if (Math.abs(freq - val) > 0.01)
					throw new Error(" value of freq is " + val);
				if (freq == 0)
					continue;
				LineFrequencyColumn lCol = (LineFrequencyColumn) entry.getKey();
				linePlan.add(lCol.getLine());
				frequencyMap.put(lCol.getLine(), freq);
			}
			if (entry.getKey() instanceof RouteSlackColumn)
			{
				RouteSlackColumn rsCol = (RouteSlackColumn) entry.getKey();
				uncoveredDemandTemp += entry.getValue();
				objectiveCheck += entry.getValue() * instance.getMissedDemandPenalty(rsCol.getODPair());
			}
			else if (entry.getKey() instanceof RouteColumn)
			{
				RouteColumn rCol = (RouteColumn) entry.getKey();
				routeMap.get(rCol.getODPair()).put(rCol.getRoute(), entry.getValue());
				objectiveCheck += rCol.getRoute().getDuration() * entry.getValue();
				routedDemand += entry.getValue();
			}
		}
		return new double[] { routedDemand, uncoveredDemandTemp, objectiveCheck };
	}

	private void checkDemandBalance(
			AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPricingProblem> abstractSolution,
			double routedDemand, double uncoveredDemandTemp)
	{
		Map<ODPair, Double> odResidual = new HashMap<>();
		for (ODPair od : instance.getODPairs())
			odResidual.put(od, od.getVolume());
		for (Entry<LinePlanningColumn, Double> e : abstractSolution.getColumnMap().entrySet())
		{
			ODPair od = null;
			if (e.getKey() instanceof RouteSlackColumn) od = ((RouteSlackColumn) e.getKey()).getODPair();
			else if (e.getKey() instanceof RouteColumn) od = ((RouteColumn) e.getKey()).getODPair();
			if (od != null) odResidual.merge(od, -e.getValue(), Double::sum);
		}
		odResidual.entrySet().stream()
			.filter(e -> Math.abs(e.getValue()) > 0.001)
			.sorted(Map.Entry.<ODPair, Double>comparingByValue().reversed())
			.forEach(e -> {
				String w = String.format(Locale.ROOT, "DemandMismatch OD %s->%s residual=%.6f",
					e.getKey().getOrigin().getName(), e.getKey().getDestination().getName(), e.getValue());
				warnings.add(w);
				System.out.printf(Locale.ROOT, "  [DemandDebug] OD %s -> %s: demand=%.4f residual=%.6f penalty=%.4f%n",
					e.getKey().getOrigin(), e.getKey().getDestination(),
					e.getKey().getVolume(), e.getValue(),
					instance.getMissedDemandPenalty(e.getKey()));
			});

		if (Math.abs(routedDemand + uncoveredDemandTemp - instance.getTotalVolume()) > 0.001)
		{
			double misMatch = routedDemand + uncoveredDemandTemp - instance.getTotalVolume();
			String warning = String.format(Locale.ROOT, "DemandMismatch=%.4f", misMatch);
			System.out.println(warning);
			warnings.add(warning);
		}
	}

	private void checkObjective(
			AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPricingProblem> abstractSolution,
			double objectiveCheck)
	{
		if (Math.abs(objectiveCheck - objectiveEstimate) <= 0.001)
			return;
		String warning = String.format(Locale.ROOT, "ObjectiveMismatch: model=%.4f recomputed=%.4f",
				objectiveEstimate, objectiveCheck);
		System.out.println(warning);
		warnings.add(warning);
		double sumByCoeff = 0, sumRoute = 0, sumSlack = 0, sumLineFreq = 0, sumLineAct = 0, sumOther = 0;
		for (Entry<LinePlanningColumn, Double> e : abstractSolution.getColumnMap().entrySet())
		{
			double contrib = e.getKey().getCoefficient() * e.getValue();
			sumByCoeff += contrib;
			if (e.getKey() instanceof RouteSlackColumn) sumSlack += contrib;
			else if (e.getKey() instanceof RouteColumn) sumRoute += contrib;
			else if (e.getKey() instanceof LineFrequencyColumn) sumLineFreq += contrib;
			else if (e.getKey() instanceof LineActivationColumn) sumLineAct += contrib;
			else sumOther += contrib;
		}
		System.out.printf(Locale.ROOT,
			"[MismatchDebug] sumByStoredCoeff=%.4f (route=%.4f slack=%.4f lineFreq=%.4f lineAct=%.4f other=%.4f)%n",
			sumByCoeff, sumRoute, sumSlack, sumLineFreq, sumLineAct, sumOther);
		throw new Error();
	}

	private double computeCost()
	{
		double cost = 0;
		for (Line l : linePlan)
			cost += LinePlanningParameters.LINE_ACTIVATION_COST + l.getCost() * frequencyMap.get(l);
		return cost;
	}

	public void printToConsole()
	{
		if (!hasSolution)
		{
			System.out.println("No solution found.");
			return;
		}
		System.out.println("Optimized Line Plan:");
		for (Line l : linePlan)
		{
			System.out.println("Frequency " + frequencyMap.get(l) + " on " + l);
		}
		System.out.printf("Spend %.1f of total budget %.1f%n", cost, (double) instance.getBudget());
		double volume = instance.getTotalVolume();
		System.out.printf("Total volume: %.1f%n", volume);
		System.out.printf(
				"Routing (Estimate): %.1f%% direct, %.1f%% one transfer, %.1f%% two or more, %.1f%% unserved%n",
				100 * directTravelersEstimate / volume, 100 * oneTransferEstimate / volume,
				100 * moreThanOneTransferEstimate / volume, 100 * uncoveredDemandEstimate / volume);
		System.out.printf(
				"Routing (Actual):   %.1f%% direct, %.1f%% one transfer, %.1f%% two or more, %.1f%% unserved%n",
				100 * directTravelersActual / volume, 100 * oneTransferActual / volume,
				100 * moreThanOneTransferActual / volume, 100 * uncoveredDemandActual / volume);
		if (computationTimeSeconds >= 0)
			System.out.printf("Computation time: %.1f s%n", computationTimeSeconds);
		double gap = getOptimalityGap();
		if (gap >= 0)
			System.out.printf("Optimality gap: %.2f%%%n", 100 * gap);
		System.out.printf("Objective (Estimate): %.4f%n", objectiveEstimate);
		System.out.printf("Objective (Actual):   %.4f%n", objectiveActual);

		Map<ODPair, Map<Route, Double>> activeRouteMap = reoptimizedSolution != null ? reoptimizedSolution.routeMap
				: routeMap;
		System.out.println("Top 10 unserved ODs:");
		instance.getODPairs().stream().map(od -> {
			double routed = activeRouteMap.get(od).values().stream().mapToDouble(Double::doubleValue).sum();
			return Map.entry(od, od.getVolume() - routed);
		}).filter(e -> e.getValue() > 1e-6)
				.sorted(Comparator.comparingDouble(Map.Entry<ODPair, Double>::getValue).reversed()).limit(10)
				.forEach(e -> System.out.printf("  %s -> %s: %.2f unserved (%.1f%%)%n",
						e.getKey().getOrigin().getNameFull(), e.getKey().getDestination().getNameFull(), e.getValue(),
						100.0 * e.getValue() / e.getKey().getVolume()));
	}

	public void exportLinePlan(String solverName, int cpuTimeSeconds, boolean useStrongInequalities, int pairsPerGroup) throws IOException
	{
		String solutionsFolder = instance.getFolder() + "/solutions";
		new File(solutionsFolder).mkdirs();

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		String slurmTaskId = System.getenv("SLURM_ARRAY_TASK_ID");
		String identifier = slurmTaskId != null ? timestamp + "_" + slurmTaskId : timestamp;
		String filePath = solutionsFolder + "/" + identifier + ".txt";

		double volume = instance.getTotalVolume();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath)))
		{
			// ── Settings ──────────────────────────────────────────────────────────
			writer.write("# Settings");
			writer.newLine();
			writer.write("Instance: " + instance.getFolder());
			writer.newLine();
			writer.write("ODMatrix: " + new File(instance.getOdMatrixFile()).getName().replace(".txt", ""));
			writer.newLine();
			writer.write(String.format(Locale.ROOT, "TotalVolume: %.1f%n", instance.getTotalVolume()));
			writer.write("TransferPenalty: " + new File(instance.getTransferPenaltyFile()).getName().replace(".txt", "")
					+ " (" + LinePlanningParameters.TRANSFER_PENALTY + ")");
			writer.newLine();
			String lgFile = instance.getLineGenerationFile();
			if (lgFile != null)
			{
				writer.write("LineGeneration: " + new File(lgFile).getName().replace(".txt", ""));
				writer.newLine();
			}
			writer.write("Budget: " + new File(instance.getBudgetFile()).getName().replace(".txt", "") + " ("
					+ instance.getBudget() + ")");
			writer.newLine();
			writer.write("LineActivationCost: " + new File(instance.getFixedCostFile()).getName().replace(".txt", ""));
			writer.newLine();
			writer.write("Solver: " + solverName);
			writer.newLine();
			String rgFile = instance.getRouteGenerationFile();
			if (rgFile != null)
			{
				writer.write("RouteGeneration: " + new File(rgFile).getName().replace(".txt", ""));
				writer.newLine();
			}
			if (rgFile == null)
			{
				writer.write("StrongInequalities: " + useStrongInequalities);
				writer.newLine();
				writer.write("PairsPerGroup: " + pairsPerGroup);
				writer.newLine();
			}
			writer.write("TimeLimitSeconds: " + cpuTimeSeconds);
			writer.newLine();
			writer.newLine();

			// ── Results (machine-readable) ────────────────────────────────────────
			writer.write("# Results");
			writer.newLine();
			writer.write("Feasible: " + hasSolution);
			writer.newLine();
			if (!hasSolution)
			{
				writer.write(String.format(Locale.ROOT, "ObjectiveActual: %.4f%n", objectiveActual));
				writer.write(String.format(Locale.ROOT, "ComputationTimeSeconds: %.1f%n", computationTimeSeconds));
			}
			else
			{
			writer.write(String.format(Locale.ROOT, "ObjectiveEstimate: %.4f%n", objectiveEstimate));
			writer.write(String.format(Locale.ROOT, "Cost: %.4f%n", cost));
			writer.write(String.format(Locale.ROOT, "ComputationTimeSeconds: %.1f%n", computationTimeSeconds));
			writer.write(String.format(Locale.ROOT, "OptimalityGap: %.4f%n", getOptimalityGap()));

			if (reoptimizedSolution != null)
			{
				writer.write(String.format(Locale.ROOT, "DirectTravelersEstimatePct: %.2f%n",
						100 * directTravelersEstimate / volume));
				writer.write(String.format(Locale.ROOT, "OneTransferEstimatePct: %.2f%n",
						100 * oneTransferEstimate / volume));
				writer.write(String.format(Locale.ROOT, "TwoPlusTransfersEstimatePct: %.2f%n",
						100 * moreThanOneTransferEstimate / volume));
				writer.write(String.format(Locale.ROOT, "ObjectiveActual: %.4f%n", objectiveActual));
				writer.write(String.format(Locale.ROOT, "UncoveredDemandActualPct: %.2f%n",
						100 * uncoveredDemandActual / volume));
				writer.write(String.format(Locale.ROOT, "DirectTravelersActualPct: %.2f%n",
						100 * directTravelersActual / volume));
				writer.write(
						String.format(Locale.ROOT, "OneTransferActualPct: %.2f%n", 100 * oneTransferActual / volume));
				writer.write(String.format(Locale.ROOT, "TwoPlusTransfersActualPct: %.2f%n",
						100 * moreThanOneTransferActual / volume));
			}
			if (!warnings.isEmpty())
			{
				writer.write("Warnings: " + String.join("; ", warnings));
				writer.newLine();
			}
			writer.newLine();

			// ── LinePlan (machine-readable) ───────────────────────────────────────
			writer.write("# LinePlan");
			writer.newLine();
			for (Line l : linePlan)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(frequencyMap.get(l));
				for (Connection c : l.getConnectionPathForward())
				{
					sb.append(" ").append(c.getIndex());
				}
				writer.write(sb.toString());
				writer.newLine();
			}
			writer.newLine();

			// ── Human-readable line plan ──────────────────────────────────────────
			writer.write("# Human-Readable Line Plan");
			writer.newLine();
			linePlan.stream().sorted((a, b) -> Integer.compare(frequencyMap.get(b), frequencyMap.get(a))).forEach(l -> {
				String type = switch (l.getType())
				{
				case LinePlanningParameters.SPRINTER -> "SPR";
				case LinePlanningParameters.INTERCITY -> "IC ";
				default -> "MIX";
				};
				StringBuilder sb = new StringBuilder();
				sb.append(l.getOrigin().getNameFull());
				for (Connection c : l.getConnectionPathForward())
				{
					sb.append(" - ").append(c.getTo().getNameFull());
				}
				try
				{
					writer.write(String.format(Locale.ROOT, "f=%-2d  %s  %s  (%.1f min)%n", frequencyMap.get(l), type,
							sb, l.getDuration()));
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			});
			} // end else (hasSolution)
		}
		try (BufferedReader verifier = new BufferedReader(new FileReader(filePath)))
		{
			String firstLine = verifier.readLine();
			if (!"# Settings".equals(firstLine))
				throw new IOException("File verification failed for " + filePath + ": unexpected first line: " + firstLine);
		}
		System.out.println("Solution exported to " + filePath);
	}

	public static LinePlanningSolution noSolution(LinePlanningInstance instance)
	{
		return new LinePlanningSolution(instance, new ArrayList<>(), new HashMap<>(), false);
	}

	// Package-private constructor for loading a solution from file
	LinePlanningSolution(LinePlanningInstance instance, List<Line> linePlan, Map<Line, Integer> frequencyMap)
	{
		this(instance, linePlan, frequencyMap, true);
	}

	private LinePlanningSolution(LinePlanningInstance instance, List<Line> linePlan, Map<Line, Integer> frequencyMap,
			boolean hasSolution)
	{
		this.hasSolution = hasSolution;
		this.instance = instance;
		this.linePlan = linePlan;
		this.frequencyMap = frequencyMap;
		this.routeMap = new HashMap<>();
		for (ODPair od : instance.getODPairs())
		{
			routeMap.put(od, new HashMap<>());
		}
		this.objectiveEstimate = 0;
		this.uncoveredDemandEstimate = 0;
		double costTemp = 0;
		for (Line l : linePlan)
		{
			costTemp += LinePlanningParameters.LINE_ACTIVATION_COST + l.getCost() * frequencyMap.get(l);
		}
		this.cost = costTemp;
	}

	public void evaluateTrueObjective() throws IloException
	{
		if (!hasSolution)
		{
			this.uncoveredDemandActual = instance.getTotalVolume();
			this.objectiveActual = instance.getODPairs().stream()
					.mapToDouble(od -> od.getVolume() * instance.getMissedDemandPenalty(od)).sum();
			return;
		}
		CGNEvaluator evaluator = new CGNEvaluator();
		this.reoptimizedSolution = evaluator.solve(instance, frequencyMap);
		this.objectiveActual = reoptimizedSolution.objectiveEstimate;
		this.uncoveredDemandActual = reoptimizedSolution.uncoveredDemandEstimate;

		double[] reopt = computeRouting(reoptimizedSolution.routeMap);
		directTravelersActual = reopt[0];
		oneTransferActual = reopt[1];
		moreThanOneTransferActual = reopt[2];
	}

	public void compareEstimateAndActualRouting()
	{
		double totalTravelTimeEstimate = 0;
		double totalTravelTimeActual = 0;
		for (ODPair od : instance.getODPairs())
		{
			double travelTimeEstimate = 0;
			for (Entry<Route, Double> ent : routeMap.get(od).entrySet())
			{
				travelTimeEstimate += ent.getValue() * ent.getKey().getDuration() / od.getVolume();
			}
			totalTravelTimeEstimate += travelTimeEstimate * od.getVolume();

			double travelTimeActual = 0;
			for (Entry<Route, Double> ent : reoptimizedSolution.routeMap.get(od).entrySet())
			{
				travelTimeActual += ent.getValue() * ent.getKey().getDuration() / od.getVolume();
			}
			totalTravelTimeActual += travelTimeActual * od.getVolume();

			if (travelTimeActual > travelTimeEstimate + 25)
			{
				System.out.println("Estimate: " + travelTimeEstimate + " Actual: " + travelTimeActual);
				System.out.println("Estimated routing: ");
				for (Entry<Route, Double> ent : routeMap.get(od).entrySet())
				{
					System.out.println(ent.getValue() + " goes to " + ent.getKey() + " with duration "
							+ ent.getKey().getDuration());
				}
				System.out.println("Actual routing: ");
				for (Entry<Route, Double> ent : reoptimizedSolution.routeMap.get(od).entrySet())
				{
					System.out.println(ent.getValue() + " goes to " + ent.getKey() + " with duration "
							+ ent.getKey().getDuration());
				}
			}
		}
		System.out.println("Total estimate:  " + totalTravelTimeEstimate);
		System.out.println("Total actual:  " + totalTravelTimeActual);

	}

	private double[] computeRouting(Map<ODPair, Map<Route, Double>> map)
	{
		double direct = 0, one = 0, more = 0;
		for (Map<Route, Double> innerMap : map.values())
			for (Entry<Route, Double> ent : innerMap.entrySet())
			{
				int n = ent.getKey().getNrTransfers();
				if (n == 0)
					direct += ent.getValue();
				else if (n == 1)
					one += ent.getValue();
				else
					more += ent.getValue();
			}
		return new double[]
		{ direct, one, more };
	}

	public List<Line> getLinePlan()
	{
		return linePlan;
	}

	public Map<ODPair, Map<Route, Double>> getRouteMap()
	{
		return routeMap;
	}

	public LinePlanningSolution getReoptimizedSolution()
	{
		return reoptimizedSolution;
	}

	public void setLowerBound(double lowerBound)
	{
		this.lowerBound = lowerBound;
	}

	public double getOptimalityGap()
	{
		if (lowerBound <= 0 || objectiveEstimate <= 0)
			return -1;
		return (objectiveEstimate - lowerBound) / objectiveEstimate;
	}

	public void setComputationTimeSeconds(double computationTimeSeconds)
	{
		this.computationTimeSeconds = computationTimeSeconds;
	}

	public double getObjectiveActual()
	{
		return objectiveActual;
	}

	public double getComputationTimeSeconds()
	{
		return computationTimeSeconds;
	}
}
