package operationalScheduling.scripts.data;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import data.crewsReaders.BlockReader;
import data.crewsReaders.PathReader;
import data.crewsReaders.StationReader;
import data.crewsReaders.TemplateReader;
import modelling.Constants;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import util.Writer;

public class ConvertCrewsDutiesToRosters
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
		String folderLocation = "singleTemplateRoster";
		new File(folderLocation).mkdir();

		List<Integer> allDays = new ArrayList<>();
		for (int i = 1; i <= numDays; i++)
		{
			allDays.add(i);
		}

		// Initiliase rosters.
		List<DutyRoster> rosters = new ArrayList<>();
		int crewID = 1;

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

				// Read data.
				List<Template> templates = TemplateReader.readTemplates(fullPath + "templates.data", stations, date);
				List<Task> tasks = BlockReader.readTasks(fullPath + "debug-blocks.data",
						fullPath + "initial-blocks.data", stations, templates, date);
				List<Duty> duties = PathReader.readDuties(fullPath + "initial-paths.data", templates, tasks);

				// Convert each duty to a separate duty roster.
				int minuteShift = 30;
				for (Duty duty : duties)
				{
					Template template = new Template(crewID, crewID, duty.getTemplate().getCrewBase(), date,
							duty.getStartTime() - minuteShift * Constants.SECONDS_PER_MINUTE,
							duty.getEndTime() + minuteShift * Constants.SECONDS_PER_MINUTE);
					DutyRoster roster = new DutyRoster(crewID, duty.getTemplate().getCrewBase());
					roster.addTemplate(date, template);
					rosters.add(roster);

					crewID++;
				}
				date = date.plusDays(1);
			}
		}

		// Write out a list of crew members.
		StringBuilder sb = new StringBuilder();
		for (DutyRoster roster : rosters)
		{
			sb.append(roster.getCrewID() + " " + roster.getCrewBase().getIndex() + "\n");
		}
		Writer.write(sb.toString(), folderLocation + "/crew.txt");

		// Write out templates to folders.
		date = LocalDate.parse("2021-10-04");
		for (int i = 0; i < numDays; i++)
		{
			// Make a folder.
			String dayFolderLocation = folderLocation + "/" + date;
			new File(dayFolderLocation).mkdir();

			// Write templates to folder.
			sb = new StringBuilder();
			for (DutyRoster roster : rosters)
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
}
