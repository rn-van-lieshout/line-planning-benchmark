package data.read;

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
		String line = bufferedReader.readLine();
		while (line != null)
		{
			stations.add(createStation(line));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();

		return stations;
	}

	public static Station createStation(String line)
	{
		String[] s = line.split("\\s+");
		int index = Integer.valueOf(s[0]);
		String name = s[1];
		return new Station(index, name);
	}
}
