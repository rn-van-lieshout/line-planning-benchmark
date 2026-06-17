package synthetic.visualiser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import data.read.ConnectionReader;
import data.read.StationReader;
import data.read.TaskReader;
import graph.structures.PricingGraph;
import modelling.Connection;
import modelling.Station;
import modelling.Task;
import util.Writer;

public class ComputeStatistics
{
	public static void main(String[] args) throws IOException
	{
		Path folder = Paths.get("instances");

		StringBuilder sb = new StringBuilder();
		sb.append("name;crewBases;tasks;connections;duration;\n");

		for (Path entry : Files.newDirectoryStream(folder))
		{
			if (Files.isDirectory(entry))
			{ // Check if it's a directory
				System.out.println("Folder: " + entry.getFileName());

				// Read in data.
				List<Station> crewBases = StationReader.readStations(
						folder + "/" + entry.getFileName() + "/stations.txt");
				List<Task> tasks = TaskReader.readTasks(folder + "/" + entry.getFileName() + "/tasks.txt", crewBases);
				List<Connection> connections = ConnectionReader.readConnections(
						folder + "/" + entry.getFileName() + "/connections.txt");

				int numCrewBases = crewBases.size();
				int numTasks = tasks.size();
				int totalDuration = 0;
				for (Task task : tasks)
				{
					totalDuration += task.getDuration();
				}
				double averageDuration = (double) totalDuration / numTasks;
				int numArcs = connections.size();
				double connectivity = 2.0 * (double) numArcs / (numTasks * (numTasks - 1));
				sb.append(entry.getFileName() + ";" + numCrewBases + ";" + numTasks + ";"
						+ Writer.formatDouble(averageDuration) + ";" + numArcs + ";" + Writer.formatDouble(connectivity)
						+ "\n");
			}
		}
		System.out.println(sb.toString());
	}

	public static void analyseTaskDistribution(PricingGraph graph)
	{
		// Initialise results.
		StringBuilder sb = new StringBuilder();
		sb.append("index;startStation;endStation;startTime;endTime;duration;inArcs;outArcs\n");

		// Analyse tasks.
		for (Task task : graph.getNodes())
		{
			if (!task.cover())
			{
				continue;
			}
			int index = task.getIndex();
			int startStation = task.getStartStation().getIndex();
			int endStation = task.getEndStation().getIndex();
			int startTime = task.getStartTime();
			int endTime = task.getEndTime();
			int duration = task.getDuration();
			int inArcs = graph.getInDegree(task);
			int outArcs = graph.getOutDegree(task);
			sb.append(index + ";" + startStation + ";" + endStation + ";" + startTime + ";" + endTime + ";" + duration
					+ ";" + inArcs + ";" + outArcs + "\n");
		}

		// Print results.
		System.out.println(sb.toString());
	}
}
