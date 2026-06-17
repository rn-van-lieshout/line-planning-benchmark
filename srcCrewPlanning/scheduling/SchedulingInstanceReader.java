package scheduling;

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
import modelling.Connection;
import modelling.Constants;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import util.MapUtil;

public class SchedulingInstanceReader
{
	private List<LocalDate> days;
	private List<Station> stations;
	private List<Station> crewBases;
	private Map<LocalDate, List<Task>> taskMap;
	private Map<LocalDate, List<Connection>> connectionMap;
	private Map<LocalDate, List<Template>> templateMap;

	public SchedulingInstanceReader(List<Station> stations, List<Station> crewBases)
	{
		this.days = new ArrayList<>();
		this.stations = new ArrayList<>(stations);
		this.crewBases = new ArrayList<>(crewBases);
		this.taskMap = new LinkedHashMap<>();
		this.connectionMap = new LinkedHashMap<>();
		this.templateMap = new LinkedHashMap<>();
	}

	/**
	 * Extend the scheduling instance by one day.
	 */
	public void addDay(LocalDate day, String taskFile, String connectionFile, String templateFile) throws IOException
	{
		// Add day.
		days.add(day);

		// Read in crews data.
		List<Task> tasks = TaskReader.readTasks(taskFile, stations);
		List<Connection> connections = ConnectionReader.readConnections(connectionFile);
		List<Template> templates = TemplateReader.readTemplates(templateFile, stations);

		taskMap.put(day, tasks);
		connectionMap.put(day, connections);
		templateMap.put(day, templates);

		// Print some statistics.
		System.out.println("#Tasks: " + tasks.size() + ". #Connections: " + connections.size());
	}

	/**
	 * Extend the scheduling instance by one day. We automatically filter all tasks
	 * to duties that are covered by crew bases in the list of crew bases.
	 */
	public void addDay(LocalDate day, String taskFile, String connectionFile, String templateFile, String dutyFile,
			List<Template> dailyTemplates) throws IOException
	{
		// Add day.
		days.add(day);

		// Read in crews data.
		List<Task> tasks = TaskReader.readTasks(taskFile, stations);
		List<Connection> connections = ConnectionReader.readConnections(connectionFile);
		List<Template> templates = TemplateReader.readTemplates(templateFile, stations);

		// Place tasks, templates, and connections in map.
		Map<Integer, Task> taskIndexMap = MapUtil.createTaskMap(tasks);
		Map<Integer, Template> templateIndexMap = MapUtil.createTemplateMap(templates);

		// Read in duties.
		List<Duty> duties = CrewsDutyReader.readDuties(dutyFile, templateIndexMap, taskIndexMap);

		// Filter duties.
		List<Duty> filteredDuties = FilterUtil.filterDuties(crewBases, duties);

		// Filter tasks.
		List<Task> filteredTasks = FilterUtil.retrieveTasks(filteredDuties);
		taskMap.put(day, filteredTasks);

		// Filter connections.
		List<Connection> filteredConnections = FilterUtil.filterConnections(filteredTasks, connections);
		connectionMap.put(day, filteredConnections);

		// Add templates to map.
		templateMap.put(day, dailyTemplates);

		// Print some statistics.
		System.out.println("#Tasks: " + filteredTasks.size() + ". #Connections: " + filteredConnections.size());
	}

	public SchedulingInstance getSchedulingInstance()
	{
		// Initialise instance.
		SchedulingInstance instance = new SchedulingInstance();
		instance.setCrewBases(crewBases);
		instance.setDays(days);
		instance.setTaskMap(taskMap);
		instance.setConnectionMap(connectionMap);
		instance.setTemplateMap(templateMap);
		instance.prepareConstraintsAndPricingGraphs();

		// Return instance.
		return instance;
	}

	public static SchedulingInstance readFolder(LocalDate day, String folder) throws IOException
	{
		// Initialise values.
		List<LocalDate> days = new ArrayList<>();
		List<Station> stations = StationReader.readStations(folder + "/stations.txt");
		List<Station> crewBases = StationReader.readStations(folder + "/crewBases.txt");
		Map<LocalDate, List<Task>> taskMap = new LinkedHashMap<>();
		Map<LocalDate, List<Connection>> connectionMap = new LinkedHashMap<>();
		Map<LocalDate, List<Template>> templateMap = new LinkedHashMap<>();

		// Add day.
		days.add(day);

		// Read in crews data.
		List<Task> tasks = TaskReader.readTasks(folder + "/tasks.txt", stations);
		List<Connection> connections = ConnectionReader.readConnections(folder + "/connections.txt");

		// Construct templates.
		List<Template> templates = new ArrayList<>();
		int templateIndex = 0;
		int startTime = 4 * Constants.SECONDS_PER_HOUR;
		int latestStartTime = 28 * Constants.SECONDS_PER_HOUR;
		int increment = Constants.SECONDS_PER_HOUR;
		while (startTime <= latestStartTime)
		{
			for (Station crewBase : crewBases)
			{
				int endTime = startTime + increment + Constants.MAXIMUM_DUTY_LENGTH;
				Template template = new Template(templateIndex, -1, crewBase, day, startTime, startTime + increment - 1,
						startTime, endTime);
				templates.add(template);
				templateIndex++;
			}
			startTime += increment;
		}

		taskMap.put(day, tasks);
		connectionMap.put(day, connections);
		templateMap.put(day, templates);

		// Initialise instance.
		SchedulingInstance instance = new SchedulingInstance();
		instance.setStations(stations);
		instance.setCrewBases(crewBases);
		instance.setDays(days);
		instance.setTaskMap(taskMap);
		instance.setConnectionMap(connectionMap);
		instance.setTemplateMap(templateMap);
		instance.prepareConstraintsAndPricingGraphs();

		// Return instance.
		return instance;
	}
}
