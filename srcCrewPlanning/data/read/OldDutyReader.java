package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modelling.Connection;
import modelling.Duty;
import modelling.Task;
import modelling.Template;

public class OldDutyReader
{
	public static List<Duty> readDuties(String file, Map<Integer, Template> templateMap,
			Map<Integer, Task> taskMap)
			throws IOException
	{
		List<Duty> duties = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			duties.add(createDuty(line, templateMap, taskMap));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return duties;
	}

	public static Duty createDuty(String line, Map<Integer, Template> templateMap,
			Map<Integer, Task> taskMap)
	{
		String[] values = line.split("\\s+");
		int index = Integer.valueOf(values[0]);
		int crewID = Integer.valueOf(values[1]);
		Template template = templateMap.get(Integer.valueOf(values[2]));
		int startTime = Integer.valueOf(values[3]);
		int endTime = Integer.valueOf(values[4]);
		int cost = Integer.valueOf(values[5]);

		List<Connection> connections = new ArrayList<>();
		Set<Task> tasks = new LinkedHashSet<>();
		for (int i = 6; i < values.length; i++)
		{
			int taskID = Integer.valueOf(values[i]);
			if (!taskMap.containsKey(taskID))
			{
				throw new IllegalArgumentException("Unknown task ID: " + taskID);
			}
			tasks.add(taskMap.get(taskID));
		}
		return new Duty(index, crewID, template, startTime, endTime, cost, connections, tasks);
	}
}
