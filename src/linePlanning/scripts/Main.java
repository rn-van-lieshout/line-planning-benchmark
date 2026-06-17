package linePlanning.scripts;

import java.io.IOException;
import java.util.List;

import ilog.concert.IloException;
import linePlanning.data.Reader;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.instance.ODPair;
import linePlanning.instance.Station;
import linePlanning.instance.StationNode;
import linePlanning.lineGeneration.Line;
import linePlanning.lineGeneration.LinePoolGenerator;
import linePlanning.solvers.CGNSolver;
import linePlanning.solvers.DPTNSolver;
import linePlanning.solvers.LinePlanningSolution;
import linePlanning.solvers.PTNBendersSolver;
import linePlanning.solvers.PTNSolver;
import linePlanning.solvers.Solver;
import util.Configuration;

public class Main
{
	public static void main(String[] args) throws IOException, IloException
	{
		String dataFolder = "instances";

		// 0 = Randstad-IC, 1 = Randstad-IC-SPR, 2 = Nederland-IC, 3 = Nederland-IC-SPR,
		// 4 = SmallSwitzerland, 5 = Switzerland
		int instanceIndex = 0;
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
		// 0 = small (MAX_DETOUR_MINUTES = 0), 1 = medium (MAX_DETOUR_MINUTES = 15), 2 =
		// large (MAX_DETOUR_MINUTES = 30)
		int routeGenerationIndex = 0;
		String routeGenerationVariant = switch (routeGenerationIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown route generation index: " + routeGenerationIndex);
		};

		// 0 = small (1.1, 60), 1 = medium (1.3, 90), 2 = large (1.5, 120)
		int lineGenerationIndex = 0;
		String lineGenerationVariant = switch (lineGenerationIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown line generation index: " + lineGenerationIndex);
		};

		// 0 = raw, 1 = strategy1-split, 2 = strategy1-agg, 3 = strategy2-split, 4 =
		// strategy2-agg, 5 = strategy3-split, 6 = strategy3-agg
		int odMatrixIndex = 0;
		String odMatrixVariant = switch (odMatrixIndex)
		{
		case 0 -> "0";
		case 1 -> "1-false";
		case 2 -> "1-true";
		case 3 -> "2-false";
		case 4 -> "2-true";
		case 5 -> "3-false";
		case 6 -> "3-true";
		default -> throw new IllegalArgumentException("Unknown OD matrix index: " + odMatrixIndex);
		};
		String odMatrixFile = instanceFolder + "/ODMatrix/" + odMatrixVariant + ".txt";

		// 0 = small, 1 = medium, 2 = large
		int transferPenaltyIndex = 1;
		String transferPenaltyVariant = switch (transferPenaltyIndex)
		{
		case 0 -> "small";
		case 1 -> "medium";
		case 2 -> "large";
		default -> throw new IllegalArgumentException("Unknown transfer penalty index: " + transferPenaltyIndex);
		};
		String transferPenaltyFile = instanceFolder + "/transferPenalty/" + transferPenaltyVariant + ".txt";

		int budgetIndex = 1;
		// 0 = small, 1 = medium, 2 = large
        String budgetVariant = switch (budgetIndex) {
            case 0 -> "small";
            case 1 -> "medium";
            case 2 -> "large";
            default -> throw new IllegalArgumentException("Unknown budget index: " + budgetIndex);
        };

		int fixedCostIndex = 1;
        // Budget folder depends on fixed cost level (calibrated via calibrate_fixed_cost_budgets.sh)
        String budgetFolder = switch (fixedCostIndex) {
            case 0 -> "budget-fixedCost-low";
            case 1 -> "budget";
            case 2 -> "budget-fixedCost-high";
            default -> throw new IllegalArgumentException("Unknown fixed cost index: " + fixedCostIndex);
        };
        String budgetFile = instanceFolder + "/" + budgetFolder + "/" + budgetVariant + ".txt";

		// 0 = low (0), 1 = medium (1), 2 = high (2)
		String fixedCostVariant = switch (fixedCostIndex)
		{
		case 0 -> "low";
		case 1 -> "medium";
		case 2 -> "high";
		default -> throw new IllegalArgumentException("Unknown fixed cost index: " + fixedCostIndex);
		};
		String fixedCostFile = instanceFolder + "/fixedCost/" + fixedCostVariant + ".txt";

		double demandScalar = 1.0;

		Configuration.initialiseConfiguration("settings.properties");

		LinePlanningInstance instance = new LinePlanningInstance(instanceFolder, odMatrixFile, budgetFile,
				transferPenaltyFile, demandScalar);
		instance.setLineGenerationFile(instanceFolder + "/LineGeneration/" + lineGenerationVariant + ".txt");

		System.out.println("Printing stations in Connection Network");
		for(StationNode s: instance.getConnectionNetwork().getNodes()) {
			System.out.println(s);
		}


		LinePoolGenerator lpGen = new LinePoolGenerator(instance);
		List<Line> linePool = lpGen.getLines();
		instance.setLinePool(linePool);

		int solverIndex = 2;
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

		 // Solver settings.
		instance.setFixedCostFile(fixedCostFile);
		LinePlanningParameters.LINE_ACTIVATION_COST = Reader.readInt(fixedCostFile);
        boolean useStrongInequalities = false;
        solver.setUseStrongInequalities(useStrongInequalities);
        solver.setPairsPerGroup(1);   

        // Call solver.
		int cpuTimeSeconds = 60;
		System.out.println("Calling solver");
		LinePlanningSolution solution = solver.solve(instance, linePool, cpuTimeSeconds);
		if (solution == null)
			solution = LinePlanningSolution.noSolution(instance);
		solution.evaluateTrueObjective();
		solution.printToConsole();
		solution.exportLinePlan(solver.getName(), cpuTimeSeconds, useStrongInequalities, 1);
	}
}
