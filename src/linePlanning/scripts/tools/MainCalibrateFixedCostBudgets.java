package linePlanning.scripts.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.LinePlanningParameters;
import linePlanning.lineGeneration.Line;
import linePlanning.lineGeneration.LinePoolGenerator;
import linePlanning.solvers.LinePlanningSolution;
import linePlanning.solvers.PTNSolver;
import util.Configuration;

/**
 * Calibrates budget files for fixedCost levels low (0) and high (2).
 *
 * Runs the Karbstein solver (PTNSolver+K) on all Dutch instances with small
 * line/route generation and fixed cost = 1.0 (medium) — the level the existing
 * budget files were tuned for. For each instance x budget-variant combination,
 * the number of unique activated lines (nr_lines) is observed and used to
 * derive adjusted budget values:
 *
 *   budget_low  = budget_current - nr_lines   (fixedCost drops by 1 per line)
 *   budget_high = budget_current + nr_lines   (fixedCost rises by 1 per line)
 *
 * Results are written to:
 *   instances/{instance}/budget-fixedCost-low/{small|medium|large}.txt
 *   instances/{instance}/budget-fixedCost-high/{small|medium|large}.txt
 */
public class MainCalibrateFixedCostBudgets
{
    // Maps instance name to the OD matrix filename (without .txt) to use for calibration.
    static final Map<String, String> INSTANCES = Map.of(
        "Switzerland",   "0"
    );
    static final String[] BUDGET_VARIANTS = { "small", "medium", "large" };

    public static void main(String[] args) throws IOException, IloException
    {
        Configuration.initialiseConfiguration("settings.properties");

        String dataFolder = "instances";

        for (Map.Entry<String, String> entry : INSTANCES.entrySet())
        {
            String instanceName   = entry.getKey();
            String odMatrixVariant = entry.getValue();
            String instanceFolder = dataFolder + "/" + instanceName;

            for (String budgetVariant : BUDGET_VARIANTS)
            {
                System.out.printf("%n=== %s  budget=%s ===%n", instanceName, budgetVariant);

                String odMatrixFile       = instanceFolder + "/ODMatrix/" + odMatrixVariant + ".txt";
                String budgetFile         = instanceFolder + "/budget/" + budgetVariant + ".txt";
                String transferPenaltyFile = instanceFolder + "/transferPenalty/medium.txt";
                String lineGenerationFile  = instanceFolder + "/LineGeneration/small.txt";
                String routeGenerationFile = instanceFolder + "/RouteGeneration/small.txt";

                LinePlanningInstance instance = new LinePlanningInstance(
                        instanceFolder, odMatrixFile, budgetFile, transferPenaltyFile);
                instance.setLineGenerationFile(lineGenerationFile);
                instance.setRouteGenerationFile(routeGenerationFile);

                LinePoolGenerator lpGen = new LinePoolGenerator(instance);
                List<Line> linePool = lpGen.getLines();
                instance.setLinePool(linePool);

                LinePlanningParameters.LINE_ACTIVATION_COST = 1;

                PTNSolver solver = new PTNSolver(true); // true = Karbstein
                //solver.setUseStrongInequalities(true);

                LinePlanningSolution solution = solver.solve(instance, linePool, 300);

                int nrLines = solution.getLinePlan().size();
                int budgetCurrent = instance.getBudget();
                int budgetLow  = budgetCurrent - nrLines;
                int budgetHigh = budgetCurrent + nrLines;

                System.out.printf("Activated lines: %d  |  budget %d  ->  low=%d  high=%d%n",
                        nrLines, budgetCurrent, budgetLow, budgetHigh);

                writeBudget(instanceFolder + "/budget-fixedCost-low/"  + budgetVariant + ".txt", budgetLow);
                writeBudget(instanceFolder + "/budget-fixedCost-high/" + budgetVariant + ".txt", budgetHigh);
            }
        }

        System.out.println("\nDone. Budget files written to instances/*/budget-fixedCost-low/ and instances/*/budget-fixedCost-high/");
    }

    private static void writeBudget(String filePath, int value) throws IOException
    {
        Files.createDirectories(Path.of(filePath).getParent());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath)))
        {
            writer.write(Integer.toString(value));
            writer.newLine();
        }
    }
}
