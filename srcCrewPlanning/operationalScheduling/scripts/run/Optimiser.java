package operationalScheduling.scripts.run;

import java.time.LocalDate;
import java.util.List;

import ilog.concert.IloException;
import operationalScheduling.columnGeneration.BranchingRuleMultipleDutyDiving;
import operationalScheduling.columnGeneration.OperationalPricingRoutine;
import operationalScheduling.sweetAndSour.AbstractScoringFunction;
import operationalScheduling.sweetAndSour.LinearScoringFunction;
import optimisation.BAP.BranchAndPrice;
import optimisation.BAP.NodeComparators.BoundComparator;
import optimisation.columnGeneration.ColumnGeneration;
import scheduling.SchedulingInstance;
import scheduling.SchedulingMaster;
import scheduling.SchedulingMasterBuilder;
import scheduling.SchedulingSolution;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import scheduling.columnGeneration.TemplateDisjointSelector;
import scheduling.columnGeneration.labelling.LabellingSolver;
import util.Configuration;

public class Optimiser
{
	public static SchedulingSolution applyOptimiser(SchedulingInstance instance, List<LocalDate> fixingDays)
			throws IloException
	{
		// Initialise master problem.
		SchedulingMaster masterProblem = new SchedulingMasterBuilder().setProblem(instance).build();

		// Initialise scoring function.
		AbstractScoringFunction scoringFunction = null;
		switch (Configuration.getConfiguration().getStringProperty("SCORING_FUNCTION"))
		{
		case "LINEAR":
			scoringFunction = new LinearScoringFunction();
			break;
		default:
			break;
		}

		// Column generation settings. We add completion bounds to use in the labelling
		// solver.
		OperationalPricingRoutine pricingRoutine = new OperationalPricingRoutine(scoringFunction);
		LabellingSolver labelling = new LabellingSolver();
		TemplateDisjointSelector disjointSelector = new TemplateDisjointSelector();
		ColumnGeneration<SchedulingInstance, SchedulingColumn, PricingProblem> columnGeneration = new ColumnGeneration<SchedulingInstance, SchedulingColumn, PricingProblem>(
				pricingRoutine, labelling, labelling, disjointSelector);

		// Branch-and-price settings.
		BranchAndPrice<SchedulingInstance, SchedulingColumn, PricingProblem> branchAndPrice = new BranchAndPrice<SchedulingInstance, SchedulingColumn, PricingProblem>(
				new BoundComparator<SchedulingInstance, SchedulingColumn, PricingProblem>(), instance, masterProblem,
				columnGeneration);

		// Fixing rule.
		branchAndPrice.addBranchingRule(new BranchingRuleMultipleDutyDiving(0, fixingDays));

		// Solve with branch-and-price.
		branchAndPrice.applyBranchAndPrice();

		// TODO: Print objective.
		System.out.println("Objective: " + branchAndPrice.getUpperBound());

		// Return solution.
		return (SchedulingSolution) branchAndPrice.getBestSolution();
	}
}
