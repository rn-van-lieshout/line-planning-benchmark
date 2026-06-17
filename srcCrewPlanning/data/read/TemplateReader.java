package data.read;

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
	public static List<Template> readTemplates(String file, List<Station> stations) throws IOException
	{
		List<Template> templates = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			templates.add(createTemplate(line, stations));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return templates;
	}

	public static Template createTemplate(String line, List<Station> stations)
	{
		String[] values = line.split("\\s+");
		int index = Integer.valueOf(values[0]);
		int crewID = Integer.valueOf(values[1]);

		int crewBaseIndex = Integer.valueOf(values[2]);
		Station crewBase = null;
		for (Station station : stations)
		{
			if (station.getIndex() == crewBaseIndex)
			{
				crewBase = station;
				break;
			}
		}

		LocalDate day = LocalDate.parse(values[3]);
		int startTime = Integer.valueOf(values[4]);	
		if (values.length == 8)
		{
			int latestStartTime = Integer.valueOf(values[5]);
			int endTime = Integer.valueOf(values[6]);
			int earliestEndTime = Integer.valueOf(values[7]);
			return new Template(index, crewID, crewBase, day, startTime, latestStartTime, endTime, earliestEndTime);
		}
		else
		{
			int endTime = Integer.valueOf(values[5]);
			return new Template(index, crewID, crewBase, day, startTime, endTime);
		}
	}
}
