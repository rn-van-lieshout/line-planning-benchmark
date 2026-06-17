package synthetic.visualiser;

import synthetic.RandomInstanceGenerator;
import util.Util;

public class GenerateInstance
{
	public static void main(String[] args)
	{
		// Parameters.
		int[] numCrewBases =
		{ 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		int[] factorSmallStations =
		{ 0, 1 };
		double[] lineCapacityFactor = {0.75, 1.0, 1.25};
		double[] distanceFactor =
		{ 0.75, 1.0, 1.25 };
		long[] seeds =
		{ 2131902, 192039102 };
		int[] sizes =
		{ 19, 2, 3, 2 };

//		int index = Integer.valueOf(args[0]);
		int index = 90;
		int[] settings = Util.retrieveSettings(sizes, index);
		int largeStations = numCrewBases[settings[0]];
		int smallStations = factorSmallStations[settings[1]] * largeStations;
//		RandomInstanceGenerator.multiplyDistances(distanceFactor[settings[2]]);
		RandomInstanceGenerator.multiplyDistances(1);
		RandomInstanceGenerator.scaleLineCapacity(0.75);
		long seed = seeds[settings[3]];
		largeStations = 6;
		smallStations = 6;
		int numBases = largeStations;

		// Generate instance.
		RandomInstanceGenerator.generateRandomNetwork(largeStations, smallStations, seed);
		RandomInstanceGenerator.constructLinePool();
		RandomInstanceGenerator.constructChangeAndGoNetwork();
		RandomInstanceGenerator.generateODPairs();
		RandomInstanceGenerator.generatePassengerPaths();
		RandomInstanceGenerator.generateLinePlan();
		RandomInstanceGenerator.computeTimeTable();
		RandomInstanceGenerator.assingRollingStockToLines();
		RandomInstanceGenerator.retrieveTasks();
		RandomInstanceGenerator.limitCrewBases(numBases);
		RandomInstanceGenerator.initialiseTaskNetwork();
		RandomInstanceGenerator.filterByColumnGeneration();

		// Write to folder.
		int numTasks = RandomInstanceGenerator.tasks.size();
		String folder = "l" + largeStations + "_s" + smallStations + "_t" + numTasks;
		RandomInstanceGenerator.writeInstance(folder);
	}
}
