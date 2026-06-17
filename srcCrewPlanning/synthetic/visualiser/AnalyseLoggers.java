package synthetic.visualiser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import util.Writer;

public class AnalyseLoggers
{
	public static void main(String[] args) throws IOException
	{
		// Parameters.
		String folder = "resultsSyntheticLayeredBranchingFrequentDiving/";
		DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folder));
		List<Path> subfolders = new ArrayList<>();
		for (Path entry : stream)
		{
			subfolders.add(entry);
		}
		stream.close();
		subfolders.sort(Comparator.comparing(Path::getFileName));

		// Read all logger files.
		StringBuilder sb = new StringBuilder();
		sb.append("instance;crewBases;tasks;rootBound;lowerBound;upperBound\n");

		for (Path subfolder : subfolders)
		{
			// Initialise results.
			String instance = subfolder.toString().substring(folder.length());
			int numCrewBases = Integer.valueOf(instance.split("_")[1].substring(1));
			String tasks = instance.split("_")[2].substring(1);
			tasks = tasks.substring(0, tasks.length() - 4);
			int numTasks = Integer.valueOf(tasks);
			double rootBound = 0;
			double lowerBound = 0;
			double upperBound = 0;

			BufferedReader bufferedReader = new BufferedReader(new FileReader(subfolder.toString()));
			bufferedReader.readLine();
			String line = bufferedReader.readLine();

			while (line != null)
			{
				String[] data = line.split(";");
				if (rootBound == 0)
				{
					rootBound = Double.valueOf(data[16]);
				}
				lowerBound = Double.valueOf(data[16]);
				upperBound = Double.valueOf(data[17]);

				line = bufferedReader.readLine();
			}
			bufferedReader.close();

			// Return results.
			sb.append(instance + ";" + numCrewBases + ";" + numTasks + ";" + Writer.formatDouble(rootBound) + ";"
					+ Writer.formatDouble(lowerBound) + ";" + Writer.formatDouble(upperBound) + "\n");
		}
		Writer.write(sb.toString(), "analysis_logger.csv");
	}
}
