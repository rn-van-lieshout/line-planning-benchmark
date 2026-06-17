package synthetic.visualiser;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import graph.structures.PricingGraph;
import ilog.concert.IloException;
import modelling.Task;
import optimisation.BAP.BranchAndPrice;
import optimisation.BAP.NodeComparators.BoundComparator;
import optimisation.BAP.primalHeuristics.DivingBacktracking;
import optimisation.columnGeneration.ColumnGeneration;
import scheduling.InitialiseConstraints;
import scheduling.SchedulingInstance;
import scheduling.SchedulingInstanceReader;
import scheduling.SchedulingMaster;
import scheduling.SchedulingMasterBuilder;
import scheduling.branching.BranchingRuleConnection;
import scheduling.branching.BranchingRuleCrewBase;
import scheduling.branching.BranchingRuleNumberDuties;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.PricingRoutine;
import scheduling.columnGeneration.SchedulingColumn;
import scheduling.columnGeneration.TemplateDisjointSelector;
import scheduling.columnGeneration.labelling.InitialiseResourceTests;
import scheduling.columnGeneration.labelling.LabellingSolver;
import scheduling.heuristic.ReducedCostFixing;
import util.Configuration;
import util.Logger;
import util.Writer;

public class RunSolver
{
	public static void main(String[] args) throws IloException, IOException
	{
		// Parameters.
		LocalDate date = LocalDate.parse("2025-02-04");

		// Retrieve folder indicated by index.
//		int index = Integer.valueOf(args[0]);
//		DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("instances/"));
//		List<Path> subfolders = new ArrayList<>();
//		for (Path entry : stream)
//		{
//			if (Files.isDirectory(entry))
//			{
//				subfolders.add(entry);
//			}
//		}
//		stream.close();
//		subfolders.sort(Comparator.comparing(Path::getFileName));
//		String folder = subfolders.get(index).toString();
		String folder = "l4_s4_t292";
		folder = "l6_s6_t737";
//		String instanceName = folder.substring(10);
		String instanceName = folder;
		System.out.println(instanceName);

		// TODO: Pass max number iterations to col gne solver

		// Initialise settings.
		String settingsFile = "defaultSynthetic.properties";
		Configuration.initialiseConfiguration(settingsFile);

		// Read instance.
		SchedulingInstance instance = SchedulingInstanceReader.readFolder(date, folder);

		// Print statistics.
		printStatistics(instance, instance.getDays().get(0));

		// Add cover constraints.
		InitialiseConstraints.initialiseCoverConstraints(instance);

		// Return instance.
		SchedulingMaster master = new SchedulingMasterBuilder().setProblem(instance).build();

		// Initialise column generation algorithm.
		PricingRoutine pricingRoutine = new PricingRoutine();
		LabellingSolver labelling = new LabellingSolver();
		List<PricingGraph> pricingGraphs = new ArrayList<>();
		pricingGraphs.add(instance.getDailyPricingGraph(date));
		labelling.addCompletionBound(
				InitialiseResourceTests.initialiseBreakResourceTest(pricingGraphs, instance.getCrewBases()));
		labelling.addCompletionBound(
				InitialiseResourceTests.initialiseLengthResourceTest(instance.getCrewBases(), pricingGraphs));
		TemplateDisjointSelector disjointSelector = new TemplateDisjointSelector();
		ColumnGeneration<SchedulingInstance, SchedulingColumn, PricingProblem> columnGeneration = new ColumnGeneration<SchedulingInstance, SchedulingColumn, PricingProblem>(
				pricingRoutine, labelling, labelling, disjointSelector);

		// Branch-and-price settings.
		BranchAndPrice<SchedulingInstance, SchedulingColumn, PricingProblem> branchAndPrice = new BranchAndPrice<SchedulingInstance, SchedulingColumn, PricingProblem>(
				new BoundComparator<SchedulingInstance, SchedulingColumn, PricingProblem>(), instance, master,
				columnGeneration);

		DivingBacktracking<SchedulingInstance, SchedulingColumn, PricingProblem> divingHeuristic = new DivingBacktracking<>();
		branchAndPrice.setRootNodeHeuristic(divingHeuristic);

		// Add reduced cost fixing module.
		double fixingThreshold = 5 * 1000;
		// TODO: Parameterise this through settings file.
		branchAndPrice.setReducedCostFixing(new ReducedCostFixing(fixingThreshold));

		// Add zero-half cut separator.
		Set<Task> cutTasks = new LinkedHashSet<>();
		for (Task task : instance.getTasks(date))
		{
			if (task.cover())
			{
				cutTasks.add(task);
			}
		}

		// Enumeration heuristic: 5740740, gap 1.44%
		// Diving: 5676420, gap

		// Branching rule.
		branchAndPrice.addBranchingRule(new BranchingRuleConnection(3));
//		branchAndPrice.addBranchingRule(new BranchingRuleConnectionTime(2));
//		branchAndPrice.addBranchingRule(new BranchingRuleDutyStartTime(1));
//		branchAndPrice.addBranchingRule(new BranchingRuleDutiesPerTask(1));
//		branchAndPrice.addBranchingRule(new BranchingRuleDutiesPerHour(2));
		branchAndPrice.addBranchingRule(new BranchingRuleCrewBase(1));
		branchAndPrice.addBranchingRule(new BranchingRuleNumberDuties(0));

		// Solve with branch-and-price.
		int timeLimit = 3 * 60 * 60 * 1000; // 3 hours
		timeLimit = 30 * 60 * 1000;
		branchAndPrice.setTimeLimit(timeLimit);
		branchAndPrice.applyBranchAndPrice();

		// Write output.
		Writer.write(Logger.getLogger().getOutput(), "logger_" + instanceName + ".csv");
	}

	public static void printStatistics(SchedulingInstance instance, LocalDate date)
	{
		System.out.println("INSTANCE STATISTICS:");
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
}
