package linePlanning.scripts.tools;

import java.io.IOException;

import linePlanning.instance.LinePlanningInstance;
import util.Configuration;

/**
 * Generates strategy-based OD matrices (1-false, 1-true, …, 3-true) for all
 * four Dutch instances by calling writeODMatrices on each.
 */
public class WriteNLODMatrices
{
    static final String[] INSTANCES = {
        "Randstad-IC",
        "Randstad-IC-SPR",
        "Nederland-IC-SPR",
        "Nederland-IC",
    };

    public static void main(String[] args) throws IOException
    {
        Configuration.initialiseConfiguration("settings.properties");

        for (String name : INSTANCES)
        {
            System.out.println("=== " + name + " ===");
            String instanceFolder = "instances/" + name;

            LinePlanningInstance instance = new LinePlanningInstance(
                    instanceFolder,
                    instanceFolder + "/ODMatrix/gravity.txt",
                    instanceFolder + "/budget/medium.txt",
                    instanceFolder + "/transferPenalty/medium.txt");

            instance.setRouteGenerationFile(instanceFolder + "/RouteGeneration/medium.txt");
            instance.writeODMatrices(instanceFolder);
        }

        System.out.println("Done.");
    }
}
