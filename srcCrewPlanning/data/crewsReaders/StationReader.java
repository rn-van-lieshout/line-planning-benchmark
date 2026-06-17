package data.crewsReaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import modelling.Station;

public class StationReader
{
	public static List<Station> readStations(String file) throws IOException
	{
		List<Station> stations = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

		// Skip lines until "BASESTATIONS" is reached.
		String line = bufferedReader.readLine();
		while (!line.trim().equals("BASESTATIONS"))
		{
			line = bufferedReader.readLine();
		}

		// Read in the templates.
		int numStations = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numStations; i++)
		{
			String data = bufferedReader.readLine();
			stations.add(readStation(data));
		}

		bufferedReader.close();
		return stations;
	}

	private static Station readStation(String data)
	{
		String[] s = data.split("\\s+");
		int index = Integer.valueOf(s[1]);
		String name = s[0].split("\\.")[0];
		return new Station(index, name);
	}
}
