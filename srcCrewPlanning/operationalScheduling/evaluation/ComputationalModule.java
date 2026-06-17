package operationalScheduling.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import util.Writer;

public class ComputationalModule
{
	public static void performComputationalAnalysis(String logFile) throws IOException
	{
		int numInstances = 0;
		long timePricing = 0;
		long timeRMP = 0;
		long timeCol = 0;
		long timeAssignment = 0;

		long numIter = 0;
		long numFixIter = 0;

		BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
		bufferedReader.readLine();
		String line = bufferedReader.readLine();

		while (line != null)
		{
			String[] data = line.split(";");
			if (Integer.valueOf(data[0]) == 0)
			{
				numInstances++;
			}
			timePricing += Long.valueOf(data[1]);
			timeRMP += Long.valueOf(data[2]);
			timeCol += Long.valueOf(data[3]);
			timeCol += Long.valueOf(data[4]);
			timeAssignment += Long.valueOf(data[5]);

			numIter++;
			if (Integer.valueOf(data[8]) == 0)
			{
				numFixIter++;
			}
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		
		// Convert to averages per day.
		long timeTotal = timePricing + timeRMP + timeCol + timeAssignment;
		System.out.println("AVERAGE COMPUTATION TIMES PER INSTANCE");
		System.out.println("TOTAL" + "\t" + "PRICING" + "\t" + "RMP"
				+ "\t" + "COLUMNS" + "\t" + "ASSIGNMENT" + "\t" + "ITER" + "\t" + "FIXING ITER" + "\t");
		System.out.println(Writer.formatDouble(timeTotal / (numInstances * 1000.0)) + "\t"
				+ Writer.formatDouble(timePricing / (numInstances * 1000.0))
				+ "\t" + Writer.formatDouble(timeRMP / (numInstances * 1000.0)) + "\t"
				+ Writer.formatDouble(timeCol / (numInstances * 1000.0)) + "\t"
				+ Writer.formatDouble(timeAssignment / (numInstances * 1000.0)) + "\t"
				+ Writer.formatDouble((double) numIter / numInstances)
				+ "\t" + Writer.formatDouble(numFixIter / numInstances));
	}
}
