package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Station;

public class CapacityReader
{
	public static Map<Station, Integer> readCapacity(String file, List<Station> stations) throws IOException
	{
		Map<Station, Integer> capacityMap = new LinkedHashMap<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			String[] s = line.split("\\s+");
			int index = Integer.valueOf(s[0]);
			int capacity = Integer.valueOf(s[1]);
			Station crewBase = null;
			for (Station station : stations)
			{
				if (station.getIndex() == index)
				{
					crewBase = station;
					break;
				}
			}
			if (crewBase == null)
			{
				bufferedReader.close();
				throw new IllegalArgumentException("Unknown station: " + index);
			}
			capacityMap.put(crewBase, capacity);
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		return capacityMap;
	}
}
