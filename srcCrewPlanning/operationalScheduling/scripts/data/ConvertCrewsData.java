package operationalScheduling.scripts.data;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import data.convertData.ConvertData;
import data.crewsReaders.StationReader;
import modelling.Station;
import util.Writer;

public class ConvertCrewsData
{
	public static void main(String[] args) throws IOException
	{
		// Number of days to include.
		int numDays = 49;

		// Location of files.
		int tempWeek = 40;
		int tempDay = 1;
		String sourceFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\Data NS\\CREWS Oktober 2021\\CREWS_2021_40-46\\Bart van Rossum\\Wilmet-DAGPLAN-week-";
		List<Integer> days = new ArrayList<>();
		for (int i = 1; i <= 7; i++)
		{
			days.add(i);
		}

		List<Integer> weeks = new ArrayList<>();
		for (int i = 40; i <= 46; i++)
		{
			weeks.add(i);
		}

		String relativePath = tempWeek + "-2021-v9-zat-zon\\Week" + tempWeek + "-dag" + tempDay + "-alles\\";
		String fullPath = sourceFolder + relativePath;

		// Read stations.
		List<Station> stations = StationReader.readStations(fullPath + "configuration.data");
		for (Station station : stations)
		{
			System.out.println(station);
		}

		// Make a folder.
		String folderLocation = "testCosts";
		new File(folderLocation).mkdir();

		// Write the stations and days to a .txt file.
		Writer.write(stations, folderLocation + "/stations.txt");
		List<Integer> allDays = new ArrayList<>();
		for (int i = 1; i <= numDays; i++)
		{
			allDays.add(i);
		}

		// Read data.
		LocalDate date = LocalDate.parse("2021-10-04");
		for (int week : weeks)
		{
			for (int day : days)
			{
				int weekDay = 7 * (week - 40) + day;
				System.out.println(weekDay);

				relativePath = week + "-2021-v9-zat-zon\\Week" + week + "-dag" + day + "-alles\\";
				fullPath = sourceFolder + relativePath;
				ConvertData.convertCrewsData(fullPath, folderLocation + "/", date);
				date = date.plusDays(1);
			}
		}
	}
}
