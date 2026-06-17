package operationalScheduling.evaluation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import data.read.ConnectionReader;
import data.read.CrewReader;
import data.read.DutyReader;
import data.read.StationReader;
import data.read.TaskReader;
import data.read.TemplateReader;
import modelling.Connection;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import operationalScheduling.RosterUtil;
import operationalScheduling.evaluation.modules.GlobalCoveredTasksModule;
import operationalScheduling.evaluation.modules.RosterDistributionModule;
import operationalScheduling.evaluation.modules.TemporalEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.Aggression;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import operationalScheduling.sweetAndSour.attributes.DoubleDeck;
import operationalScheduling.sweetAndSour.attributes.DutyLength;
import operationalScheduling.sweetAndSour.attributes.TypeA;
import util.Configuration;
import util.MapUtil;

public class Evaluation
{
	public static void main(String[] args) throws IOException
	{
		// Initialise a roster for each crew member.
		String dataFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2\\data\\";
		String rosterFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2\\roster\\";
		String resultsFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2\\analysis";

		File folder = new File(resultsFolder);
		String[] directories = folder.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File current, String name)
			{
				return new File(current, name).isDirectory();
			}
		});

		for (String directory : directories)
		{
			// Retrieve output folder.
			String outputFolder = resultsFolder + "\\" + directory + "\\";
			System.out.println(directory);

			// Set correct settings.
			String settingsFile = outputFolder + "settings.properties";
			String logFile = outputFolder + "log.csv";
			Configuration.initialiseConfiguration(settingsFile);

			// Add attributes.
			AttributeVectorStorage.resetStorage();
			AttributeVectorStorage storage = AttributeVectorStorage.getAttributeVectorStorage();
			storage.addAttribute(new DutyLength());
			storage.addAttribute(new TypeA());
			storage.addAttribute(new Aggression());
			storage.addAttribute(new DoubleDeck());

			// Initialise daily evaluation modules.
			List<DailyEvaluationModule> dailyModules = new ArrayList<>();
//			dailyModules.add(new CoveredTasksModule());
			dailyModules.add(new TemporalEvaluationModule(storage));

			// Initialise global evaluation modules.
			List<GlobalEvaluationModule> globalModules = new ArrayList<>();
//			globalModules.add(new DutyDistributionModule());
			globalModules.add(new RosterDistributionModule());
//			globalModules.add(new InstanceModule());
			globalModules.add(new GlobalCoveredTasksModule());

			// Retrieve date settings.
			LocalDate startDateSolver = LocalDate.parse(
					Configuration.getConfiguration().getStringProperty("START_DATE_SOLVER"));
			LocalDate endDateSolver = LocalDate.parse(
					Configuration.getConfiguration().getStringProperty("END_DATE_SOLVER"));

			// Read in data.
			List<Station> stations = StationReader.readStations(dataFolder + "stations.txt");
			Map<Integer, Station> crewMap = CrewReader.readCrew(rosterFolder + "crew.txt", stations);

			List<DutyRoster> rosters = new ArrayList<>();
			for (Entry<Integer, Station> crewMember : crewMap.entrySet())
			{
				rosters.add(new DutyRoster(crewMember.getKey(), crewMember.getValue()));
			}

			// Initialise a task map.
			Map<LocalDate, Map<Integer, Task>> tasksPerDay = new LinkedHashMap<>();

			// Read in templates and duties for each day of the period.
			LocalDate date = startDateSolver;
			while (!date.isAfter(endDateSolver))
			{
				// Read in templates.
				List<Template> templates = TemplateReader.readTemplates(rosterFolder + date + "/templates.txt",
						stations);
				for (Template template : templates)
				{
					for (DutyRoster roster : rosters)
					{
						if (roster.getCrewID() != template.getCrewID())
						{
							continue;
						}
						roster.addTemplate(date, template);
						break;
					}
				}

				// Read in tasks, connections, and templates.
				List<Task> tasks = TaskReader.readTasks(dataFolder + date + "/tasks.txt", stations);
				tasksPerDay.put(date, MapUtil.createTaskMap(tasks));
				Map<Integer, Template> templateIndexMap = MapUtil.createTemplateMap(
						TemplateReader.readTemplates(rosterFolder + date + "/templates.txt",
								stations));
				Map<Integer, Connection> connectionIndexMap = MapUtil.createConnectionMap(
						ConnectionReader.readConnections(dataFolder + date + "/connections.txt"));

				// Read in duties.
				List<Duty> duties = DutyReader.readDuties(outputFolder + date + "/duties.txt", templateIndexMap,
						connectionIndexMap, tasksPerDay.get(date));

				// Place duties in rosters.
				RosterUtil.fixDuties(rosters, duties);

				// Perform all daily evaluations.
				for (DailyEvaluationModule module : dailyModules)
				{
					module.performDailyEvaluation(rosters, tasksPerDay, storage, date);
				}

				// Increment date.
				date = date.plusDays(1);
			}

			// Print all daily modules.
			for (DailyEvaluationModule module : dailyModules)
			{
				System.out.println(module.printEvaluation());
			}

			// Perform all global modules.
			for (GlobalEvaluationModule module : globalModules)
			{
				System.out.println(module.performFinalEvaluation(rosters, tasksPerDay, storage));
			}

			// Computational performance.
			ComputationalModule.performComputationalAnalysis(logFile);
			IntegralityGapModule.performComputationalAnalysis(logFile);
			System.out.println();
		}
	}
}
