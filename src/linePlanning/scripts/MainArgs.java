package linePlanning.scripts;

import java.io.IOException;
import java.util.List;

import ilog.concert.IloException;
import linePlanning.data.Reader;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.lineGeneration.Line;
import linePlanning.lineGeneration.LinePoolGenerator;
import linePlanning.solvers.CGNSolver;
import linePlanning.solvers.PTNBendersSolver;
import linePlanning.solvers.PTNSolver;
import linePlanning.solvers.DPTNSolver;
import linePlanning.solvers.LinePlanningSolution;
import linePlanning.solvers.Solver;
import util.Configuration;

/**
 * Command-line entry point. Arguments (all required, in order):
 *   0  instanceIndex        0=Randstad-IC, 1=Randstad-IC-SPR, 2=Nederland-IC, 3=Nederland-IC-SPR, 4=SmallSwitzerland, 5=Switzerland
 *   1  odMatrixIndex        0=raw, 1=1-false, 2=1-true, 3=2-false, 4=2-true, 5=3-false, 6=3-true
 *   2  budgetIndex          0=small, 1=medium, 2=large
 *   3  transferPenaltyIndex 0=small, 1=medium, 2=large
 *   4  fixedCostIndex       0=low(0), 1=medium(1), 2=high(2)
 *   5  lineGenerationIndex  0=small(1.1,60), 1=medium(1.3,90), 2=large(1.5,120)
 *   6  routeGenerationIndex 0=small(15min), 1=large(30min)  [ignored for solvers 4-5]
 *   7  solverIndex          0=CNSolver, 1=CNSolver+K, 2=CNBendersSolver, 3=DEPTNSolver(MIP), 4=DEPTNSolver(B&P), 5=CGNSolver
 *   8  cpuTimeSeconds
 *   9  useStrongInequalities
 */
public class MainArgs {
    public static void main(String[] args) throws IOException, IloException
    {
        if (args.length != 10)
        {
            System.err.println("Usage: MainArgs <instanceIndex> <odMatrixIndex> <budgetIndex> <transferPenaltyIndex> <fixedCostIndex> <lineGenerationIndex> <routeGenerationIndex> <solverIndex> <cpuTimeSeconds> <useStrongInequalities>");
            System.exit(1);
        }

        int instanceIndex        = Integer.parseInt(args[0]);
        int odMatrixIndex        = Integer.parseInt(args[1]);
        int budgetIndex          = Integer.parseInt(args[2]);
        int transferPenaltyIndex = Integer.parseInt(args[3]);
        int fixedCostIndex       = Integer.parseInt(args[4]);
        int lineGenerationIndex  = Integer.parseInt(args[5]);
        int routeGenerationIndex = Integer.parseInt(args[6]);
        int solverIndex          = Integer.parseInt(args[7]);
        int cpuTimeSeconds       = Integer.parseInt(args[8]);
        boolean useStrongInequalities = Boolean.parseBoolean(args[9]);
        int pairsPerGroup = 1;

        String dataFolder = "instances";

        // 0 = Randstad-IC, 1 = Randstad-IC-SPR, 2 = Nederland-IC, 3 = Nederland-IC-SPR, 4 = SmallSwitzerland, 5 = Switzerland
        String instanceFolder = dataFolder + "/" + switch (instanceIndex) {
            case 0 -> "Randstad-IC";
            case 1 -> "Randstad-IC-SPR";
            case 2 -> "Nederland-IC";
            case 3 -> "Nederland-IC-SPR";
            case 4 -> "SmallSwitzerland";
            case 5 -> "Switzerland";
            default -> throw new IllegalArgumentException("Unknown instance index: " + instanceIndex);
        };

        // 0 = raw, 1 = strategy1-split, 2 = strategy1-agg, 3 = strategy2-split, 4 = strategy2-agg, 5 = strategy3-split, 6 = strategy3-agg
        String odMatrixVariant = switch (odMatrixIndex) {
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
        String transferPenaltyVariant = switch (transferPenaltyIndex) {
            case 0 -> "small";
            case 1 -> "medium";
            case 2 -> "large";
            default -> throw new IllegalArgumentException("Unknown transfer penalty index: " + transferPenaltyIndex);
        };
        String transferPenaltyFile = instanceFolder + "/transferPenalty/" + transferPenaltyVariant + ".txt";

        // 0 = small, 1 = medium, 2 = large
        String budgetVariant = switch (budgetIndex) {
            case 0 -> "small";
            case 1 -> "medium";
            case 2 -> "large";
            default -> throw new IllegalArgumentException("Unknown budget index: " + budgetIndex);
        };
        // Budget folder depends on fixed cost level (calibrated via calibrate_fixed_cost_budgets.sh)
        String budgetFolder = switch (fixedCostIndex) {
            case 0 -> "budget-fixedCost-low";
            case 1 -> "budget";
            case 2 -> "budget-fixedCost-high";
            default -> throw new IllegalArgumentException("Unknown fixed cost index: " + fixedCostIndex);
        };
        String budgetFile = instanceFolder + "/" + budgetFolder + "/" + budgetVariant + ".txt";

        // 0 = small (1.1, 60), 1 = medium (1.3, 90), 2 = large (1.5, 120)
        String lineGenerationVariant = switch (lineGenerationIndex) {
            case 0 -> "small";
            case 1 -> "medium";
            case 2 -> "large";
            default -> throw new IllegalArgumentException("Unknown line generation index: " + lineGenerationIndex);
        };

        // 0 = small (MAX_DETOUR_MINUTES = 0), 1 = medium (MAX_DETOUR_MINUTES = 15), 2 = large (MAX_DETOUR_MINUTES = 30)
        String routeGenerationVariant = switch (routeGenerationIndex) {
            case 0 -> "small";
            case 1 -> "medium";
            case 2 -> "large";
            default -> throw new IllegalArgumentException("Unknown route generation index: " + routeGenerationIndex);
        };

        Configuration.initialiseConfiguration("settings.properties");

        LinePlanningInstance instance = new LinePlanningInstance(instanceFolder, odMatrixFile, budgetFile, transferPenaltyFile);
        instance.setLineGenerationFile(instanceFolder + "/LineGeneration/" + lineGenerationVariant + ".txt");

        LinePoolGenerator lpGen = new LinePoolGenerator(instance);
        List<Line> linePool = lpGen.getLines();
        instance.setLinePool(linePool);

        Solver solver = switch (solverIndex) {
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
        
        // 0 = low (0), 1 = medium (1), 2 = high (2)
        String fixedCostVariant = switch (fixedCostIndex) {
            case 0 -> "low";
            case 1 -> "medium";
            case 2 -> "high";
            default -> throw new IllegalArgumentException("Unknown fixed cost index: " + fixedCostIndex);
        };
        String fixedCostFile = instanceFolder + "/fixedCost/" + fixedCostVariant + ".txt";
        instance.setFixedCostFile(fixedCostFile);

        // Solver settings.
        LinePlanningParameters.LINE_ACTIVATION_COST = Reader.readInt(fixedCostFile);
        solver.setUseStrongInequalities(useStrongInequalities);
        solver.setPairsPerGroup(pairsPerGroup);
        
        // Call solver.
        LinePlanningSolution solution = solver.solve(instance, linePool, cpuTimeSeconds);
        if (solution == null)
            solution = LinePlanningSolution.noSolution(instance);
        solution.evaluateTrueObjective();
        solution.printToConsole();
        solution.exportLinePlan(solver.getName(), cpuTimeSeconds, useStrongInequalities, pairsPerGroup);
    }
}
