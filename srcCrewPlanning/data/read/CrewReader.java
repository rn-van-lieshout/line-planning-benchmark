package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Station;

public class CrewReader
{
	public static Map<Integer, Station> readCrew(String file, List<Station> stations)
			throws IOException
	{
		Map<Integer, Station> crewMap = new LinkedHashMap<>();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			String[] values = line.split("\\s+");
			int crewID = Integer.valueOf(values[0]);
			int stationIndex = Integer.valueOf(values[1]);
			for (Station station : stations)
			{
				if (station.getIndex() == stationIndex)
				{
					crewMap.put(crewID, station);
					break;
				}
			}
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return crewMap;
	}
}
