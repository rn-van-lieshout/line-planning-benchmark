package linePlanning.data;

import java.io.IOException;

/**
 * Regenerates the OD matrices for Switzerland and SmallSwitzerland
 * from the population/gravity model in PrepareSwissInstance.
 *
 * Writes directly to:
 *   instances/Switzerland/ODMatrix/0.txt
 *   instances/SmallSwitzerland/ODMatrix/0.txt
 */
public class GenerateSwissODMatrices
{
    public static void main(String[] args) throws IOException
    {
        //generate(false);
        generate(true);
    }

    private static void generate(boolean small) throws IOException
    {
        String stationsFile = small ? "dataSwiss/stationsWithPopulationsSmall.txt"
                                    : "dataSwiss/stationsWithPopulations.txt";
        String outputFile   = small ? "instances/SmallSwitzerland/ODMatrix/0.txt"
                                    : "instances/Switzerland/ODMatrix/0.txt";

        PrepareSwissInstance.generateODPairs(stationsFile, outputFile);
    }
}
