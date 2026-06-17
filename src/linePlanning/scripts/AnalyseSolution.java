package linePlanning.scripts;

import java.io.IOException;

import ilog.concert.IloException;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.solvers.LinePlanningSolution;
import linePlanning.solvers.LinePlanningSolutionReader;
import util.Configuration;

public class AnalyseSolution
{
    public static void main(String[] args) throws IOException, IloException
    {
        String dataFolder = "instances";

        // 0 = Randstad-IC, 1 = Randstad-IC-SPR, 2 = Nederland-IC, 3 = Nederland-IC-SPR, 4 = SmallSwitzerland, 5 = Switzerland
        int instanceIndex = 0;
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
        int odMatrixIndex = 6;
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
        int budgetIndex = 1;
        String budgetVariant = switch (budgetIndex) {
            case 0 -> "small";
            case 1 -> "medium";
            case 2 -> "large";
            default -> throw new IllegalArgumentException("Unknown budget index: " + budgetIndex);
        };
        String budgetFile = instanceFolder + "/budget/" + budgetVariant + ".txt";

        String solutionFile = instanceFolder + "/solutions/test.txt";

        Configuration.initialiseConfiguration("settings.properties");

        LinePlanningInstance instance = new LinePlanningInstance(instanceFolder, odMatrixFile, budgetFile);

        LinePlanningSolution solution = LinePlanningSolutionReader.read(solutionFile, instance);
        solution.evaluateTrueObjective();
        solution.printToConsole();
    }
}
