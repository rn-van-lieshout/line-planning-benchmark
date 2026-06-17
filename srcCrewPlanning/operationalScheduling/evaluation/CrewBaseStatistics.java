package operationalScheduling.evaluation;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import data.crewsReaders.BlockReader;
import data.crewsReaders.PathReader;
import data.crewsReaders.StationReader;
import data.crewsReaders.TemplateReader;
import data.read.CrewReader;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import modelling.Template;

public class CrewBaseStatistics
{
	public static void main(String[] args) throws IOException
	{
		// Number of days to include.
		int numDays = 49;

		// Location of files.
		int tempWeek = 40;
		int tempDay = 1;
		String sourceFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\Data NS\\CREWS Oktober 2021\\CREWS_2021_40-46\\Bart van Rossum\\Wilmet-DAGPLAN-week-";
		String rosterFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2\\roster\\";
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

		// Write the stations and days to a .txt file.
		List<Integer> allDays = new ArrayList<>();
		for (int i = 1; i <= numDays; i++)
		{
			allDays.add(i);
		}

		// Initialise statistics per crew base.
		Map<Station, Integer> numDuties = new LinkedHashMap<>();
		Map<Station, Integer> numTemplates = new LinkedHashMap<>();
		Map<Station, Integer> numRosters = new LinkedHashMap<>();
		Map<Station, Integer> numTasks = new LinkedHashMap<>();
		Map<Station, Integer> workedTime = new LinkedHashMap<>();
		Map<Station, Integer> typeA = new LinkedHashMap<>();
		Map<Station, Integer> aggression = new LinkedHashMap<>();
		Map<Station, Integer> doubleDeck = new LinkedHashMap<>();
		for (Station station : stations)
		{
			numDuties.put(station, 0);
			numTemplates.put(station, 0);
			numRosters.put(station, 0);
			numTasks.put(station, 0);

			workedTime.put(station, 0);
			typeA.put(station, 0);
			aggression.put(station, 0);
			doubleDeck.put(station, 0);
		}

		// Read rosters.
		Map<Integer, Station> crewMap = CrewReader.readCrew(rosterFolder + "crew.txt", stations);
		for (Integer index : crewMap.keySet())
		{
			Station station = crewMap.get(index);
			numRosters.put(station, numRosters.get(station) + 1);
		}

		// Read daily data.
		LocalDate date = LocalDate.parse("2021-10-04");
		for (int week : weeks)
		{
			for (int day : days)
			{
				relativePath = week + "-2021-v9-zat-zon\\Week" + week + "-dag" + day + "-alles\\";
				fullPath = sourceFolder + relativePath;

				// Read data.
				List<Template> templates = TemplateReader.readTemplates(fullPath + "templates.data", stations, date);
				List<Task> tasks = BlockReader.readTasks(fullPath + "debug-blocks.data",
						fullPath + "initial-blocks.data",
						stations, templates, date);
				List<Duty> duties = PathReader.readDuties(fullPath + "initial-paths.data", templates, tasks);

				// Aggregate statistics per crew base.
				for (Duty duty : duties)
				{
					Station crewBase = duty.getTemplate().getCrewBase();
					numDuties.put(crewBase, numDuties.get(crewBase) + 1);
					for (Task task : duty.getTasks())
					{
						if (task.cover())
						{
							numTasks.put(crewBase, numTasks.get(crewBase) + 1);
							workedTime.put(crewBase, workedTime.get(crewBase) + task.getRefWorkingTime());
							typeA.put(crewBase, typeA.get(crewBase) + task.getTypeATime());
							aggression.put(crewBase, aggression.get(crewBase) + task.getAggressionTime());
							doubleDeck.put(crewBase, doubleDeck.get(crewBase) + task.getDoubleDeckTime());
						}
					}

				}

				// Read in templates.
				List<Template> rosterTemplates = data.read.TemplateReader.readTemplates(
						rosterFolder + date + "/templates.txt", stations);
				for (Template template : rosterTemplates)
				{
					Station station = template.getCrewBase();
					numTemplates.put(station, numTemplates.get(station) + 1);
				}
				date = date.plusDays(1);
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("station;rosters;templates;duties;tasks;typeA;aggression;doubleDeck\n");

		for (Station station : stations)
		{
			double typeAScore = (double) typeA.get(station) / workedTime.get(station);
			double aggressionScore = (double) aggression.get(station) / workedTime.get(station);
			double doubleDeckScore = (double) doubleDeck.get(station) / workedTime.get(station);
			sb.append(station.getName() + ";" + numRosters.get(station) + ";" + numTemplates.get(station) + ";"
					+ numDuties.get(station) + ";" + numTasks.get(station) + ";" + typeAScore + ";" + aggressionScore
					+ ";" + doubleDeckScore + "\n");
		}
		System.out.println(sb.toString());
	}
}
