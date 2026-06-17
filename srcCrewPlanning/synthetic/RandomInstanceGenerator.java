package synthetic;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import graph.algorithms.search.DFS;
import graph.algorithms.shortestPath.Dijkstra;
import graph.algorithms.shortestPath.arcWeightFunctions.DefaultArcWeightFunction;
import graph.algorithms.tree.Prim;
import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import linePlanning.LinePlanningMaster;
import linePlanning.LinePlanningSolution;
import linePlanning.columnGeneration.LineColumn;
import linePlanning.columnGeneration.PathColumn;
import linePlanning.constraints.LineSegmentCapacityConstraint;
import linePlanning.constraints.PathAssignmentConstraint;
import linePlanning.constraints.ReverseLineConstraint;
import linePlanning.data.GenerateCNGNetwork;
import linePlanning.instance.CGN;
import linePlanning.instance.CGNArc;
import linePlanning.instance.Line;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import linePlanning.instance.Path;
import linePlanning.instance.StationNode;
import linePlanning.instance.Track;
import linePlanning.instance.TravelArc;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Constants;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import modelling.comparators.TaskComparator;
import optimisation.columnGeneration.ColumnGeneration;
import scheduling.InitialiseConstraints;
import scheduling.SchedulingInstance;
import scheduling.SchedulingMaster;
import scheduling.SchedulingMasterBuilder;
import scheduling.SchedulingSolution;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.PricingRoutine;
import scheduling.columnGeneration.SchedulingColumn;
import scheduling.columnGeneration.TemplateDisjointSelector;
import scheduling.columnGeneration.labelling.InitialiseResourceTests;
import scheduling.columnGeneration.labelling.LabellingSolver;
import scheduling.constraints.CoverConstraint;
import synthetic.visualiser.LineIntersection;
import synthetic.visualiser.LineIntersection.Point;
import timeTabling.Evaluator;
import timeTabling.PESP;
import timeTabling.ScheduledLine;
import timeTabling.ScheduledRollingStockUnit;
import util.Configuration;
import util.Pair;
import util.Writer;

public class RandomInstanceGenerator
{
	// Parameters.
	private final static double MAX_WIDTH = 500;
	private final static double MAX_HEIGHT = 500;

	public static double MAX_STATION_DISTANCE = 50;
	public static double MAX_STATION_DISTANCE_SMALL = 15;
	public static double MIN_STATION_DISTANCE_SMALL = 5;
	public static double MIN_STATION_DISTANCE_LARGE = 10;
	public static double MAX_TRACK_DISTANCE = 60;

	public static double SMALL_CITY_LOWER_THRESHOLD = 0.05;
	public static double SMALL_CITY_UPPER_THRESHOLD = 0.25;
	public static double LARGE_CITY_LOWER_THRESHOLD = 0.5;
	public static double LARGE_CITY_UPPER_THRESHOLD = 1;
	public static double FRACTION_EXTRA_ARCS = 0.5;

	public static double DEMAND_LOWER_THRESHOLD = 0.0025;
	public static double MAX_DEMAND_TRAVEL_DISTANCE = 2 * 60;

	public static int DWELL_TIME = 2;
	public static int TRANSFER_TIME = 5;
	public static int TRANSFER_PENALTY = 10;
	public static int PERIOD_LENGTH = 60;
	public static int TIME_LIMIT = 60 * 60;

	public static int MAX_LINE_FREQUENCY = 6;
	public static double LINE_CAPACITY = 0.25;
	public static int MAX_LINE_DISTANCE = 2 * 60 + 30;
	public static int FIXED_LINE_COST = 30;
	public static double MAX_DETOUR_FRACTION_LINES = 2;
	public static int MAX_LINES_PER_PAIR = 2;
	public static int MAX_PATHS_PER_OD_PAIR = 100;
	public static double MAX_DETOUR_FRACTION_PATHS = 1.25;

	public static int EARLIEST_START_TIME = 6 * 60;
	public static int LATEST_START_TIME = 22 * 60;

	// Crew scheduling units (in seconds).
	public static int TIME_TO_DEPOT = 5 * Constants.SECONDS_PER_MINUTE;
	public static int MIN_TRANSFER_TIME = 10 * Constants.SECONDS_PER_MINUTE;
	public static int MAX_TRANSFER_TIME = 2 * Constants.SECONDS_PER_HOUR;
	public static int VARIABLE_COST = 1;
	public static int FIXED_COST = 8 * Constants.SECONDS_PER_HOUR;

	// Random infra network.
	public static Set<GeoStation> stations;
	public static Set<GeoStation> largeStations;
	public static DirectedGraph<StationNode, Track> infraNetwork;

	// Passenger demand.
	public static List<ODPair> ODPairs;

	// Line plan.
	public static Set<Line> linePool = new LinkedHashSet<>();
	public static LinePlanningInstance linePlanningInstance;
	public static LinePlanningSolution linePlan;
	public static Map<Line, Line> reverseLineMap;

	// Passenger flows.
	public static CGN changeAndGo;
	public static Set<Path> paths;

	// Timetable.
	public static List<ScheduledLine> scheduledLines;

	// Rolling stock.
	public static Set<ScheduledRollingStockUnit> scheduledRollingStock;

	// Crew schedule.
	public static Set<Task> tasks;
	public static Map<Task, ScheduledRollingStockUnit> rollingStockPerTaskMap;
	public static Set<Connection> connections;
	public static LocalDate date = LocalDate.parse("2025-02-04");

	public static void multiplyDistances(double factor)
	{
		MAX_STATION_DISTANCE *= factor;
		MAX_STATION_DISTANCE_SMALL *= factor;
		MIN_STATION_DISTANCE_SMALL *= factor;
		MIN_STATION_DISTANCE_LARGE *= factor;
		MAX_TRACK_DISTANCE *= factor;
	}

	public static void scaleLineCapacity(double factor)
	{
		LINE_CAPACITY *= factor;
	}

	public static void writeInstance(String folder)
	{
		try
		{
			new File(folder).mkdirs();

			// All large stations (before any limitCrewBases call).
			List<Station> allLargeStations = new ArrayList<Station>();
			for (GeoStation station : stations)
			{
				if (station.isLarge())
				{
					allLargeStations.add(new Station(station.getIndex(), station.getName()));
				}
			}

			// Crew bases (possibly a subset of large stations after limitCrewBases).
			List<Station> crewBases = new ArrayList<Station>();
			for (GeoStation station : largeStations)
			{
				crewBases.add(new Station(station.getIndex(), station.getName()));
			}

			// Write crew scheduling data.
			Writer.write(allLargeStations, folder + "/stations.txt");
			Writer.write(crewBases, folder + "/crewBases.txt");
			Writer.write(tasks, folder + "/tasks.txt");
			Writer.write(connections, folder + "/connections.txt");

			// Write infrastructure data.
			Writer.write(stations, folder + "/geoStations.txt");
			StringBuilder sb = new StringBuilder();
			for (DirectedGraphArc<StationNode, Track> arc : infraNetwork.getArcs())
			{
				sb.append(arc.getData().getFrom().getIndex() + " " + arc.getData().getTo().getIndex() + " "
						+ arc.getData().getDuration() + "\n");
			}
			Writer.write(sb.toString(), folder + "/tracks.txt");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void printStatistics(SchedulingInstance instance)
	{
		System.out.println("INSTANCE STATISTICS:");
		System.out.println("# Stations: " + instance.getStations().size());
		System.out.println("# Crewbases: " + instance.getCrewBases().size());
		int numTasks = instance.getTasks(date).size();
		int totalDuration = 0;
		for (Task task : instance.getTasks(date))
		{
			totalDuration += task.getDuration();
		}
		System.out.println("# Tasks: " + numTasks);
		System.out.println(
				"Avg. task duration (min): " + Writer.formatDouble(((double) totalDuration / (60 * numTasks))));
		int numArcs = instance.getConnections(date).size();
		System.out.println("# Connections: " + numArcs);
		double connectivity = (2.0 * (double) numArcs / (numTasks * (numTasks - 1)));
		System.out.println("Connectivity: " + Writer.formatDouble((connectivity)));
	}

	public static void initialiseTaskNetwork()
	{
		PricingGraph graph = new PricingGraph();
		connections = new LinkedHashSet<>();

		// Add a source and sink node.
		Task sourceTask = new Task(-2, false, date, 0, 0, new Station(-1, "source"), new Station(-1, "source"), 0, 0, 0,
				0);
		Task sinkTask = new Task(-1, false, date, 2 * Constants.SECONDS_PER_DAY, 2 * Constants.SECONDS_PER_DAY,
				new Station(-1, "sink"), new Station(-1, "sink"), 0, 0, 0, 0);
		graph.addNode(sourceTask);
		graph.addNode(sinkTask);

		List<Pair<StationNode, Integer>> tracksPerStation = new ArrayList<>();
		for (StationNode node : infraNetwork.getNodes())
		{
			tracksPerStation.add(new Pair<>(node, infraNetwork.getInDegree(node)));
		}
		tracksPerStation.sort((pair1, pair2) -> pair2.getValue().compareTo(pair1.getValue()));

		// Add all tasks.
		for (Task task : tasks)
		{
			graph.addNode(task);
		}
		graph.sortNodes(new TaskComparator());

		// Add all connections.
		for (int i = 0; i < graph.getNumberOfNodes(); i++)
		{
			Task from = graph.getNodes().get(i);
			for (int j = i + 1; j < graph.getNumberOfNodes(); j++)
			{
				Task to = graph.getNodes().get(j);
				boolean fromSource = from.equals(sourceTask);
				boolean toSink = to.equals(sinkTask);

				int index = connections.size();
				ConnectionType connectionType = null;
				int startBlock = 0;
				int endBlock = 0;

				int timeBeforeBreak = -1;
				int timeAfterBreak = -1;
				int duration = 0;
				int cost = 0;
				int refWorkingTime = 0;

				int typeATime = 0;
				int aggressionTime = 0;
				int doubleDeckTime = 0;
				List<Integer> passengerTasks = new ArrayList<>();

				if (fromSource && toSink)
				{
					continue;
				}
				if (fromSource)
				{
					// Only sign on from a crew base station.
					if (!largeStations.contains(to.getStartStation()))
					{
						continue;
					}
					connectionType = ConnectionType.SOURCE;
					startBlock = to.getStartStation().getIndex();
					endBlock = to.getIndex();

					duration = TIME_TO_DEPOT + to.getDuration();
					refWorkingTime += to.getDuration();
				}
				if (toSink)
				{
					// Only sign off at a crew base station.
					if (!largeStations.contains(from.getEndStation()))
					{
						continue;
					}
					connectionType = ConnectionType.SINK;
					startBlock = from.getIndex();
					endBlock = from.getEndStation().getIndex();

					duration = TIME_TO_DEPOT;
				}
				if (!fromSource && !toSink)
				{
					// Start and end station must match.
					if (!from.getEndStation().equals(to.getStartStation()))
					{
						continue;
					}
					int transferTime = to.getStartTime() - from.getEndTime();

					// Transfer time should be in order.
					if (transferTime > MAX_TRANSFER_TIME)
					{
						continue;
					}
					if (!rollingStockPerTaskMap.get(from).equals(rollingStockPerTaskMap.get(to)))
					{
						if (transferTime < MIN_TRANSFER_TIME)
						{
							continue;
						}
					}

					// Meal break.
					if (transferTime > MIN_TRANSFER_TIME + TIME_TO_DEPOT + Constants.MINIMUM_MEAL_BREAK_LENGTH)
					{
						timeBeforeBreak = TIME_TO_DEPOT;
						timeAfterBreak = transferTime - timeBeforeBreak - TIME_TO_DEPOT
								- Constants.MINIMUM_MEAL_BREAK_LENGTH;
					}

					connectionType = ConnectionType.CONNECTION;
					startBlock = from.getIndex();
					endBlock = to.getIndex();

					duration = to.getEndTime() - from.getEndTime();
					refWorkingTime += to.getDuration();
				}

				// We use variable costs equal to the duration, and a fixed cost per duty.
				cost = duration * VARIABLE_COST;
				cost += connectionType.equals(ConnectionType.SOURCE) ? FIXED_COST : 0;

				Connection connection = new Connection(index, connectionType, startBlock, endBlock, timeBeforeBreak,
						timeAfterBreak, duration, cost, refWorkingTime, typeATime, aggressionTime, doubleDeckTime,
						passengerTasks);
				connections.add(connection);
				graph.addArc(from, to, connection, cost);
			}
		}

		// Remove nodes with in- or out-degree of zero.
		System.out.println("Before removal: Tasks: " + tasks.size() + ". Arcs: " + graph.getNumberOfArcs());
		while (true)
		{
			Set<Task> remove = new LinkedHashSet<>();
			for (Task task : graph.getNodes())
			{
				if (task.equals(sinkTask) || task.equals(sourceTask))
				{
					continue;
				}
				if (graph.getInDegree(task) == 0 || graph.getOutDegree(task) == 0)
				{
					remove.add(task);
				}
			}
			for (Task task : remove)
			{
				tasks.remove(task);
				graph.removeNode(task);
			}
			if (remove.isEmpty())
			{
				break;
			}
		}
		System.out.println("After removal: Tasks: " + tasks.size() + ". Arcs: " + graph.getNumberOfArcs());
	}

	public static void limitCrewBases(int K)
	{
		// Rank large stations by total number of infrastructure connections.
		Map<GeoStation, Integer> degreeMap = new LinkedHashMap<>();
		for (StationNode node : infraNetwork.getNodes())
		{
			GeoStation station = (GeoStation) node.getStation();
			if (largeStations.contains(station))
			{
				degreeMap.put(station, infraNetwork.getInDegree(node) + infraNetwork.getOutDegree(node));
			}
		}
		List<GeoStation> sorted = new ArrayList<>(largeStations);
		sorted.sort((a, b) -> degreeMap.get(b) - degreeMap.get(a));

		largeStations = new LinkedHashSet<>();
		for (int i = 0; i < Math.min(K, sorted.size()); i++)
		{
			largeStations.add(sorted.get(i));
		}
		System.out.println("Crew bases limited to " + largeStations.size() + " stations.");
	}

	public static void filterByColumnGeneration()
	{
		try
		{
			Configuration.initialiseConfiguration("default.properties");

			// Build crew base list from restricted largeStations.
			List<Station> crewBases = new ArrayList<>();
			for (GeoStation s : largeStations)
			{
				crewBases.add(new Station(s.getIndex(), "station_" + s.getIndex()));
			}

			// Build templates: one per crew base per hour.
			List<Template> templates = new ArrayList<>();
			int templateIndex = 0;
			int startTime = 4 * Constants.SECONDS_PER_HOUR;
			int latestStartTime = 24 * Constants.SECONDS_PER_HOUR;
			int increment = Constants.SECONDS_PER_HOUR;
			while (startTime <= latestStartTime)
			{
				for (Station base : crewBases)
				{
					int endTime = startTime + increment + Constants.MAXIMUM_DUTY_LENGTH;
					templates.add(new Template(templateIndex++, -1, base, date, startTime, startTime + increment - 1,
							startTime, endTime));
				}
				startTime += increment;
			}

			// Build scheduling instance from in-memory data.
			List<LocalDate> days = new ArrayList<>();
			days.add(date);
			Map<LocalDate, List<Task>> taskMap = new LinkedHashMap<>();
			Map<LocalDate, List<Connection>> connectionMap = new LinkedHashMap<>();
			Map<LocalDate, List<Template>> templateMap = new LinkedHashMap<>();
			taskMap.put(date, new ArrayList<>(tasks));
			connectionMap.put(date, new ArrayList<>(connections));
			templateMap.put(date, templates);

			SchedulingInstance instance = new SchedulingInstance();
			instance.setCrewBases(crewBases);
			instance.setDays(days);
			instance.setTaskMap(taskMap);
			instance.setConnectionMap(connectionMap);
			instance.setTemplateMap(templateMap);
			instance.prepareConstraintsAndPricingGraphs();
			InitialiseConstraints.initialiseCoverConstraints(instance);

			// Run root-node column generation.
			SchedulingMaster master = new SchedulingMasterBuilder().setProblem(instance).build();
			List<PricingGraph> pricingGraphs = new ArrayList<>();
			pricingGraphs.add(instance.getDailyPricingGraph(date));
			LabellingSolver labelling = new LabellingSolver();
			labelling.addCompletionBound(InitialiseResourceTests.initialiseBreakResourceTest(pricingGraphs, crewBases));
			labelling
					.addCompletionBound(InitialiseResourceTests.initialiseLengthResourceTest(crewBases, pricingGraphs));
			ColumnGeneration<SchedulingInstance, SchedulingColumn, PricingProblem> columnGeneration = new ColumnGeneration<>(
					new PricingRoutine(), labelling, labelling, new TemplateDisjointSelector());
			columnGeneration.applyColumnGeneration(master, instance, 0);

			double numDuties = 0;
			double totalWorkload = 0;
			double totalWorkedTime = 0;
			SchedulingSolution solution = master.getSolution();
			for (Entry<SchedulingColumn, Double> entry : solution.getColumnMap().entrySet())
			{
				Duty duty = ((DutyColumn) entry.getKey()).getDuty();
				numDuties += entry.getValue();
				totalWorkload += entry.getValue() * duty.getWorkload();
				int workedTime = 0;
				for (Task task : duty.getTasks())
				{
					workedTime += task.getDuration();
				}
				totalWorkedTime += entry.getValue() * workedTime;
				System.out.println(
						"Workload: " + (duty.getWorkload() / 3600.0) + ". Worked time: " + (workedTime / 3600.0));
			}
			System.out.println("#Duties: " + numDuties + ". Avg. load: " + totalWorkload / (3600.0 * numDuties)
					+ ". Avg worked time: " + totalWorkedTime / (3600.0 * numDuties));

			// Remove tasks that cannot be covered (positive LP slack).
			Set<Task> infeasible = new LinkedHashSet<>();
			for (CoverConstraint c : instance.getCoverConstraints(date).values())
			{
				if (master.getSlackValue(c) > 0)
				{
					infeasible.add(c.getTask());
				}
			}
			System.out.println("Filtering " + infeasible.size() + " infeasible tasks.");
			tasks.removeAll(infeasible);

			// Rebuild connection network with the filtered task set.
			initialiseTaskNetwork();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void retrieveTasks()
	{
		// Note: We must convert units from minutes to seconds.
		tasks = new LinkedHashSet<>();
		rollingStockPerTaskMap = new LinkedHashMap<>();
		for (ScheduledRollingStockUnit rollingStock : scheduledRollingStock)
		{
			for (ScheduledLine scheduledLine : rollingStock.getScheduledLines())
			{
				// Aggregate to large stations! We can easily retrieve the time at which we
				// arrive at the next large station!
				Station startStation = null;
				int startTime = 0;
				for (Track track : scheduledLine.getLine().getLineSegments())
				{
					Station from = track.getFrom();
					Station to = track.getTo();
					if (largeStations.contains(from))
					{
						startStation = from;
						startTime = scheduledLine.getDepartureTime(track) * Constants.SECONDS_PER_MINUTE;
					}
					if (largeStations.contains(to))
					{
						// Add task.
						int endTime = scheduledLine.getArrivalTime(track) * Constants.SECONDS_PER_MINUTE;
						int duration = endTime - startTime;
						Task task = new Task(tasks.size(), true, date, startTime, endTime, startStation, to, duration,
								0, 0, 0);
						tasks.add(task);
						rollingStockPerTaskMap.put(task, rollingStock);
					}
				}
			}
		}
		System.out.println("#Tasks: " + tasks.size());
	}

	public static void assingRollingStockToLines()
	{
		scheduledRollingStock = new LinkedHashSet<>();
		List<ScheduledLine> unscheduledLines = new ArrayList<>(scheduledLines);
		while (!unscheduledLines.isEmpty())
		{
			int index = 0;
			ScheduledLine scheduledLine = unscheduledLines.get(index);
			Set<ScheduledLine> coveredLines = new LinkedHashSet<>();
			coveredLines.add(scheduledLine);
			Station startStation = scheduledLine.getLine().getStartStation();
			ScheduledRollingStockUnit rollingStock = new ScheduledRollingStockUnit(startStation);
			rollingStock.addScheduledLine(scheduledLine);

			while (true)
			{
				// Find a reverse line to add.
				Line reverseLine = reverseLineMap.get(scheduledLine.getLine());
				boolean foundLine = false;
				for (int i = index + 1; i < unscheduledLines.size(); i++)
				{
					ScheduledLine candidate = unscheduledLines.get(i);
					if (!candidate.getLine().equals(reverseLine))
					{
						continue;
					}
					if (candidate.getStartTime() < scheduledLine.getEndTime() + PESP.TurnAroundTime)
					{
						continue;
					}
					coveredLines.add(candidate);
					rollingStock.addScheduledLine(candidate);
					foundLine = true;
					index = i;
					scheduledLine = candidate;
				}
				if (!foundLine)
				{
					break;
				}
			}

			// Schedule extra copy of reverse of initial line to make sure rolling stock
			// returns to base.
			unscheduledLines.removeAll(coveredLines);
			if (!rollingStock.getLastStation().equals(rollingStock.getStartStation()))
			{
				Line reverseLine = reverseLineMap.get(rollingStock.getScheduledLines().get(0).getLine());
				int interval = PERIOD_LENGTH / linePlan.getLineFrequencyMap().get(reverseLine);
				ScheduledLine reverseScheduledLine = null;
				for (ScheduledLine candidate : scheduledLines)
				{
					if (candidate.getLine().equals(reverseLine))
					{
						reverseScheduledLine = candidate;
					}
				}
				int shift = 0;
				while (reverseScheduledLine.getStartTime() + shift < rollingStock.getEndTime() + PESP.TurnAroundTime)
				{
					shift += interval;
				}
				rollingStock.addScheduledLine(reverseScheduledLine.shiftScheduledLine(shift));
			}

			// Add rolling stock to schedule.
			scheduledRollingStock.add(rollingStock);
		}
	}

	public static List<ScheduledLine> extendScheduledLines(Set<ScheduledLine> scheduledLines)
	{
		List<ScheduledLine> extendedLines = new ArrayList<>();
		for (ScheduledLine scheduledLine : scheduledLines)
		{
			int startTime = scheduledLine.getStartTime();
			int shift = 0;
			int interval = PERIOD_LENGTH / linePlan.getLineFrequencyMap().get(scheduledLine.getLine());
			while (startTime + shift < EARLIEST_START_TIME)
			{
				shift += interval;
			}
			while (startTime + shift <= LATEST_START_TIME)
			{
				extendedLines.add(scheduledLine.shiftScheduledLine(shift));
				shift += interval;
			}
		}
		extendedLines.sort(Comparator.comparingInt(ScheduledLine::getStartTime));
		return extendedLines;
	}

	public static void computeTimeTable()
	{
		try
		{
			Evaluator evaluator = new Evaluator(linePlanningInstance, linePlan);
			evaluator.evaluate(TIME_LIMIT);
			Set<ScheduledLine> initialScheduledLines = evaluator.getScheduledLines();
			scheduledLines = extendScheduledLines(initialScheduledLines);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void generateLinePlan()
	{
		try
		{
			// Initialise settings.
			String settingsFile = "default.properties";
			Configuration.initialiseConfiguration(settingsFile);

			// Initialise model.
			linePlanningInstance = new LinePlanningInstance(infraNetwork, ODPairs, linePool, changeAndGo, PERIOD_LENGTH,
					TRANSFER_PENALTY);
			LinePlanningMaster model = new LinePlanningMaster(linePlanningInstance);
			System.out.println("Lines: " + linePool.size() + ". Paths: " + paths.size());

			// Add constraints.
			for (ODPair pair : linePlanningInstance.getODPairs())
			{
				model.addConstraint(new PathAssignmentConstraint(pair));
			}
			for (DirectedGraphArc<StationNode, CGNArc> arc : changeAndGo.getArcs())
			{
				if (arc.getData() instanceof TravelArc)
				{
					// Capacity constraint.
					TravelArc travelArc = (TravelArc) arc.getData();
					model.addConstraint(new LineSegmentCapacityConstraint(travelArc));
				}
			}

			// Add reverse line constraints.
			for (Line line : linePlanningInstance.getLinePool())
			{
				if (line.isReverseLine())
				{
					model.addConstraint(new ReverseLineConstraint(line));
				}
			}

			// Add line variables.
			for (Line line : linePlanningInstance.getLinePool())
			{
				model.addColumn(new LineColumn(line));
			}

			// Add path variables.
			for (Path path : paths)
			{
				model.addColumn(new PathColumn(path));
			}

			// Apply RMH.
			model.setOut(System.out);
			linePlan = (LinePlanningSolution) model.applyRestrictedMasterHeuristic(TIME_LIMIT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void generatePassengerPaths()
	{
		Map<StationNode, Set<ODPair>> ODPairMap = new LinkedHashMap<>();
		for (ODPair pair : ODPairs)
		{
			StationNode origin = pair.getOrigin();
			if (!ODPairMap.containsKey(origin))
			{
				ODPairMap.put(origin, new LinkedHashSet<>());
			}
			ODPairMap.get(origin).add(pair);
		}

		// Enumerate paths through CNG network for each OD pair.
		paths = new LinkedHashSet<>();
		int count = 0;
		for (StationNode origin : ODPairMap.keySet())
		{
			count++;
			System.out.println(count + " out of " + ODPairMap.size());
			Dijkstra<StationNode, CGNArc> dijkstra = new Dijkstra<>(changeAndGo, origin);
			dijkstra.computeDistances();
			for (ODPair pair : ODPairMap.get(origin))
			{
				double distance = dijkstra.getDistance(pair.getDestination());
				if (!dijkstra.containsPath(pair.getDestination()))
				{
					System.out.println("No path included.");
					continue;
				}

				List<Path> pathsPerPair = new ArrayList<>();
				double increment = 0.05;
				double detourFraction = 1.05;
				while (detourFraction <= MAX_DETOUR_FRACTION_PATHS)
				{
					pathsPerPair = new ArrayList<>();
					double threshold = detourFraction * distance;
					DFS<StationNode, CGNArc> DFS = new DFS<>();
					for (List<DirectedGraphArc<StationNode, CGNArc>> list : DFS.applyDFS(changeAndGo, origin,
							pair.getDestination(), threshold, new DefaultArcWeightFunction<StationNode, CGNArc>()))
					{
						double length = 0;
						List<CGNArc> arcs = new ArrayList<>();
						for (DirectedGraphArc<StationNode, CGNArc> arc : list)
						{
							arcs.add(arc.getData());
							length += arc.getCost();
						}
						pathsPerPair.add(new Path(pair, arcs, length));
					}
					if (pathsPerPair.size() >= MAX_PATHS_PER_OD_PAIR)
					{
						break;
					}
					detourFraction += increment;
				}
				pathsPerPair.sort(new PathComparator());
				pathsPerPair = pathsPerPair.subList(0, Math.min(MAX_PATHS_PER_OD_PAIR, pathsPerPair.size()));
				paths.addAll(pathsPerPair);
			}
		}
	}

	private static class PathComparator implements Comparator<Path>
	{
		@Override
		public int compare(Path a, Path b)
		{
			return Double.compare(a.getLength(), b.getLength());
		}
	}

	public static void constructChangeAndGoNetwork()
	{
		changeAndGo = GenerateCNGNetwork.generateCGNNetwork(infraNetwork, linePool, DWELL_TIME, TRANSFER_TIME,
				TRANSFER_PENALTY);
		System.out.println(changeAndGo.getNumberOfNodes() + " and arcs " + changeAndGo.getNumberOfArcs());
	}

	public static void generateODPairs()
	{
		// Do a first pass to generate total demand.
		double totalDemand = 0;
		double targetDemand = 0;
		for (GeoStation origin : stations)
		{
			targetDemand += origin.getPopulation();
			for (GeoStation destination : stations)
			{
				if (origin.equals(destination))
				{
					continue;
				}
				if (origin.getDistance(destination) > MAX_DEMAND_TRAVEL_DISTANCE)
				{
					continue;
				}
				totalDemand += origin.getPopulation() * destination.getPopulation() / origin.getDistance(destination);
			}
		}

		// Do a second pass to generate scaled demand.
		ODPairs = new ArrayList<>();
		for (GeoStation origin : stations)
		{
			for (GeoStation destination : stations)
			{
				if (origin.equals(destination))
				{
					continue;
				}
				if (origin.getDistance(destination) > MAX_DEMAND_TRAVEL_DISTANCE)
				{
					continue;
				}
				double demand = origin.getPopulation() * destination.getPopulation() / origin.getDistance(destination);
				if (demand < DEMAND_LOWER_THRESHOLD)
				{
					continue;
				}
				demand = (demand / totalDemand) * targetDemand;
				ODPairs.add(new ODPair(changeAndGo.getOriginNode(origin), changeAndGo.getDestinationNode(destination),
						demand));
			}
		}
	}

	public static void constructLinePool()
	{
		// Enumerate a set of lines.
		linePool = new LinkedHashSet<>();
		reverseLineMap = new LinkedHashMap<>();
		int lineIndex = 1;
		Set<Station> coveredNodes = new LinkedHashSet<>();
		for (int i = 0; i < infraNetwork.getNumberOfNodes(); i++)
		{
			StationNode firstStationNode = infraNetwork.getNodes().get(i);
			GeoStation firstStation = (GeoStation) firstStationNode.getStation();
			if (!firstStation.isLarge())
			{
				continue;
			}

			// Compute distances to other stations.
			Dijkstra<StationNode, Track> dijkstra = new Dijkstra<>(infraNetwork, firstStationNode);
			dijkstra.computeDistances();

			for (int j = i + 1; j < infraNetwork.getNumberOfNodes(); j++)
			{
				StationNode secondStationNode = infraNetwork.getNodes().get(j);
				GeoStation secondStation = (GeoStation) secondStationNode.getStation();
				if (!secondStation.isLarge())
				{
					continue;
				}

				// Find lines in station network.
				double distance = dijkstra.getDistance(secondStationNode);
				if (distance > MAX_LINE_DISTANCE)
				{
					continue;
				}
				double threshold = Math.min(MAX_LINE_DISTANCE, MAX_DETOUR_FRACTION_LINES * distance);
				DFS<StationNode, Track> DFS = new DFS<>();
				List<Line> possibleLines = new ArrayList<>();
				for (List<DirectedGraphArc<StationNode, Track>> list : DFS.applyDFS(infraNetwork, firstStationNode,
						secondStationNode, threshold, new DefaultArcWeightFunction<StationNode, Track>()))
				{
					// Construct a line out of this.
					int cost = FIXED_LINE_COST;
					List<Track> lineSegments = new ArrayList<>();
					for (DirectedGraphArc<StationNode, Track> arc : list)
					{
						coveredNodes.add(arc.getTo().getStation());
						coveredNodes.add(arc.getFrom().getStation());
						cost += arc.getData().getDuration();
						lineSegments.add(arc.getData());
					}
					Line line = new Line(lineIndex, true, lineSegments, cost, MAX_LINE_FREQUENCY, LINE_CAPACITY, null);
					lineIndex++;
					possibleLines.add(line);
				}

				// A line should visit at least 2 stations, if there are also non-direct
				// options.
				possibleLines.sort((a, b) -> Integer.compare(getNumberLargeStations(a, largeStations),
						getNumberLargeStations(b, largeStations)));
				if (possibleLines.size() >= 2 && getNumberLargeStations(possibleLines.get(0), largeStations) == 2)
				{
					possibleLines.remove(0);
				}

				// Filter the X shortest lines.
				possibleLines.sort((a, b) -> Integer.compare(a.getCost(), b.getCost()));
				possibleLines = possibleLines.subList(0, Math.min(possibleLines.size(), MAX_LINES_PER_PAIR));

				// Add lines and their reverse.
				for (Line line : possibleLines)
				{
					linePool.add(line);
					Line reverseLine = line.reverseLine(lineIndex);
					lineIndex++;
					linePool.add(reverseLine);
					reverseLineMap.put(line, reverseLine);
					reverseLineMap.put(reverseLine, line);

					// Also create a sprinter line.
					if (stations.size() - largeStations.size() > 0)
					{
						Line sprinterLine = new Line(lineIndex, false, line.getLineSegments(), line.getCost(),
								MAX_LINE_FREQUENCY, LINE_CAPACITY, null);
						lineIndex++;
						linePool.add(sprinterLine);
						Line reverseSprinterLine = sprinterLine.reverseLine(lineIndex);
						lineIndex++;
						linePool.add(reverseSprinterLine);
						reverseLineMap.put(sprinterLine, reverseSprinterLine);
						reverseLineMap.put(reverseSprinterLine, sprinterLine);
					}
				}
			}
		}
	}

	public static int getNumberLargeStations(Line line, Set<GeoStation> largeStations)
	{
		int count = 0;
		for (Station station : line.getStations())
		{
			if (largeStations.contains(station))
			{
				count++;
			}
		}
		return count;
	}

	public static double computeDistanceToTrack(GeoStation a, GeoStation b, GeoStation station)
	{
		double deltaX = b.getX() - a.getX();
		double deltaY = b.getY() - a.getY();

		// Parameter t determines the closest point on the segment
		double x = station.getX();
		double y = station.getY();
		double t = ((x - a.getX()) * deltaX + (y - a.getY()) * deltaY) / (deltaX * deltaX + deltaY * deltaY);

		// Clamp t to the range [0, 1]
		t = Math.max(0, Math.min(1, t));

		// Find the closest point on the segment
		double closestX = a.getX() + t * deltaX;
		double closestY = a.getY() + t * deltaY;

		// Compute the distance from the point to the closest point
		return Math.hypot(x - closestX, y - closestY);
	}

	public static void generateRandomNetwork(int numLargeStations, int numSmallStations, long seed)
	{
		// Initialise random generator.
		Random random = new Random(seed);

		// Generate large stations.
		stations = new LinkedHashSet<>();
		largeStations = new LinkedHashSet<>();
		while (stations.size() < numLargeStations)
		{
			double x = MAX_WIDTH * random.nextDouble();
			double y = MAX_HEIGHT * random.nextDouble();
			double population = LARGE_CITY_LOWER_THRESHOLD
					+ random.nextDouble(LARGE_CITY_UPPER_THRESHOLD - LARGE_CITY_LOWER_THRESHOLD);
			GeoStation station = new GeoStation(stations.size(), String.valueOf((char) ('A' + stations.size())), x, y,
					population, true);

			// Directly add the city if there are no other cities yet.
			if (stations.size() == 0)
			{
				stations.add(station);
				continue;
			}

			// Check if the distance to existing cities is not too small or large.
			double nearestDistance = Double.MAX_VALUE;
			for (GeoStation otherStation : stations)
			{
				double distance = station.getDistance(otherStation);
				nearestDistance = Math.min(distance, nearestDistance);
			}
			if (nearestDistance > MAX_STATION_DISTANCE)
			{
				continue;
			}
			if (nearestDistance < MIN_STATION_DISTANCE_LARGE)
			{
				continue;
			}
			stations.add(station);
		}
		largeStations.addAll(stations);

		// Generate full physical network.
		DirectedGraph<StationNode, Track> network = new DirectedGraph<>();
		for (GeoStation station : stations)
		{
			network.addNode(new StationNode(station));
		}
		for (int i = 0; i < network.getNumberOfNodes(); i++)
		{
			StationNode from = network.getNodes().get(i);
			GeoStation stationFrom = (GeoStation) from.getStation();
			for (int j = i + 1; j < network.getNumberOfNodes(); j++)
			{
				StationNode to = network.getNodes().get(j);
				GeoStation stationTo = (GeoStation) to.getStation();
				int distance = (int) Math.ceil(stationFrom.getDistance(stationTo));
				if (distance > MAX_TRACK_DISTANCE)
				{
					continue;
				}
				network.addArc(from, to, new Track(stationFrom, stationTo, distance), distance);
			}
		}

		// Convert to minimum spanning tree.
		infraNetwork = new Prim<StationNode, Track>().minimumSpanningTree(network);

		// Add arcs in reverse direction too.
		List<DirectedGraphArc<StationNode, Track>> arcs = new ArrayList<>();
		arcs.addAll(infraNetwork.getArcs());
		for (DirectedGraphArc<StationNode, Track> arc : arcs)
		{
			infraNetwork.addArc(arc.getTo(), arc.getFrom(),
					new Track(arc.getTo().getStation(), arc.getFrom().getStation(), arc.getData().getDuration()),
					arc.getData().getDuration());
		}

		// Add those arcs that would reduce the average travel time by the most.
		int numExtraArcs = (int) Math.ceil(FRACTION_EXTRA_ARCS * (numLargeStations - 1));
		for (int i = 0; i < numExtraArcs; i++)
		{
			// For each possible addition to the network, compute the reduction in
			// total travel time.
			double minTravelTime = Double.MAX_VALUE;
			DirectedGraphArc<StationNode, Track> minArc = null;
			for (StationNode station : infraNetwork.getNodes())
			{
				arcLoop:
				for (DirectedGraphArc<StationNode, Track> outArc : network.getOutArcs(station))
				{
					// Temporarily add arcs.
					if (infraNetwork.getArcs().contains(outArc))
					{
						continue;
					}
					// Check if intersects with existing arcs.
					for (DirectedGraphArc<StationNode, Track> arc : infraNetwork.getArcs())
					{
						if (intersects(outArc.getData(), arc.getData()))
						{
							continue arcLoop;
						}
					}

					DirectedGraphArc<StationNode, Track> firstArc = infraNetwork.addArc(outArc.getFrom(),
							outArc.getTo(), outArc.getData(), outArc.getCost());
					DirectedGraphArc<StationNode, Track> secondArc = infraNetwork.addArc(outArc.getTo(),
							outArc.getFrom(), new Track(outArc.getTo().getStation(), outArc.getFrom().getStation(),
									outArc.getData().getDuration()),
							outArc.getCost());

					// Compute total weighted travel time.
					double travelTime = computeDistance(infraNetwork);
					if (travelTime < minTravelTime)
					{
						minTravelTime = travelTime;
						minArc = outArc;
					}

					// Remove arcs again.
					infraNetwork.removeArc(firstArc);
					infraNetwork.removeArc(secondArc);
				}
			}
			System.out.println("Travel time: " + minTravelTime);
			if (minArc != null)
			{
				infraNetwork.addArc(minArc.getFrom(), minArc.getTo(), new Track(minArc.getFrom().getStation(),
						minArc.getTo().getStation(), minArc.getData().getDuration()), minArc.getCost());
				infraNetwork.addArc(minArc.getTo(), minArc.getFrom(), new Track(minArc.getTo().getStation(),
						minArc.getFrom().getStation(), minArc.getData().getDuration()), minArc.getCost());
			}
		}

		// Generate small stations.
		while (stations.size() < numLargeStations + numSmallStations)
		{
			double x = MAX_WIDTH * random.nextDouble();
			double y = MAX_HEIGHT * random.nextDouble();
			double population = SMALL_CITY_LOWER_THRESHOLD
					+ random.nextDouble(SMALL_CITY_UPPER_THRESHOLD - SMALL_CITY_LOWER_THRESHOLD);
			GeoStation station = new GeoStation(stations.size(),
					String.valueOf((char) ('a' + stations.size() - numLargeStations)), x, y, population, false);

			// Directly add the city if there are no other cities yet.
			if (stations.size() == 0)
			{
				stations.add(station);
				continue;
			}

			// Check if the distance to existing cities is not too small or large.
			double nearestDistance = Double.MAX_VALUE;
			for (GeoStation otherStation : stations)
			{
				double distance = station.getDistance(otherStation);
				nearestDistance = Math.min(distance, nearestDistance);
			}
			if (nearestDistance > MAX_STATION_DISTANCE_SMALL)
			{
				continue;
			}
			if (nearestDistance < MIN_STATION_DISTANCE_SMALL)
			{
				continue;
			}

			// Project onto the nearest arc.
			double nearestTrackDistance = Double.MAX_VALUE;
			DirectedGraphArc<StationNode, Track> nearestArc = null;
			for (DirectedGraphArc<StationNode, Track> arc : infraNetwork.getArcs())
			{
				GeoStation from = (GeoStation) arc.getFrom().getStation();
				GeoStation to = (GeoStation) arc.getTo().getStation();

				// Compute relative detour when splitting up track.
				double fractionDetour = (station.getDistance(from) + station.getDistance(to)) / from.getDistance(to);
				if (fractionDetour > 1.2)
				{
					continue;
				}

				double distance = computeDistanceToTrack(from, to, station);
				if (distance < nearestTrackDistance)
				{
					nearestTrackDistance = distance;
					nearestArc = arc;
				}
			}
			if (nearestTrackDistance > 10)
			{
				continue;
			}
			stations.add(station);
			StationNode stationNode = new StationNode(station);
			infraNetwork.addNode(stationNode);

			// Split up original arc into new ones.
			infraNetwork.removeArc(nearestArc);
			Track nearestTrack = nearestArc.getData();
			DirectedGraphArc<StationNode, Track> reverseArc = new DirectedGraphArc<>(nearestArc.getTo(),
					nearestArc.getFrom(),
					new Track(nearestTrack.getTo(), nearestTrack.getFrom(), nearestTrack.getDuration()),
					nearestTrack.getDuration());
			infraNetwork.removeArc(reverseArc);

			// Add arc to original from.
			StationNode from = nearestArc.getFrom();
			GeoStation stationFrom = (GeoStation) from.getStation();
			int distance = (int) Math.ceil(station.getDistance(stationFrom));
			infraNetwork.addArc(from, stationNode, new Track(stationFrom, station, distance), distance);
			infraNetwork.addArc(stationNode, from, new Track(station, stationFrom, distance), distance);

			// Add arc to original to.
			StationNode to = nearestArc.getTo();
			GeoStation stationTo = (GeoStation) to.getStation();
			distance = (int) Math.ceil(station.getDistance(stationTo));
			infraNetwork.addArc(to, stationNode, new Track(stationTo, station, distance), distance);
			infraNetwork.addArc(stationNode, to, new Track(station, stationTo, distance), distance);

		}

		// Postprocessing.
		rescaleCoordinates();
	}

	private static void rescaleCoordinates()
	{
		// Scale cities back to origin.
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		for (GeoStation station : stations)
		{
			minX = Math.min(minX, station.getX());
			minY = Math.min(minY, station.getY());
		}
		double scaleX = Math.max(0, minX - 5);
		double scaleY = Math.max(0, minY - 5);
		for (GeoStation station : stations)
		{
			station.setX(station.getX() - scaleX);
			station.setY(station.getY() - scaleY);
		}
	}

	private static boolean intersects(Track a, Track b)
	{
		GeoStation startA = (GeoStation) a.getFrom();
		GeoStation endA = (GeoStation) a.getTo();
		Point startPointA = new Point(startA.getX(), startA.getY());
		Point endpointA = new Point(endA.getX(), endA.getY());

		GeoStation startB = (GeoStation) b.getFrom();
		GeoStation endB = (GeoStation) b.getTo();
		Point startPointB = new Point(startB.getX(), startB.getY());
		Point endpointB = new Point(endB.getX(), endB.getY());
		return LineIntersection.doIntersect(startPointA, endpointA, startPointB, endpointB);
	}

	private static double computeDistance(DirectedGraph<StationNode, Track> network)
	{
		double distance = 0;
		for (StationNode origin : network.getNodes())
		{
			Dijkstra<StationNode, Track> dijkstra = new Dijkstra<>(network, origin);
			dijkstra.computeDistances();
			for (StationNode destination : network.getNodes())
			{
				distance += dijkstra.getDistance(destination);
			}
		}
		return distance;
	}
}
