package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import modelling.Station;
import modelling.Task;

public class TaskReader
{
	public static List<Task> readTasks(String file, List<Station> stations) throws IOException
	{
		List<Task> tasks = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			tasks.add(createTask(line, stations));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return tasks;
	}

	public static Task createTask(String line, List<Station> stations)
	{
		String[] values = line.split("\\s+");

		int index = Integer.valueOf(values[0]);
		boolean cover = (values[1].equals("true"));
		LocalDate day = LocalDate.parse(values[2]);
		int startTime = Integer.valueOf(values[3]);
		int endTime = Integer.valueOf(values[4]);

		int startStationIndex = Integer.valueOf(values[5]);
		int endStationIndex = Integer.valueOf(values[6]);
		Station startStation = new Station(startStationIndex, "-1"), endStation = new Station(endStationIndex, "-1");
		for (Station station : stations)
		{
			if (station.getIndex() == startStationIndex)
			{
				startStation = station;
			}
			if (station.getIndex() == endStationIndex)
			{
				endStation = station;
			}
		}

		int refWorkingTime = Integer.valueOf(values[7]);
		int typeATime = Integer.valueOf(values[8]);
		int aggressionTime = Integer.valueOf(values[9]);
		int doubleDeckTime = Integer.valueOf(values[10]);

		// TODO: Include compatible stations.
		return new Task(index, cover, day, startTime, endTime, startStation, endStation, refWorkingTime, typeATime,
				aggressionTime, doubleDeckTime);
	}
}
