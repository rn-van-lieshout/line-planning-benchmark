package operationalScheduling.scripts.run;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import data.FilterUtil;
import data.read.ConnectionReader;
import data.read.CrewsDutyReader;
import data.read.StationReader;
import data.read.TaskReader;
import data.read.TemplateReader;
import ilog.concert.IloException;
import modelling.Connection;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import scheduling.InitialiseConstraints;
import scheduling.SchedulingInstance;
import scheduling.SchedulingSolution;
import util.Configuration;
import util.MapUtil;
import util.Writer;

public class InnerSolver
{
	public static Map<LocalDate, List<Duty>> applyInnerSolver(List<LocalDate> days, List<DutyRoster> rosters)
			throws IOException, IloException
	{
		// Initialise map of duties.
		Map<LocalDate, List<Duty>> dutyMap = new LinkedHashMap<>();
		for (LocalDate day : days)
		{
			dutyMap.put(day, new ArrayList<>());
		}

		// Read in data.
		String dataFolder = Configuration.getConfiguration().getStringProperty("DATA_FOLDER");
		List<Station> stations = StationReader.readStations(dataFolder + "stations.txt");
		Map<LocalDate, List<Task>> tasks = new LinkedHashMap<>();
		Map<LocalDate, List<Connection>> connections = new LinkedHashMap<>();
		Map<LocalDate, List<Template>> templates = new LinkedHashMap<>();
		for (LocalDate day : days)
		{
			tasks.put(day, TaskReader.readTasks(dataFolder + day + "/tasks.txt", stations));
			connections.put(day, ConnectionReader.readConnections(dataFolder + day + "/connections.txt"));
			templates.put(day, TemplateReader.readTemplates(dataFolder + day + "/templates.txt", stations));
		}

		// Decompose the network, and pass problem to optimiser.
		for (int i = 1; i <= Configuration.getConfiguration().getIntProperty("NUM_DECOMPOSITION_GROUPS"); i++)
		{
			// Retrieve crew bases in decomposition group.
			List<Station> crewBases = new ArrayList<>();
			String decompositionGroup = Configuration.getConfiguration().getStringProperty("DECOMPOSITION_GROUP_" + i);
			String[] values = decompositionGroup.split("-");
			for (int j = 0; j < values.length; j++)
			{
				String[] data = values[j].replace("(", "").replace(")", "").split(",");
				crewBases.add(new Station(Integer.valueOf(data[1]), data[0]));
			}

			// Filter rosters.
			List<DutyRoster> filteredRosters = FilterUtil.filterRosters(crewBases, rosters);
			Map<LocalDate, List<Task>> taskMap = new LinkedHashMap<>();
			Map<LocalDate, List<Connection>> connectionMap = new LinkedHashMap<>();

			for (LocalDate day : days)
			{
				// Place tasks, templates, and connections in map.
				Map<Integer, Task> taskIndexMap = MapUtil.createTaskMap(tasks.get(day));
				Map<Integer, Template> templateIndexMap = MapUtil.createTemplateMap(templates.get(day));

				// Read in duties.
				List<Duty> duties = CrewsDutyReader.readDuties(dataFolder + day + "/crews_duties.txt", templateIndexMap,
						taskIndexMap);

				// Filter duties.
				List<Duty> filteredDuties = FilterUtil.filterDuties(crewBases, duties);

				// Filter tasks.
				List<Task> filteredTasks = FilterUtil.retrieveTasks(filteredDuties);
				taskMap.put(day, filteredTasks);

				// Filter connections.
				List<Connection> filteredConnections = FilterUtil.filterConnections(filteredTasks,
						connections.get(day));
				connectionMap.put(day, filteredConnections);
			}

			// Initialise instance.
			SchedulingInstance instance = new SchedulingInstance();
			instance.setCrewBases(crewBases);
			instance.setDays(days);
			instance.setTaskMap(taskMap);
			instance.setConnectionMap(connectionMap);
			instance.setRosters(filteredRosters);
			instance.constructTemplateMap();
			// TODO: Always call this!
			instance.prepareConstraintsAndPricingGraphs();

			InitialiseConstraints.initialiseCoverConstraints(instance);
			InitialiseConstraints.initialiseTemplateAssignmentConstraints(instance);

			// TODO: Replace this property by USE_ROSTER_CONSTRAINTS?
			if (Configuration.getConfiguration().getBooleanProperty("USE_REST_TIME_CONSTRAINTS"))
			{
				InitialiseConstraints.initialiseCliqueConstraints(instance);
				InitialiseConstraints.initialiseEarlyRotationConstraints(instance);
				InitialiseConstraints.initialiseLateRotationConstraints(instance);
			}

			// Potentially add crew base level constraints.
			if (Configuration.getConfiguration().getBooleanProperty("USE_SHARING_SWEET_AND_SOUR_CONSTRAINTS"))
			{
				InitialiseConstraints.initialiseSharingSweetAndSourConstraints(instance);
			}

			// Fixing rule.
			List<LocalDate> fixingDays = new ArrayList<>();
			for (int t = 0; t < Configuration.getConfiguration().getIntProperty("NUM_DAYS_FIX"); t++)
			{
				fixingDays.add(instance.getDays().get(t));
			}

			// Pass these values to the optimiser and retrieve solution.
			SchedulingSolution solution = Optimiser.applyOptimiser(instance, fixingDays);

			// Retrieve duties from days to be fixed and add these to solution.
			Map<LocalDate, List<Duty>> duties = solution.retrieveDuties(fixingDays);
			for (LocalDate day : duties.keySet())
			{
				dutyMap.get(day).addAll(duties.get(day));
			}
		}

		// Write duties to specified output file.
		for (LocalDate day : dutyMap.keySet())
		{
			// Make a folder.
			String outputFolder = Configuration.getConfiguration().getStringProperty("OUTPUT_FOLDER") + "/" + day;
			new File(outputFolder).mkdir();

			// Write duties.
			Writer.write(dutyMap.get(day), outputFolder + "/duties.txt");
		}

		// Return list of duties to outer solver.
		return dutyMap;
	}
}
