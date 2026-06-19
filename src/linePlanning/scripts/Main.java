package linePlanning.scripts;

import java.io.IOException;
import java.util.List;

import ilog.concert.IloException;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.instance.LinePlanningSolution;
import linePlanning.lineGeneration.Line;
import linePlanning.lineGeneration.LinePoolGenerator;
import linePlanning.solvers.CGNSolver;
import linePlanning.solvers.DPTNSolver;
import linePlanning.solvers.PTNBendersSolver;
import linePlanning.solvers.PTNSolver;
import linePlanning.solvers.Solver;
import util.Configuration;
import util.Reader;

public class Main
{
	public static void main(String[] args) throws IOException, IloException
	{
		// ── Settings ────────────────────────────────────────────────────────────────
		// 0 = Randstad-IC, 1 = Randstad-IC-SPR, 2 = Nederland-IC, 3 = Nederland-IC-SPR,
		// 4 = SmallSwitzerland, 5 = Switzerland
		int instanceIndex = 0;

		// 0 = small (MAX_DETOUR_MINUTES = 0), 1 = medium (15), 2 = large (30)
		int routeGenerationIndex = 0;

		// 0 = small (detour ratio 1.1), 1 = medium (1.3), 2 = large (1.5)
		int lineGenerationIndex = 0;

		// 0 = Dense, 1 = Sparse
		int odMatrixIndex = 0;

		// 0 = small (15 min.), 1 = medium (15), 2 = large (25)
		int transferPenaltyIndex = 1;

		// 0 = small, 1 = medium, 2 = large
		int budgetIndex = 2;

		// 0 = low, 1 = medium, 2 = high
		int fixedCostIndex = 1;

		// 0 = PTNSolver, 1 = PTNSolver (B&P), 2 = PTNBendersSolver,
		// 3 = DPTNSolver, 4 = DPTNSolver (B&P), 5 = CGNSolver
		int solverIndex = 1;
		boolean useStrongInequalities = false;
		int cpuTimeSeconds = 60;
		double demandScalar = 1.0;
		// ── End of settings ──────────────────────────────────────────────────────────

		String dataFolder = "instances";

		String instanceFolder = dataFolder + "/" + switch (instanceIndex)
		{
		case 0 -> "Randstad-IC";
		case 1 -> "Randstad-IC-SPR";
		case 2 -> "Nederland-IC";
		case 3 -> "Nederland-IC-SPR";
		case 4 -> "SmallSwitzerland";
		case 5 -> "Switzerland";
		default -> throw new IllegalArgumentException("Unknown instance index: " + instanceIndex);
		};

		String routeGenerationVariant = switch (routeGenerationIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown route generation index: " + routeGenerationIndex);
		};

		String lineGenerationVariant = switch (lineGenerationIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown line generation index: " + lineGenerationIndex);
		};

		String odMatrixVariant = switch (odMatrixIndex)
		{
		case 0 -> "0";
		case 1 -> "3-true";
		default -> throw new IllegalArgumentException("Unknown OD matrix index: " + odMatrixIndex);
		};
		String odMatrixFile = instanceFolder + "/ODMatrix/" + odMatrixVariant + ".txt";

		String transferPenaltyVariant = switch (transferPenaltyIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown transfer penalty index: " + transferPenaltyIndex);
		};
		String transferPenaltyFile = instanceFolder + "/transferPenalty/" + transferPenaltyVariant + ".txt";

		String budgetVariant = switch (budgetIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown budget index: " + budgetIndex);
		};

		// Budget folder depends on fixed cost level (calibrated via calibrate_fixed_cost_budgets.sh)
		String budgetFolder = switch (fixedCostIndex)
		{
		case 0 -> "budget-fixedCost-low";
		case 1 -> "budget";
		case 2 -> "budget-fixedCost-high";
		default -> throw new IllegalArgumentException("Unknown fixed cost index: " + fixedCostIndex);
		};
		String budgetFile = instanceFolder + "/" + budgetFolder + "/" + budgetVariant + ".txt";

		String fixedCostVariant = switch (fixedCostIndex)
		{
		case 0 -> "low";
		case 1 -> "medium";
		case 2 -> "high";
		default -> throw new IllegalArgumentException("Unknown fixed cost index: " + fixedCostIndex);
		};
		String fixedCostFile = instanceFolder + "/fixedCost/" + fixedCostVariant + ".txt";

		Configuration.initialiseConfiguration("settings.properties");

		LinePlanningInstance instance = new LinePlanningInstance(instanceFolder, odMatrixFile, budgetFile,
				transferPenaltyFile, demandScalar);
		instance.setLineGenerationFile(instanceFolder + "/LineGeneration/" + lineGenerationVariant + ".txt");

		LinePoolGenerator lpGen = new LinePoolGenerator(instance);
		List<Line> linePool = lpGen.getLines();
		instance.setLinePool(linePool);

		Solver solver = switch (solverIndex)
		{
		case 0 -> new PTNSolver(false);
		case 1 -> new PTNSolver(true);
		case 2 -> new PTNBendersSolver();
		case 3 -> new DPTNSolver(false);
		case 4 -> new DPTNSolver(true);
		case 5 -> new CGNSolver();
		default -> throw new IllegalArgumentException("Unknown solver index: " + solverIndex);
		};
		if (solverIndex < 4)
		{
			instance.setRouteGenerationFile(instanceFolder + "/RouteGeneration/" + routeGenerationVariant + ".txt");
		}

		instance.setFixedCostFile(fixedCostFile);
		LinePlanningParameters.LINE_ACTIVATION_COST = Reader.readInt(fixedCostFile);
		solver.setUseStrongInequalities(useStrongInequalities);

		System.out.println("Calling solver");
		LinePlanningSolution solution = solver.solve(instance, linePool, cpuTimeSeconds);
		if (solution == null)
			solution = LinePlanningSolution.noSolution(instance);
		solution.evaluateTrueObjective();
		solution.printToConsole();
		solution.exportLinePlan(solver.getName(), cpuTimeSeconds, useStrongInequalities);
	}
}
