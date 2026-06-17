package data.crewsReaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import modelling.Duty;
import modelling.Task;
import modelling.Template;

public class PathReader
{
	public static List<Duty> readDuties(String file, List<Template> templates, List<Task> tasks)
			throws IOException
	{
		List<Duty> duties = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

		// Skip lines until "PATHS" is reached.
		String line = bufferedReader.readLine();
		while (!line.trim().equals("PATHS"))
		{
			line = bufferedReader.readLine();
		}

		// Read in the duties.
		int numTemplates = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numTemplates; i++)
		{
			String[] data = new String[6];
			for (int j = 0; j < 6; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			duties.add(readDuty(data, templates, tasks));
		}

		bufferedReader.close();
		return duties;
	}

	private static Duty readDuty(String[] data, List<Template> templates, List<Task> tasks)
	{
		String[] line = data[0].split("\\s+");
		int index = Integer.valueOf(line[0]);
		Template template = templates.get(Integer.valueOf(line[5]) - 1);

		Set<Task> dutyTasks = new LinkedHashSet<>();
		line = data[1].split("\\s+");
		for (int i = 1; i < line.length; i++)
		{
			Task task = tasks.get(Integer.valueOf(line[i]) - 1);
			dutyTasks.add(task);
		}

		line = data[2].split("\\s+");
		int cost = Integer.valueOf(line[2]);

		// TODO: Temporarily use a crew ID of -1.
		// TODO: Temporarily use an empty list of connections.
		return new Duty(index, -1, template, template.getStartTime(), template.getEndTime(), cost,
				new ArrayList<>(), dutyTasks);
	}
}
