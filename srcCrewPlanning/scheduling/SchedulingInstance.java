package scheduling;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import graph.structures.CleanPricingGraph;
import graph.structures.PricingGraph;
import modelling.Connection;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import scheduling.constraints.CoverConstraint;
import util.Writer;

public class SchedulingInstance extends AbstractInstance
{
	private List<Station> stations;
	private List<Station> crewBases;
	private List<LocalDate> days;
	private Map<LocalDate, List<Template>> templateMap;
	private Map<LocalDate, List<Task>> taskMap;
	private Map<LocalDate, List<Connection>> connectionMap;
	private List<DutyRoster> rosters;

	private List<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> constraints;
	private List<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> genericConstraints;
	private Map<LocalDate, Map<Task, CoverConstraint>> coverConstraintMap;
	private Map<LocalDate, Map<Template, List<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>>>> groupedConstraintMap;

	private Map<LocalDate, PricingGraph> dailyPricingGraphMap;

	public void prepareConstraintsAndPricingGraphs()
	{
		this.constraints = new ArrayList<>();
		this.genericConstraints = new ArrayList<>();
		this.coverConstraintMap = new LinkedHashMap<>();
		for (LocalDate day : days)
		{
			coverConstraintMap.put(day, new LinkedHashMap<>());
		}
		initialiseGroupedConstraintMap();
		initialisePricingGraphs();
	}

	public void setDailyPricingGraph(LocalDate date, PricingGraph graph)
	{
		dailyPricingGraphMap.put(date, graph);
	}

	public List<Station> getStations()
	{
		return stations;
	}

	public void setStations(List<Station> stations)
	{
		this.stations = stations;
	}

	public void setCrewBases(List<Station> crewBases)
	{
		this.crewBases = crewBases;
	}

	public void setTemplateMap(Map<LocalDate, List<Template>> templateMap)
	{
		this.templateMap = templateMap;
	}

	public Map<LocalDate, List<Template>> getTemplateMap()
	{
		return templateMap;
	}

	public void setDays(List<LocalDate> days)
	{
		this.days = days;
	}

	public void setTaskMap(Map<LocalDate, List<Task>> taskMap)
	{
		this.taskMap = taskMap;
	}

	public Map<LocalDate, List<Connection>> getConnectionMap()
	{
		return connectionMap;
	}

	public Map<LocalDate, List<Task>> getTaskMap()
	{
		return taskMap;
	}

	public void setConnectionMap(Map<LocalDate, List<Connection>> connectionMap)
	{
		this.connectionMap = connectionMap;
	}

	public void setRosters(List<DutyRoster> rosters)
	{
		if (rosters == null)
		{
			throw new IllegalArgumentException("Rosters have not been defined.");
		}
		this.rosters = rosters;
	}

	// TODO: Call this in the operational setting!
	public void constructTemplateMap()
	{
		this.templateMap = new LinkedHashMap<>();
		for (LocalDate day : days)
		{
			templateMap.put(day, new ArrayList<>());
			for (DutyRoster roster : rosters)
			{
				if (roster.hasTemplate(day))
				{
					templateMap.get(day).add(roster.getTemplate(day));
				}
			}
		}
	}

	private void initialisePricingGraphs()
	{
		this.dailyPricingGraphMap = new LinkedHashMap<>();
		for (LocalDate day : days)
		{
			Map<Integer, Task> indexTaskMap = new LinkedHashMap<>();
			for (Task task : taskMap.get(day))
			{
				indexTaskMap.put(task.getIndex(), task);
			}
			dailyPricingGraphMap.put(day, CleanPricingGraph.initialisePricingGraph(day, taskMap.get(day), indexTaskMap,
					connectionMap.get(day)));
		}
	}

	public List<Task> getTasks(LocalDate day)
	{
		return taskMap.get(day);
	}

	public List<Station> getCrewBases()
	{
		return Collections.unmodifiableList(crewBases);
	}

	public List<Connection> getConnections(LocalDate day)
	{
		return Collections.unmodifiableList(connectionMap.get(day));
	}

	public List<Template> getTemplates(LocalDate day)
	{
		return Collections.unmodifiableList(templateMap.get(day));
	}

	public List<LocalDate> getDays()
	{
		return Collections.unmodifiableList(days);
	}

	public List<DutyRoster> getRosters()
	{
		return Collections.unmodifiableList(rosters);
	}

	public List<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> getConstraints()
	{
		return Collections.unmodifiableList(constraints);
	}

	public CoverConstraint getCoverConstraint(Task task)
	{
		return coverConstraintMap.get(task.getDay()).get(task);
	}

	public Map<Task, CoverConstraint> getCoverConstraints(LocalDate day)
	{
		return Collections.unmodifiableMap(coverConstraintMap.get(day));
	}

	public PricingGraph getDailyPricingGraph(LocalDate day)
	{
		return dailyPricingGraphMap.get(day);
	}

	public List<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> getGenericConstraints()
	{
		return genericConstraints;
	}

	public void addGenericConstraint(
			AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint)
	{
		constraints.add(constraint);
		genericConstraints.add(constraint);
	}

	private void initialiseGroupedConstraintMap()
	{
		groupedConstraintMap = new LinkedHashMap<>();
		for (LocalDate day : days)
		{
			groupedConstraintMap.put(day, new LinkedHashMap<>());
			for (Template template : templateMap.get(day))
			{
				groupedConstraintMap.get(day).put(template, new ArrayList<>());
			}
		}
	}

	public void addConstraint(AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint)
	{
		constraints.add(constraint);
	}

	public void addCoverConstraint(LocalDate day, CoverConstraint constraint)
	{
		coverConstraintMap.get(day).put(constraint.getTask(), constraint);
	}

	public void placeInGroupedConstraintMap(LocalDate day, Template template,
			AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem> constraint)
	{
		// TODO: Can we do this more elegantly?
		if (!groupedConstraintMap.get(day).get(template).contains(constraint))
		{
			groupedConstraintMap.get(day).get(template).add(constraint);
		}
	}

	public List<AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>> getGroupedConstraints(
			LocalDate day, Template template)
	{
		if (!groupedConstraintMap.containsKey(day))
		{
			return new ArrayList<>();
		}
		if (!groupedConstraintMap.get(day).containsKey(template))
		{
			return new ArrayList<>();
		}
		return groupedConstraintMap.get(day).get(template);
	}

	public void writeInstance(String folder) throws IOException
	{
		// Make a folder.
		new File(folder).mkdir();

		// Write the stations and days to a .txt file.
		Writer.write(stations, folder + "/stations.txt");
		Writer.write(crewBases, folder + "/crewBases.txt");
		Writer.write(days, folder + "/days.txt");

		// For each day of the instance, write the tasks, connections, and templates to
		// a dedicated folder.
		for (LocalDate day : days)
		{
			String dayFolderLocation = folder + "/" + day;
			new File(dayFolderLocation).mkdir();

			Writer.write(taskMap.get(day), dayFolderLocation + "/tasks.txt");
			Writer.write(connectionMap.get(day), dayFolderLocation + "/connections.txt");
			Writer.write(templateMap.get(day), dayFolderLocation + "/templates.txt");
		}
	}
}
