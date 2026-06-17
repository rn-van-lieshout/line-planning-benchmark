package operationalScheduling.evaluation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import util.Configuration;

public class SolverEvaluation
{
	public static void main(String[] args) throws IOException
	{
		// Initialise a roster for each crew member.
		String resultsFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2\\analysis";

		File folder = new File(resultsFolder);
		String[] directories = folder.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File current, String name)
			{
				return new File(current, name).isDirectory();
			}
		});

		for (String directory : directories)
		{
			// Retrieve output folder.
			String outputFolder = resultsFolder + "\\" + directory + "\\";
			System.out.println(directory);

			// Set correct settings.
			String settingsFile = outputFolder + "settings.properties";
			String logFile = outputFolder + "log.csv";
			Configuration.initialiseConfiguration(settingsFile);

			// Computational performance.
			ComputationalModule.performComputationalAnalysis(logFile);
			System.out.println();
		}
	}
}
