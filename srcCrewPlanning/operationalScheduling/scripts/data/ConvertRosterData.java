package operationalScheduling.scripts.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import data.read.StationReader;
import modelling.Constants;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Template;
import operationalScheduling.RosterUtil;
import util.Configuration;
import util.Writer;

public class ConvertRosterData
{
	private static int CREW_ID = 1;
	private static int TEMPLATE_ID = 1;
	private static LocalDate START_DATE = LocalDate.parse(
			Configuration.getConfiguration().getStringProperty("START_DATE_SOLVER"));
	private static LocalDate END_DATE = LocalDate.parse(
			Configuration.getConfiguration().getStringProperty("END_DATE_SOLVER"));

	public static void main(String[] args) throws IOException
	{
		// Make a roster folder.
		String folderLocation = "roster";
		new File(folderLocation).mkdir();

		// Read in cyclic rosters of all guards.
		String dataFolder = Configuration.getConfiguration().getStringProperty("DATA_FOLDER");
		List<Station> stations = StationReader.readStations(dataFolder + "stations.txt");
		List<DutyRoster> cyclicRosters = readRosters("rostersCorrectBaseKey.csv", stations);

		// Convert to individual rosters.
		List<DutyRoster> individualRosters = new ArrayList<>();
		for (DutyRoster roster : cyclicRosters)
		{
			individualRosters.addAll(RosterUtil.createIndividualRosters(roster, START_DATE));
		}

		// Expand rosters when necessary.
		int minNumWeeks = (int) ChronoUnit.WEEKS.between(START_DATE, END_DATE) + 1;
		for (DutyRoster roster : individualRosters)
		{
			RosterUtil.setMinimumLength(roster, minNumWeeks, START_DATE);
		}

		// Shift templates in the roster.
		for (DutyRoster roster : individualRosters)
		{
			RosterUtil.expandTemplateWidth(roster, Constants.SECONDS_PER_HALF_HOUR);
		}

		// Remove all templates that fall outside the planning period.
		LocalDate startDate = Configuration.getConfiguration().getDateProperty("START_DATE_SOLVER");
		LocalDate endDate = Configuration.getConfiguration().getDateProperty("END_DATE_SOLVER");
		for (DutyRoster roster : individualRosters)
		{
			List<LocalDate> removeDays = new ArrayList<>();
			for (Template template : roster.getTemplates())
			{
				if (template.getDay().isBefore(startDate) || template.getDay().isAfter(endDate))
				{
					removeDays.add(template.getDay());
				}
			}
			for (LocalDate day : removeDays)
			{
				roster.removeTemplate(day);
			}
		}

		// Ensure unique indices.
		RosterUtil.ensureUniqueIndices(individualRosters);

		// Write out a list of crew members.
		StringBuilder sb = new StringBuilder();
		for (DutyRoster roster : individualRosters)
		{
			sb.append(roster.getCrewID() + " " + roster.getCrewBase().getIndex() + "\n");
		}
		Writer.write(sb.toString(), folderLocation + "/crew.txt");

		// Write out templates to folders.
		LocalDate date = START_DATE;
		while (!date.isAfter(END_DATE))
		{
			// Make a folder.
			String dayFolderLocation = folderLocation + "/" + date;
			new File(dayFolderLocation).mkdir();

			// Write templates to folder.
			sb = new StringBuilder();
			for (DutyRoster roster : individualRosters)
			{
				if (roster.hasTemplate(date))
				{
					sb.append(roster.getTemplate(date) + "\n");
				}
			}
			Writer.write(sb.toString(), dayFolderLocation + "/templates.txt");

			// Increment date.
			date = date.plusDays(1);
		}
	}

	public static List<DutyRoster> readRosters(String rosterFile, List<Station> stations) throws IOException
	{
		List<DutyRoster> rosters = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(rosterFile));

		// Skip first line.
		String line = reader.readLine();

		// Initialise variables.
		int rosterCode = -1;
		int week = 1;
		int weekInFile = -1;

		DutyRoster roster = null;
		while ((line = reader.readLine()) != null)
		{
			String name = line.split(";")[2].toLowerCase();
			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			Station crewBase = matchStation(name, stations);

			int nextRosterCode = Integer.valueOf(line.split(";")[3]);

			// Increase week if necessary.
			int nextWeekInFile = Integer.valueOf(line.split(";")[7]);
			if (rosterCode != nextRosterCode)
			{
				// Only add non-empty rosters. Rosters may be empty when the crew base is not
				// included in the list of stations.
				if (roster != null && roster.getTemplates().size() > 0)
				{
					// Correctly set the number of weeks this roster has.
					roster.setNumWeeks(week);

					// Add roster to list.
					rosters.add(roster);
				}

				// Initialise new roster and reset variables.
				roster = new DutyRoster(CREW_ID, crewBase);
				week = 1;
				weekInFile = nextWeekInFile;
				CREW_ID++;
			}
			else if (nextWeekInFile != weekInFile)
			{
				week++;
				weekInFile = nextWeekInFile;
			}

			Template template = readRosterLine(line, stations, week);
			if (template != null)
			{
				// Add template to roster.
				roster.addTemplate(template.getDay(), template);
			}
			rosterCode = nextRosterCode;
		}
		roster.setNumWeeks(week);
		rosters.add(roster);
		reader.close();
		return rosters;
	}

	private static Template readRosterLine(String data, List<Station> stations, int week)
	{
		String[] line = data.split(";");

		String name = line[2].toLowerCase();
		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		Station crewBase = matchStation(name, stations);
		if (crewBase == null)
		{
			return null;
		}

		int day = Integer.valueOf(line[6]);

		LocalDate date = START_DATE.plusWeeks(week - 1);
		date = date.plusDays(day - 1);

		// Check if the line contains a duty or not based on the start time.
		if (line[11].length() == 0)
		{
			return null;
		}

		// Filter out duties with numbers of 6XX or 1XXX.
		int dutyNumber = Integer.valueOf(line[9]);
		if (dutyNumber >= 1000 || (dutyNumber >= 600 && dutyNumber < 700))
		{
			return null;
		}

		// Read in start and end time.
		int startTime = Integer.valueOf(line[11]);
		int endTime = Integer.valueOf(line[12]);

		// Correct for end of day effect.
		if (endTime < startTime)
		{
			endTime += Constants.SECONDS_PER_DAY;
		}
		Template template = new Template(TEMPLATE_ID, CREW_ID, crewBase, date, startTime, endTime);
		TEMPLATE_ID++;
		return template;

	}

	private static Station matchStation(String name, List<Station> stations)
	{
		for (Station station : stations)
		{
			if (station.getName().equals(name))
			{
				return station;
			}
		}
		return null;
	}
}
