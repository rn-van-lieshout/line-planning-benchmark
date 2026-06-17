package operationalScheduling.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IntegralityGapModule
{
	public static void performComputationalAnalysis(String logFile) throws IOException
	{
		List<Double> rootNode = new ArrayList<>();
		List<Double> integral = new ArrayList<>();

		int currentNode = 0;
		double currentObjective = Double.MAX_VALUE;

		BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
		bufferedReader.readLine();
		String line = bufferedReader.readLine();

		while (line != null)
		{
			String[] data = line.split(";");
			int node = Integer.valueOf(data[0]);
			double objective = Double.valueOf(data[13]);

			// Write LP if the node index equals 0 and the iteration switches to 0.
			if (currentNode == 0 && node > 0)
			{
				rootNode.add(currentObjective);
			}

			// Write integral solution is the node index switches to 0.
			if (currentNode > 0 && node == 0)
			{
				integral.add(currentObjective);
			}

			// Update values.
			currentNode = node;
			currentObjective = objective;
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		// Write the last integral objective.
		if (integral.size() == rootNode.size() - 1)
		{
			integral.add(currentObjective);
		}

		// Print results.
		System.out.println("ROOTNODE AND INTEGRAL OBJECTIVE PER DAY:");
		{
			for (int i = 0; i < rootNode.size(); i++)
			{
				System.out.println(rootNode.get(i) + " " + integral.get(i));
			}
		}
	}
}
