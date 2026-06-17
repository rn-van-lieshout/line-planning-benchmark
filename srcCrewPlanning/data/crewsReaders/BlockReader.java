package data.crewsReaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import modelling.Station;
import modelling.Task;
import modelling.Template;

public class BlockReader
{
	public static List<Task> readTasks(String debugBlocksFile, String initialBlocksFile, List<Station> stations,
			List<Template> templates, LocalDate day) throws IOException
	{
		List<Task> tasks = new ArrayList<>();
		BufferedReader initialReader = new BufferedReader(new FileReader(initialBlocksFile));
		BufferedReader debugReader = new BufferedReader(new FileReader(debugBlocksFile));

		// Skip lines until "ORIGINAL-BLOCKS" is reached.
		String line = initialReader.readLine();
		while (!line.trim().equals("ORIGINAL-BLOCKS"))
		{
			line = initialReader.readLine();
		}
		initialReader.readLine();

		debugReader.readLine();
		int numTasks = Integer.valueOf(debugReader.readLine());

		// Read in the tasks.
		for (int i = 0; i < numTasks; i++)
		{
			String debugData = debugReader.readLine();
			String[] data = new String[6];
			for (int j = 0; j < 6; j++)
			{
				data[j] = initialReader.readLine();
			}
			tasks.add(readTask(debugData, data, stations, templates, day));
		}

		initialReader.close();
		debugReader.close();
		return tasks;
	}

	private static Task readTask(String debugData, String[] data, List<Station> stations, List<Template> templates,
			LocalDate day)
	{
		String[] line = data[0].split("\\s+");
		int index = Integer.valueOf(line[0]);
//		int day = Integer.valueOf(line[1]);
		boolean cover = line[2].equals("1");

		line = data[2].split("\\s+");
		int startTime = Integer.valueOf(line[2]);
		int endTime = Integer.valueOf(line[3]);
		int refWorkingTime = Integer.valueOf(line[4]);
		int typeATime = Integer.valueOf(line[6]);
		int aggressionTime = Integer.valueOf(line[7]);
		int doubleDeckTime = Integer.valueOf(line[8]);

		// Retrieve start and end station. Initialise a new station with index -1 if
		// station is not a crewbase.
		line = debugData.split("\\s+");
		String startStationName = line[2].split("-")[0];
		String endStationName = line[2].split("-")[1];
		Station startStation = new Station(-1, startStationName), endStation = new Station(-1, endStationName);
		for (Station station : stations)
		{
			if (station.getName().equals(startStationName))
			{
				startStation = station;
			}
			if (station.getName().equals(endStationName))
			{
				endStation = station;
			}
		}

		return new Task(index, cover, day, startTime, endTime, startStation,
				endStation, refWorkingTime, typeATime, aggressionTime, doubleDeckTime);
	}
}
