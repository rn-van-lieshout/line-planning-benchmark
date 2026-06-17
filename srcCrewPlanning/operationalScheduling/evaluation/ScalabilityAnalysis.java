package operationalScheduling.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import data.crewsReaders.BlockReader;
import data.crewsReaders.PathReader;
import data.crewsReaders.StationReader;
import data.crewsReaders.TemplateReader;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import util.Configuration;

public class ScalabilityAnalysis
{
	public static void main(String[] args) throws IOException
	{
		// Initialise output string.
		StringBuilder sb = new StringBuilder();
		sb.append("date;tasks;timePricing;timeRMP;timeTotal;index;integrated\n");

		for (int index = 0; index < 14; index++)
		{
			// Read in log file.
			String outputFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2\\resultsScalability\\runs\\run_"
					+ index + "\\";
			System.out.println(outputFolder);

			// Set correct settings.
			String settingsFile = outputFolder + "settings.properties";
			String logFile = outputFolder + "log.csv";
			Configuration.initialiseConfiguration(settingsFile);

			// Read in subset of stations.
			Set<Station> crewBases = new LinkedHashSet<>();
			String decompositionGroup = Configuration.getConfiguration().getStringProperty("DECOMPOSITION_GROUP_" + 1);
			String[] values = decompositionGroup.split("-");
			for (int j = 0; j < values.length; j++)
			{
				String[] data = values[j].replace("(", "").replace(")", "").split(",");
				crewBases.add(new Station(Integer.valueOf(data[1]), data[0]));
			}

			// Read in computation times.
			int[] timePricing = new int[49];
			int[] timeRMP = new int[49];
			int[] timeTotal = new int[49];
			int currentNode = 0;

			BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
			bufferedReader.readLine();
			String line = bufferedReader.readLine();

			int d = 0;
			while (line != null)
			{
				String[] data = line.split(";");
				int node = Integer.valueOf(data[0]);

				// Write time if the node index switches to 0.
				if (currentNode > 0 && node == 0)
				{
					d++;
				}
				currentNode = node;

				timePricing[d] += Integer.valueOf(data[2]);
				timeRMP[d] += Integer.valueOf(data[3]);
				timeTotal[d] += Integer.valueOf(data[2]) + Integer.valueOf(data[3]) + Integer.valueOf(data[4])
						+ Integer.valueOf(data[5]) + Integer.valueOf(data[6]);

				// Read next line.
				line = bufferedReader.readLine();
			}
			bufferedReader.close();

			// Location of files.
			int tempWeek = 40;
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

			String relativePath = tempWeek + "-2021-v9-zat-zon\\Week" + tempWeek + "-dag" + 1 + "-alles\\";
			String fullPath = sourceFolder + relativePath;

			// Read stations.
			List<Station> stations = StationReader.readStations(fullPath + "configuration.data");

			// Read daily data.
			d = 0;
			LocalDate date = LocalDate.parse("2021-10-04");
			for (int week : weeks)
			{
				for (int day : days)
				{
					int numTasks = 0;

					relativePath = week + "-2021-v9-zat-zon\\Week" + week + "-dag" + day + "-alles\\";
					fullPath = sourceFolder + relativePath;

					// Read data.
					List<Template> templates = TemplateReader.readTemplates(fullPath + "templates.data", stations,
							date);
					List<Task> tasks = BlockReader.readTasks(fullPath + "debug-blocks.data",
							fullPath + "initial-blocks.data",
							stations, templates, date);
					List<Duty> duties = PathReader.readDuties(fullPath + "initial-paths.data", templates, tasks);

					// Count tasks in this instance.
					for (Duty duty : duties)
					{
						Station crewBase = duty.getTemplate().getCrewBase();
						for (Task task : duty.getTasks())
						{
							if (task.cover() && crewBases.contains(crewBase))
							{
								numTasks++;
							}
						}
					}

					// Add to output.
					sb.append(date + ";" + numTasks + ";" + timePricing[d] + ";" + timeRMP[d] + ";" + timeTotal[d] + ";"
							+ index + ";" + (index % 2 == 0) + "\n");
					date = date.plusDays(1);
					d++;
				}
			}
		}
		System.out.println(sb.toString());
	}
}
