//package scheduling.columnGeneration;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import graph.algorithms.shortestPath.ShortestPath;
//import graph.algorithms.shortestPath.ShortestPathTimeLimit;
//import graph.structures.Path;
//import graph.structures.PricingGraph;
//import graph.structures.digraph.DirectedGraphArc;
//import graph.structures.digraph.activation.Activation;
//import graph.structures.digraph.activation.DefaultActivationFunction;
//import modelling.Connection;
//import modelling.Connection.ConnectionType;
//import modelling.Constants;
//import modelling.Task;
//import modelling.Template;
//import modelling.comparators.ConnectionBreakStartTimeComparator;
//import modelling.comparators.ConnectionEndTimeComparator;
//import modelling.constraints.dutyConstraints.DutyLength;
//import modelling.constraints.dutyConstraints.MealBreakFunction;
//import optimisation.columnGeneration.AbstractMasterProblem;
//import optimisation.columnGeneration.pricing.AbstractPricingProblemSolver;
//import scheduling.ArcTime;
//import scheduling.ConstructDuty;
//import scheduling.SchedulingInstance;
//import util.Configuration;
//import util.Pair;
//
//public class BreakPricing extends AbstractPricingProblemSolver<SchedulingInstance, SchedulingColumn, PricingProblem>
//{
//	private Map<DirectedGraphArc<Task, Connection>, Set<Task>> breakTasksPerSourceArc;
//	private Map<Task, List<DirectedGraphArc<Task, Connection>>> breakArcsPerBreakTask;
//	private Map<Task, List<DirectedGraphArc<Task, Connection>>> sinkArcsPerBreakTask;
//
//	private final int DISCRETISATION_FACTOR = Constants.SECONDS_PER_HALF_HOUR;
//
//	public BreakPricing()
//	{
//		super("breakPricing");
//
//		this.breakTasksPerSourceArc = new LinkedHashMap<>();
//		this.breakArcsPerBreakTask = new LinkedHashMap<>();
//		this.sinkArcsPerBreakTask = new LinkedHashMap<>();
//	}
//
//	public void performPreProcessing(PricingGraph graph)
//	{
//		// Pre-processing is not template-specific, and hence we must use a general
//		// activation function.
//		Activation.getActivation().setActivationFunction(new DefaultActivationFunction());
//
//		// Compute break end tasks.
//		List<DirectedGraphArc<Task, Connection>> breakArcs = graph.getBreakArcs();
//		Set<Task> postBreakTasks = getPostBreakTasks(breakArcs);
//
//		for (Task postBreakTask : postBreakTasks)
//		{
//			// Compute reachable pre-break tasks.
//			breakArcsPerBreakTask.put(postBreakTask, new ArrayList<>());
//			for (DirectedGraphArc<Task, Connection> arc : graph.getInArcs(postBreakTask))
//			{
//				if (arc.getData().hasBreak())
//				{
//					breakArcsPerBreakTask.get(postBreakTask).add(arc);
//				}
//			}
//
//			// Sort break arcs by increasing break start time.
//			breakArcsPerBreakTask.get(postBreakTask).sort(new ConnectionBreakStartTimeComparator());
//
//			// Compute shortest path.
//			ShortestPathTimeLimit shortestPath = new ShortestPathTimeLimit(graph, postBreakTask, graph.getSinkNode(),
//					true, 0, postBreakTask.getStartTime() + Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK);
//
//			// Compute reachable end tasks.
//			sinkArcsPerBreakTask.put(postBreakTask, new ArrayList<>());
//			for (DirectedGraphArc<Task, Connection> arc : graph.getInArcs(graph.getSinkNode()))
//			{
//				// Check if this end task can be reached.
//				if (!shortestPath.containsPath(arc.getFrom()))
//				{
//					continue;
//				}
//
//				// Check if the time since the start of the post-break task to end does not
//				// violate the meal break restriction already.
//				int timeAfterBreak = ArcTime.getEndTime(arc) - postBreakTask.getStartTime();
//				if (!MealBreakFunction.satisfiesMealBreakConstraint(timeAfterBreak))
//				{
//					continue;
//				}
//				sinkArcsPerBreakTask.get(postBreakTask).add(arc);
//			}
//
//			// Sort sink arcs by increasing end time.
//			sinkArcsPerBreakTask.get(postBreakTask).sort(new ConnectionEndTimeComparator());
//		}
//
//		// Compute start tasks.
//		for (DirectedGraphArc<Task, Connection> sourceArc : graph.getOutArcs(graph.getSourceNode()))
//		{
//			// Retrieve start task and start time.
//			Task startTask = sourceArc.getTo();
//			int startTime = ArcTime.getStartTime(sourceArc);
//
//			// Compute shortest path.
//			ShortestPathTimeLimit shortestPath = new ShortestPathTimeLimit(graph, startTask, graph.getSinkNode(), true,
//					0, startTime + Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK);
//
//			// Compute reachable break tasks.
//			breakTasksPerSourceArc.put(sourceArc, new LinkedHashSet<>());
//			for (Task postBreakTask : postBreakTasks)
//			{
//				// Only add if one of pre-break tasks of this post-break task can be reached.
//				for (DirectedGraphArc<Task, Connection> breakArc : breakArcsPerBreakTask.get(postBreakTask))
//				{
//					if (shortestPath.containsPath(breakArc.getFrom()))
//					{
//						breakTasksPerSourceArc.get(sourceArc).add(postBreakTask);
//						break;
//					}
//				}
//
//				// Alternatively: The task after the break should equal the start task.
//				if (postBreakTask.equals(startTask))
//				{
//					breakTasksPerSourceArc.get(sourceArc).add(postBreakTask);
//				}
//			}
//		}
//	}
//
//	@Override
//	public List<Pair<SchedulingColumn, Double>> generateColumns(
//			AbstractMasterProblem<SchedulingInstance, SchedulingColumn, PricingProblem> masterProblem,
//			PricingProblem pricingProblem, double reducedCostThreshold)
//	{
//		// Initialise paths and retrieve pricing graph.
//		List<Path> paths = new ArrayList<>();
//		PricingGraph graph = pricingProblem.getPricingGraph();
//		double fixedTemplateDual = pricingProblem.getTemplateDual();
//
//		// General completion bound.
//		ShortestPath<Task, Connection> generalShortestPath = new ShortestPath<>(graph, graph.getSinkNode(),
//				graph.getSourceNode(), false);
//		double generalCompletionBound = generalShortestPath.getDistance(graph.getSourceNode()) - fixedTemplateDual;
//		if (generalCompletionBound > reducedCostThreshold)
//		{
//			return ConstructDuty.constructDutyColumns(pricingProblem, paths);
//		}
//
//		// Filter and sort source arcs.
//		List<DirectedGraphArc<Task, Connection>> sourceArcs = graph	.getOutArcs(graph.getSourceNode()).stream().filter(
//																			arc -> Activation	.getActivation()
//																								.getActivationFunction()
//																								.isActiveArc(arc))
//																	.collect(Collectors.toList());
//		sourceArcs.sort(new SourceArcComparator());
//
//		// Initialise shortest path map and discretised path maps.
//		Map<Integer, ShortestPathTimeLimit> discretisedSourceMap = new LinkedHashMap<>();
//		Map<Integer, ShortestPathTimeLimit> discretisedSinkMap = new LinkedHashMap<>();
//		Map<Task, ShortestPathTimeLimit> shortestPathMap = new LinkedHashMap<>();
//
//		// Loop over source arcs (and thereby over start tasks).
//		for (DirectedGraphArc<Task, Connection> sourceArc : sourceArcs)
//		{
//			// Initialise cost determined by source arc.
//			double minCostStart = sourceArc.getWeight() + sourceArc.getTo().getWeight() - fixedTemplateDual;
//
//			// Apply a preliminary completion bound.
//			double bound = minCostStart + generalShortestPath.getDistance(sourceArc.getTo());
//			if (bound > reducedCostThreshold)
//			{
//				continue;
//			}
//
//			// Retrieve start task and start time.
//			Task startTask = sourceArc.getTo();
//			int startTime = ArcTime.getStartTime(sourceArc);
//			ensureInShortestPathMap(startTask, shortestPathMap,
//					graph, pricingProblem.getTemplate());
//
//			// Loop over all break end tasks.
//			breakTaskLoop: for (Task breakEndTask : breakTasksPerSourceArc.get(sourceArc))
//			{
//				// Perform activation check.
//				if (!Activation.getActivation().getActivationFunction().isActiveNode(breakEndTask))
//				{
//					continue;
//				}
//
//				// Only consider break end tasks that are within maximum end time.
//				boolean isSink = graph.getSinkNode().equals(breakEndTask);
//				if (!DutyLength.isFeasibleEndTime(startTime, breakEndTask.getEndTime()) && !isSink)
//				{
//					continue;
//				}
//
//				// Compute a completion bound. We either use the discretised shortest path map,
//				// or the actual distance from the start task, if this is precomputed. We bound
//				// the distance from break to the sink using the maximum allowed end time.
//				int latestEndTime = Math.min(startTime + Constants.dutyLengthFunction(startTime),
//						breakEndTask.getStartTime() + Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK);
//				int hourSink = roundUp(latestEndTime);
//				int endHour = roundUp(pricingProblem.getTemplate().getEndTime());
//				hourSink = Math.min(endHour, hourSink);
//				int hourSource = roundDown(startTime);
//				ensureInDiscretisedSinkMap(hourSink, discretisedSinkMap, graph);
//				bound = discretisedSinkMap.get(hourSink).getDistance(breakEndTask);
//
//				if (shortestPathMap.get(startTask).containsPath(breakEndTask))
//				{
//					bound += minCostStart;
//					bound += shortestPathMap.get(startTask).getDistance(breakEndTask);
//				}
//				else
//				{
//					ensureInDiscretisedSourceMap(hourSource, discretisedSourceMap, graph);
//					bound += discretisedSourceMap.get(hourSource).getDistance(breakEndTask);
//					bound -= fixedTemplateDual;
//				}
//				if (bound > reducedCostThreshold)
//				{
//					continue;
//				}
//
//				// Determine the break arc.
//				double minCostBreak = 0;
//				DirectedGraphArc<Task, Connection> minimumBreakArc = null;
//				if (startTask.equals(breakEndTask))
//				{
//					// If the post-break task equals the start task, the source arc must contain a
//					// break.
//					if (!sourceArc.getData().hasBreak())
//					{
//						continue;
//					}
//					minimumBreakArc = sourceArc;
//				}
//				else
//				{
//					// We determine the optimal pre-break task.
//					minimumBreakArc = determineOptimalBreakArc(startTime, startTask,
//							breakEndTask, shortestPathMap);
//					if (minimumBreakArc == null)
//					{
//						continue;
//					}
//					minCostBreak = shortestPathMap.get(startTask).getDistance(minimumBreakArc.getFrom())
//							+ minimumBreakArc.getWeight() + breakEndTask.getWeight();
//				}
//
//				// Apply a more fine-grained completion bound.
//				bound = minCostStart + minCostBreak
//						+ discretisedSinkMap.get(hourSink).getDistance(breakEndTask);
//				if (bound > reducedCostThreshold)
//				{
//					continue;
//				}
//
//				// If the shortest path from the break to the sink satisfies the duty length
//				// constraint, we can convert the discretised shortest path to a feasible path.
//				for (DirectedGraphArc<Task, Connection> sinkArc : discretisedSinkMap.get(hourSink)
//																					.getPath(breakEndTask))
//				{
//					if (sinkArc.getData().getConnectionType().equals(ConnectionType.SINK))
//					{
//						if (ArcTime.getEndTime(sinkArc) > latestEndTime)
//						{
//							break;
//						}
//
//						// Add path. We incorporate the fixed template dual here.
//						Path path = new Path();
//						path.addWeight(-fixedTemplateDual);
//						path.addArc(sourceArc);
//						if (!minimumBreakArc.getTo().equals(startTask))
//						{
//							for (DirectedGraphArc<Task, Connection> arc : shortestPathMap	.get(sourceArc.getTo())
//																							.getPath(
//																									minimumBreakArc.getFrom()))
//							{
//								path.addArc(arc);
//							}
//							path.addArc(minimumBreakArc);
//						}
//						for (DirectedGraphArc<Task, Connection> arc : discretisedSinkMap.get(hourSink)
//																						.getPath(
//																								breakEndTask))
//						{
//							path.addArc(arc);
//						}
//						paths.add(path);
//						continue breakTaskLoop;
//					}
//				}
//
//				// Ensure that task is in shortest path map.
//				ensureInShortestPathMap(breakEndTask, shortestPathMap,
//						graph, pricingProblem.getTemplate());
//
//				// Completion bound.
//				if (minCostStart + minCostBreak + shortestPathMap.get(breakEndTask).getDistance(
//						graph.getSinkNode()) > reducedCostThreshold)
//				{
//					continue;
//				}
//
//				// Determine the best end task.
//				double minCostEnd = 0;
//				DirectedGraphArc<Task, Connection> minimumSinkArc = null;
//				if (breakEndTask.equals(graph.getSinkNode()))
//				{
//					// We are already at the sink.
//					minimumSinkArc = minimumBreakArc;
//				}
//				else
//				{
//					// We determine the optimal path to the sink.
//					minimumSinkArc = determineOptimalSinkArc(startTime, breakEndTask, shortestPathMap);
//					if (minimumSinkArc == null)
//					{
//						continue;
//					}
//					minCostEnd = shortestPathMap.get(breakEndTask).getDistance(minimumSinkArc.getFrom())
//							+ minimumSinkArc.getWeight();
//				}
//
//				// Construct a path if the reduced cost is negative.
//				double reducedCost = minCostStart + minCostBreak + minCostEnd;
//				if (reducedCost > reducedCostThreshold)
//				{
//					continue;
//				}
//
//				// Add path. We incorporate the fixed template dual here.
//				Path path = new Path();
//				path.addWeight(-fixedTemplateDual);
//				path.addArc(sourceArc);
//				if (!minimumBreakArc.getTo().equals(startTask))
//				{
//					for (DirectedGraphArc<Task, Connection> arc : shortestPathMap	.get(sourceArc.getTo())
//																					.getPath(minimumBreakArc.getFrom()))
//					{
//						path.addArc(arc);
//					}
//					path.addArc(minimumBreakArc);
//				}
//				if (!minimumBreakArc.equals(minimumSinkArc))
//				{
//					for (DirectedGraphArc<Task, Connection> arc : shortestPathMap	.get(minimumBreakArc.getTo())
//																					.getPath(minimumSinkArc.getFrom()))
//					{
//						path.addArc(arc);
//					}
//					path.addArc(minimumSinkArc);
//				}
//				paths.add(path);
//			}
//		}
//
//		// Return the best K columns.
//		int K = Configuration.getConfiguration().getIntProperty("K");
//		if (paths.size() > K)
//		{
//			paths.sort(new PathComparator());
//			paths = paths.subList(0, K);
//		}
//		return ConstructDuty.constructDutyColumns(pricingProblem, paths);
//	}
//
//	public Map<DirectedGraphArc<Task, Connection>, Set<Task>> performReducedCostFixing(PricingProblem pricingProblem,
//			double gap)
//	{
//		// Retrieve pricing graph.
//		PricingGraph graph = pricingProblem.getPricingGraph();
//		double fixedTemplateDual = pricingProblem.getTemplateDual();
//		int fixed = 0;
//		int total = 0;
//
//		// Filter and sort source arcs.
//		List<DirectedGraphArc<Task, Connection>> sourceArcs = graph	.getOutArcs(graph.getSourceNode()).stream().filter(
//																			arc -> Activation	.getActivation()
//																								.getActivationFunction()
//																								.isActiveArc(arc))
//																	.collect(Collectors.toList());
//		sourceArcs.sort(new SourceArcComparator());
//
//		// Initialise map of allowed break tasks per source arc.
//		Map<DirectedGraphArc<Task, Connection>, Set<Task>> allowedBreakTasksPerSourceArc = new LinkedHashMap<>();
//
//		// Initialise shortest path map.
//		Map<Task, ShortestPathTimeLimit> shortestPathMap = new LinkedHashMap<>();
//
//		// Loop over source arcs (and thereby over start tasks).
//		for (DirectedGraphArc<Task, Connection> sourceArc : sourceArcs)
//		{
//			// Initialise set of allowed break tasks.
//			Set<Task> allowedBreakTasks = new LinkedHashSet<>();
//
//			// Initialise cost determined by source arc.
//			double minCostStart = sourceArc.getWeight() + sourceArc.getTo().getWeight() - fixedTemplateDual;
//
//			// Retrieve start task and start time.
//			Task startTask = sourceArc.getTo();
//			int startTime = ArcTime.getStartTime(sourceArc);
//			ensureInShortestPathMap(startTask, shortestPathMap,
//					graph, pricingProblem.getTemplate());
//
//			// Loop over all break end tasks.
//			for (Task breakEndTask : breakTasksPerSourceArc.get(sourceArc))
//			{
//				// Perform activation check.
//				if (!Activation.getActivation().getActivationFunction().isActiveNode(breakEndTask))
//				{
//					continue;
//				}
//
//				// Only consider break end tasks that are within maximum end time.
//				boolean isSink = graph.getSinkNode().equals(breakEndTask);
//				if (!DutyLength.isFeasibleEndTime(startTime, breakEndTask.getEndTime()) && !isSink)
//				{
//					continue;
//				}
//
//				// Determine the break arc.
//				double minCostBreak = 0;
//				DirectedGraphArc<Task, Connection> minimumBreakArc = null;
//				if (startTask.equals(breakEndTask))
//				{
//					// If the post-break task equals the start task, the source arc must contain a
//					// break.
//					if (!sourceArc.getData().hasBreak())
//					{
//						continue;
//					}
//					minimumBreakArc = sourceArc;
//				}
//				else
//				{
//					// We determine the optimal pre-break task.
//					minimumBreakArc = determineOptimalBreakArc(startTime, startTask,
//							breakEndTask, shortestPathMap);
//					if (minimumBreakArc == null)
//					{
//						continue;
//					}
//					minCostBreak = shortestPathMap.get(startTask).getDistance(minimumBreakArc.getFrom())
//							+ minimumBreakArc.getWeight() + breakEndTask.getWeight();
//				}
//
//				// Ensure that task is in shortest path map.
//				ensureInShortestPathMap(breakEndTask, shortestPathMap,
//						graph, pricingProblem.getTemplate());
//
//				// Determine the best end task.
//				double minCostEnd = 0;
//				DirectedGraphArc<Task, Connection> minimumSinkArc = null;
//				if (breakEndTask.equals(graph.getSinkNode()))
//				{
//					// We are already at the sink.
//					minimumSinkArc = minimumBreakArc;
//				}
//				else
//				{
//					// We determine the optimal path to the sink.
//					minimumSinkArc = determineOptimalSinkArc(startTime, breakEndTask, shortestPathMap);
//					if (minimumSinkArc == null)
//					{
//						continue;
//					}
//					minCostEnd = shortestPathMap.get(breakEndTask).getDistance(minimumSinkArc.getFrom())
//							+ minimumSinkArc.getWeight();
//				}
//
//				// Construct a path if the reduced cost is negative.
//				double reducedCost = minCostStart + minCostBreak + minCostEnd;
//				if (reducedCost > gap)
//				{
//					continue;
//				}
//				allowedBreakTasks.add(breakEndTask);
//			}
//			if (allowedBreakTasks.size() > 0)
//			{
//				allowedBreakTasksPerSourceArc.put(sourceArc, allowedBreakTasks);
//			}
//			total += breakTasksPerSourceArc.size();
//			fixed += (breakTasksPerSourceArc.size() - allowedBreakTasks.size());
//		}
//		System.out.println("Fixed " + fixed + " out of " + total + " break combinations.");
//
//		// TODO: Print number of available sources.
//		int numAvailable = 0;
//		for (DirectedGraphArc<Task, Connection> sourceArc : sourceArcs)
//		{
//			if (allowedBreakTasksPerSourceArc.containsKey(sourceArc))
//			{
//				numAvailable++;
//			}
//		}
//		System.out.println("Available source arcs: " + numAvailable + " out of " + sourceArcs.size());
//		return allowedBreakTasksPerSourceArc;
//	}
//
//	private class PathComparator implements Comparator<Path>
//	{
//		@Override
//		public int compare(Path o1, Path o2)
//		{
//			return Double.compare(o1.getWeight(), o2.getWeight());
//		}
//	}
//
//	private DirectedGraphArc<Task, Connection> determineOptimalBreakArc(
//			int startTime, Task startTask, Task breakEndTask,
//			Map<Task, ShortestPathTimeLimit> shortestPathMap)
//	{
//		double minCostBreak = Double.MAX_VALUE;
//		DirectedGraphArc<Task, Connection> minimumBreakArc = null;
//		for (DirectedGraphArc<Task, Connection> breakArc : breakArcsPerBreakTask.get(breakEndTask))
//		{
//			// Check activation.
//			if (!Activation.getActivation().getActivationFunction().isActiveArc(breakArc))
//			{
//				continue;
//			}
//			Task preBreakTask = breakArc.getFrom();
//
//			// Check if time without break is not exceeded.
//			int timeToBreak = ArcTime.getStartTime(breakArc) + breakArc.getData().getTimeBeforeBreak() - startTime;
//			if (!MealBreakFunction.satisfiesMealBreakConstraint(timeToBreak))
//			{
//				// We sort break arcs by increasing time to break. If this one violates the
//				// constraints, so will all subsequent arcs.
//				return minimumBreakArc;
//			}
//
//			// We include the weight of the start task and break end task here,
//			// as it is not captured in the arc or shortest path computations.
//			double costBreak = shortestPathMap.get(startTask).getDistance(preBreakTask)
//					+ breakArc.getWeight() + breakEndTask.getWeight();
//			if (costBreak < minCostBreak)
//			{
//				minCostBreak = costBreak;
//				minimumBreakArc = breakArc;
//			}
//		}
//		return minimumBreakArc;
//	}
//
//	private DirectedGraphArc<Task, Connection> determineOptimalSinkArc(int startTime,
//			Task breakEndTask, Map<Task, ShortestPathTimeLimit> shortestPathMap)
//	{
//		double minCostEnd = Double.MAX_VALUE;
//		DirectedGraphArc<Task, Connection> minimumSinkArc = null;
//		for (DirectedGraphArc<Task, Connection> sinkArc : sinkArcsPerBreakTask.get(breakEndTask))
//		{
//			// Retrieve end task and end time.
//			Task endTask = sinkArc.getFrom();
//			int endTime = ArcTime.getEndTime(sinkArc);
//
//			// Only consider end tasks that are within maximum end time. Note that the break
//			// is already checked by construction.
//			if (!DutyLength.isFeasibleEndTime(startTime, endTime))
//			{
//				// We sort sink arcs by increasing end time. If this one violates the duty
//				// length constraints, so will all subsequent arcs.
//				return minimumSinkArc;
//			}
//
//			// Check activation.
//			if (!Activation.getActivation().getActivationFunction().isActiveArc(sinkArc))
//			{
//				continue;
//			}
//
//			// Add the distance to sink.
//			double costEnd = shortestPathMap.get(breakEndTask).getDistance(endTask) + sinkArc.getWeight();
//			if (costEnd < minCostEnd)
//			{
//				minCostEnd = costEnd;
//				minimumSinkArc = sinkArc;
//			}
//		}
//		return minimumSinkArc;
//	}
//
//	private class SourceArcComparator implements Comparator<DirectedGraphArc<Task, Connection>>
//	{
//		@Override
//		public int compare(DirectedGraphArc<Task, Connection> a, DirectedGraphArc<Task, Connection> b)
//		{
//			return a.getTo().getEndTime() - b.getTo().getEndTime();
//		}
//	}
//
//	private void ensureInDiscretisedSourceMap(int hour, Map<Integer, ShortestPathTimeLimit> discretisedSourceMap,
//			PricingGraph graph)
//	{
//		if (!discretisedSourceMap.containsKey(hour))
//		{
//			int startTime = hour * DISCRETISATION_FACTOR;
//			int duration = DISCRETISATION_FACTOR + Constants.MAXIMUM_DUTY_LENGTH;
//			discretisedSourceMap.put(hour, new ShortestPathTimeLimit(graph, graph.getSourceNode(),
//					graph.getSinkNode(), true, startTime, startTime + duration));
//		}
//	}
//
//	private void ensureInDiscretisedSinkMap(int hour, Map<Integer, ShortestPathTimeLimit> discretisedSinkMap,
//			PricingGraph graph)
//	{
//		if (!discretisedSinkMap.containsKey(hour))
//		{
//			int endTime = hour * DISCRETISATION_FACTOR;
//			int duration = DISCRETISATION_FACTOR + Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK;
//			discretisedSinkMap.put(hour, new ShortestPathTimeLimit(graph, graph.getSinkNode(),
//					graph.getSourceNode(), false, endTime - duration, endTime));
//		}
//	}
//
//	private void ensureInShortestPathMap(Task task, Map<Task, ShortestPathTimeLimit> shortestPathMap,
//			PricingGraph graph, Template template)
//	{
//		if (!shortestPathMap.containsKey(task))
//		{
//			// Maximum allowed end time is determined by template and by meal break
//			// constraint.
//			int latestTime = Math.min(task.getStartTime() + Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK,
//					template.getEndTime());
//			shortestPathMap.put(task,
//					new ShortestPathTimeLimit(graph, task, graph.getSinkNode(), true, 0, latestTime));
//		}
//	}
//
//	private int roundDown(int time)
//	{
//		return (int) Math.floor((double) time / DISCRETISATION_FACTOR);
//	}
//
//	private int roundUp(int time)
//	{
//		return (int) Math.ceil((double) time / DISCRETISATION_FACTOR);
//	}
//
//	private Set<Task> getPostBreakTasks(List<DirectedGraphArc<Task, Connection>> breakArcs)
//	{
//		Set<Task> breakTasks = new LinkedHashSet<>();
//		for (DirectedGraphArc<Task, Connection> breakArc : breakArcs)
//		{
//			breakTasks.add(breakArc.getTo());
//		}
//		return breakTasks;
//	}
//}
