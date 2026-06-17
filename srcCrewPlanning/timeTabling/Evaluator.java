package timeTabling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraphArc;
import ilog.concert.IloException;
import linePlanning.LinePlanningSolution;
import linePlanning.instance.BoardingArc;
import linePlanning.instance.CGN;
import linePlanning.instance.CGNArc;
import linePlanning.instance.DwellArc;
import linePlanning.instance.Line;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import linePlanning.instance.Path;
import linePlanning.instance.StationLineNode;
import linePlanning.instance.StationNode;
import linePlanning.instance.Track;
import linePlanning.instance.TransferArc;
import linePlanning.instance.TravelArc;
import modelling.Station;
import synthetic.RandomInstanceGenerator;
import util.Pair;

public class Evaluator
{
	private final LinePlanningInstance inst;
	private final Set<Line> lines;
	private final Map<Line, Integer> lineFrequencies;
	private final Map<ODPair, Set<Pair<Path, Double>>> pathsPerODLinePlan; // paths according to LP

	private PESP pesp; // the pesp instance induced by the solution
	private Set<Activity> transfersNoPax; // the transfers without passengers according to the line plan model
	private EAN eanTimed; // EAN with the costs on each arc representing the travel costs (so including
							// transfer penalty)
	private Map<Event, Integer> timetable; // event times

	// map between cgn arcs and activities
	private Map<CGNArc, Activity> cgnArcToAct;

	// statistics according to line planning model
	private double travelTimeLinePlan; // total travel time
	private double totNrTransfersLinePlan; // total number of transfers
	private double transferPaxLinePlan; // proportion of passengers with at least one transfer
	private double transferTimeLinePlan; // average transfer time according to line plan

	// statistics according to PESP
	private double travelTimePESP; // total travel time
	private double transferTimePESP; // average transfer time

	public Evaluator(LinePlanningInstance inst, LinePlanningSolution solution)
	{
		this.inst = inst;
		this.lines = solution.getLines();
		this.lineFrequencies = solution.getLineFrequencyMap();
		System.out.println("Selected " + lines.size() / 2 + " lines:");
		for (Line l : lines)
		{
			System.out.println(l + " with frequency " + lineFrequencies.get(l));
		}
		this.pathsPerODLinePlan = solution.getPathsPerOD();
		this.travelTimeLinePlan = solution.getTravelTime();
	}

	public Set<ScheduledLine> getScheduledLines()
	{
		Set<ScheduledLine> scheduledLines = new LinkedHashSet<>();
		for (Line line : lineFrequencies.keySet())
		{
			int startTime = Integer.MAX_VALUE;
			int endTime = 0;
			Map<Track, Integer> departureTimes = new LinkedHashMap<>();
			Map<Track, Integer> arrivalTimes = new LinkedHashMap<>();
			int lastArrivalTime = -inst.getPeriod();
			for (Track track : line.getLineSegments())
			{
				Event departureEvent = null;
				Event arrivalEvent = null;
				for (Event event : timetable.keySet())
				{
					if (!event.getLine().equals(line))
					{
						continue;
					}
					if (event.isArrival() && event.getStation().equals(track.getTo()))
					{
						arrivalEvent = event;
					}
					if (!event.isArrival() && event.getStation().equals(track.getFrom()))
					{
						departureEvent = event;
					}
				}
				if (departureEvent == null || arrivalEvent == null)
				{
					System.out.println(line.isInterCity() + " " + departureEvent + " " + arrivalEvent);
					throw new Error("One or more events could not be matched.");
				}

				// Assign times.
				int departureTime = timetable.get(departureEvent) % departureEvent.getInterval();
				while (departureTime < lastArrivalTime + RandomInstanceGenerator.DWELL_TIME)
				{
					departureTime += departureEvent.getInterval();
				}
				int arrivalTime = departureTime + track.getDuration();
				departureTimes.put(track, departureTime);
				arrivalTimes.put(track, arrivalTime);
				startTime = Math.min(startTime, departureTime);
				endTime = Math.max(endTime, arrivalTime);
				lastArrivalTime = arrivalTime;
			}
			// TODO: Print.
			System.out.print(lineFrequencies.get(line) + " ");
			for (Track track : line.getLineSegments())
			{
				System.out.print("(" + departureTimes.get(track) + " :" + track.getDuration() + ": " + arrivalTimes.get(track) +")");
			}
			System.out.println();
			scheduledLines.add(new ScheduledLine(line, startTime, endTime, departureTimes, arrivalTimes));
		}
		return scheduledLines;
	}

	public void evaluate(long timeLimitPESP) throws IloException
	{
		computeTransferStatsLinePlan(pathsPerODLinePlan, false);
		printStatsLinePlan();
		generatePESP();
		solvePESP(timeLimitPESP);
		printStatsPESP();
	}

	private void printStatsPESP()
	{
		System.out.println("\nStatistics PESP: ");
		System.out.println("Average Travel Time: \t" + travelTimePESP / inst.getNrPax());
		System.out.println("Average Transfer Time (per transfering passenger): \t" + transferTimePESP);
	}

	private void printStatsLinePlan()
	{
		System.out.println("\nStatistics Line Planning Model: ");
		System.out.println("Average Travel Time: \t \t \t \t \t" + travelTimeLinePlan / inst.getNrPax());
		System.out.println("Proportion with at least one transfer: \t\t\t" + transferPaxLinePlan);
		System.out.println("Average Transfer Time (per transfering passenger): \t" + transferTimeLinePlan);
	}

	private void computeTransferStatsLinePlan(Map<ODPair, Set<Pair<Path, Double>>> pathsPerOD, boolean exPost)
	{
		double transferPax = 0;
		double transferTime = 0;
		double nrTransfers = 0;

		for (Set<Pair<Path, Double>> pathsAndWeights : pathsPerOD.values())
		{
			for (Pair<Path, Double> pathAndWeight : pathsAndWeights)
			{
				double nrPax = pathAndWeight.getValue();
				boolean pathHasTransfer = false;
				for (CGNArc a : pathAndWeight.getKey().getArcs())
				{
					if (a instanceof TransferArc)
					{
						pathHasTransfer = true;
						transferTime += ((TransferArc) a).getTransferTime() * nrPax;
						nrTransfers += nrPax;
					}
				}
				if (pathHasTransfer)
				{
					transferPax += nrPax;
				}
			}
		}
		// Divide by the total number of passengers
		double totPax = inst.getNrPax();
		if (!exPost)
		{
			transferPaxLinePlan = transferPax / totPax;
			transferTimeLinePlan = transferTime / transferPax;
			totNrTransfersLinePlan = nrTransfers;
		}
	}

	private void generatePESP() throws IloException
	{
		CGN filteredCGN = inst.getCGN().filter(lines);
		transfersNoPax = new HashSet<>();
		cgnArcToAct = new HashMap<>();

		Map<CGNArc, Double> weights = retrieveArcWeights();

		// Generate the Event-Activity Network
		Map<StationLineNode, Event> cgnToEan = new HashMap<>();
		EAN ean = new EAN();
		int id = 1;
		for (StationLineNode n : filteredCGN.getStationLineNodes())
		{
			int freq = lineFrequencies.get(n.getLine());
			if (inst.getPeriod() % freq != 0)
			{
				System.out.println("Freq: " + freq);
				throw new Error("code does not support frequencies that do not divide T ");
			}
			Event e = new Event(n.getLine(), n.getStation(), n.isArrival(), inst.getPeriod() / freq, id);
			id++;
			ean.addNode(e);
			cgnToEan.put(n, e);
		}

		for (DirectedGraphArc<StationNode, CGNArc> arc : filteredCGN.getArcs())
		{
			CGNArc cArc = arc.getData();

			// Skip boarding arcs
			if (cArc instanceof BoardingArc)
			{
				continue;
			}

			Event from = cgnToEan.get(arc.getFrom());
			Event to = cgnToEan.get(arc.getTo());
			int lb = cArc.getDuration();
			int ub = cArc.getDuration();
			String type;
			if (cArc instanceof DwellArc)
			{
				ub = lb + PESP.DWELL_TIME_SLACK;
				type = "dwell";
			}
			else if (cArc instanceof TravelArc)
			{
				ub = lb + PESP.TravelTimeSlack;
				type = "drive";
			}
			else
			{
				// It is a transfer arc, currently ub of period time.
				ub = lb + inst.getPeriod();
				type = "transfer";
			}
			double weight = 0;
			if (weights.containsKey(cArc))
			{
				weight = weights.get(cArc);
			}
			Activity act = new Activity(from, to, lb, ub, weight, type);
			// add the arc, unless it is a transfer without any passengers
			if (weight > 0 || !(cArc instanceof TransferArc))
			{
				ean.addArc(from, to, act, weight);
			}
			else
			{
				// Add the activity to a set, saving for later
				transfersNoPax.add(act);
			}
			// Add to the maps
			cgnArcToAct.put(cArc, act);
		}

		// Add turnaround constraints.
		for (StationLineNode node : filteredCGN.getStationLineNodes())
		{
			// Filter start or end stations.
			Line line = node.getLine();
			Station station = node.getStation();
			if (line.isReverseLine())
			{
				continue;
			}
			boolean isStart = (station.equals(line.getStartStation()) && !node.isArrival());
			boolean isEnd = (station.equals(line.getEndStation()) && node.isArrival());
			if (!isStart && !isEnd)
			{
				continue;
			}

			// Retrieve start station of reverse line.
			for (StationLineNode otherNode : filteredCGN.getStationLineNodes())
			{
				// Filter stations of reverse line.
				Line otherLine = otherNode.getLine();
				if (!otherLine.equals(line.getReverseLine()))
				{
					continue;
				}
				Station otherStation = otherNode.getStation();
				if (!otherStation.equals(station))
				{
					continue;
				}
				if ((isStart && otherNode.isArrival()) || (isEnd && !otherNode.isArrival()))
				{
					continue;
				}

				// Add turnaround activity.
				Event from = isStart ? cgnToEan.get(otherNode) : cgnToEan.get(node);
				Event to = isStart ? cgnToEan.get(node) : cgnToEan.get(otherNode);
				int lowerBound = PESP.TurnAroundTime;
				int upperBound = lowerBound + PESP.TurnAroundSlack;
				String type = "turnAround";
				double weight = 0;
				Activity activity = new Activity(from, to, lowerBound, upperBound, weight, type);
				ean.addArc(from, to, activity, weight);
			}
		}

		// Compute the incurred transfer penalty
		double totTransferPenalty = inst.getTransferPenalty() * totNrTransfersLinePlan;
		pesp = new PESP(ean, inst.getPeriod(), inst.getTransferPenalty(), totTransferPenalty);
	}

	public void solvePESP(long timeLimitPESP) throws IloException
	{
		pesp.solve(timeLimitPESP);
		travelTimePESP = pesp.getObjective();
		eanTimed = pesp.getTravelCostNetwork();
		timetable = pesp.getPotentials();
		transferTimePESP = 0;
		// compute the transfertime, and also include the transfer penalty in the
		// travelTime
		for (DirectedGraphArc<Event, Activity> a : eanTimed.getArcs())
		{
			if (a.getData().getType().equals("transfer"))
			{
				double flow = a.getData().getWeight();
				transferTimePESP += (a.getCost() - inst.getTransferPenalty()) * flow;
			}
		}
		transferTimePESP = transferTimePESP / (transferPaxLinePlan * inst.getNrPax());
	}

	/**
	 * Method that determines the periodic tension based on the potentials
	 */
	private int getTensionFromTimetable(Activity act)
	{
		int time = timetable.get(act.getTo()) - timetable.get(act.getFrom());
		while (time < act.getLb())
		{
			time += act.getPeriodicity();
		}
		return time;
	}

	/**
	 * Method that computes the number of passengers on each arc
	 */
	private Map<CGNArc, Double> retrieveArcWeights()
	{
		Map<CGNArc, Double> weights = new HashMap<>();
		for (Set<Pair<Path, Double>> pathsAndWeights : pathsPerODLinePlan.values())
		{
			for (Pair<Path, Double> pathAndWeight : pathsAndWeights)
			{
				double nrPax = pathAndWeight.getValue();
				for (CGNArc a : pathAndWeight.getKey().getArcs())
				{
					if (weights.containsKey(a))
					{
						weights.put(a, weights.get(a) + nrPax);
					}
					else
					{
						weights.put(a, nrPax);
					}
				}
			}
		}
		return weights;
	}

	public int getActualTime(CGNArc cArc)
	{
		// method might fail for boarding arcs
		return getTensionFromTimetable(cgnArcToAct.get(cArc));
	}

	public static int gcd(int a, int b)
	{
		if (b == 0) return a;
		return gcd(b, a % b);
	}
}
