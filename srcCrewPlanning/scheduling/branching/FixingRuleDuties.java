package scheduling.branching;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Task;
import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class FixingRuleDuties extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final SchedulingInstance instance;
	private final boolean isPartition;

	public FixingRuleDuties(double priority, SchedulingInstance instance, boolean isPartition)
	{
		super(priority);

		this.instance = instance;
		this.isPartition = isPartition;
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve total duty value.
		Set<DutyColumn> dutiesToFix = new LinkedHashSet<>();
		DutyColumn highestValueDuty = null;
		double highestValue = 0;
		for (Entry<SchedulingColumn, Double> entry : parent.getSolution().getColumnMap().entrySet())
		{
			DutyColumn dutyColumn = (DutyColumn) entry.getKey();
			double value = entry.getValue();
			if (value >= PRECISION && value > highestValue && value < 1 - PRECISION)
			{
				highestValueDuty = dutyColumn;
				highestValue = value;
			}
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		if (highestValueDuty != null)
		{
			// Add branching candidate.
			BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
					highestValue);
			dutiesToFix.add(highestValueDuty);
			candidate.addBranchingDecision(new BranchingDecisionDuties(true, dutiesToFix));
			candidates.add(candidate);
		}

		// If we use partition constraints, we can remove all arcs connecting nodes to
		// be covered from pricing graph.
		if (isPartition)
		{
			for (DutyColumn column : dutiesToFix)
			{
				PricingGraph graph = instance.getDailyPricingGraph(column.getDuty().getTemplate().getDay());
				for (Task task : column.getDuty().getTasks())
				{
					Set<DirectedGraphArc<Task, Connection>> arcs = new LinkedHashSet<>();
					arcs.addAll(graph.getInArcs(task));
					arcs.addAll(graph.getOutArcs(task));
					for (DirectedGraphArc<Task, Connection> arc : arcs)
					{
						graph.removeArc(arc);
					}
				}
			}
		}
		return candidates;
	}
}