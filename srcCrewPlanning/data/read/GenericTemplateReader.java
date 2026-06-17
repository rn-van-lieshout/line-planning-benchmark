package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import modelling.GenericTemplate;
import modelling.Station;

public class GenericTemplateReader
{
	public static List<GenericTemplate> readGenericTemplates(String file, List<Station> stations) throws IOException
	{
		List<GenericTemplate> genericTemplates = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			genericTemplates.add(createGenericTemplate(line, stations));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return genericTemplates;
	}

	public static GenericTemplate createGenericTemplate(String line, List<Station> stations)
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

		DayOfWeek dayOfWeek = DayOfWeek.valueOf(values[3]);
		int startTime = Integer.valueOf(values[4]);
		int endTime = Integer.valueOf(values[5]); 
		return new GenericTemplate(index, crewID, crewBase, dayOfWeek, startTime, endTime);
	}
}
