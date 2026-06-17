package data.crewsReaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import modelling.Station;
import modelling.Template;

public class TemplateReader
{
	public static List<Template> readTemplates(String file, List<Station> stations, LocalDate day) throws IOException
	{
		List<Template> templates = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

		// Skip lines until "TEMPLATES" is reached.
		String line = bufferedReader.readLine();
		while (!line.trim().equals("TEMPLATES"))
		{
			line = bufferedReader.readLine();
		}

		// Read in the templates.
		int numTemplates = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numTemplates; i++)
		{
			String[] data = new String[8];
			for (int j = 0; j < 8; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			templates.add(readTemplate(data, stations, day));
		}

		bufferedReader.close();
		return templates;
	}

	private static Template readTemplate(String[] data, List<Station> stations, LocalDate day)
	{
		String[] line = data[0].split("\\s+");
		int index = Integer.valueOf(line[0]);

		Station crewBase = stations.get(Integer.valueOf(line[1].substring(2, line[1].length())));
//		int day = Integer.valueOf(line[3]);
		line = data[4].split("\\s+");
		int startTime = Integer.valueOf(line[2]);
		int endTime = Integer.valueOf(line[3]);

		// TODO: Temporarily use a crew ID of -1.
		return new Template(index, -1, crewBase, day, startTime, endTime);
	}
}
