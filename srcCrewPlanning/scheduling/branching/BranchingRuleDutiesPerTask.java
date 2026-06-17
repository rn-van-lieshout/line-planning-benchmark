package scheduling.branching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Task;
import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleDutiesPerTask
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleDutiesPerTask(double priority)
	{
		super(priority);
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve total duty value per task.
		Map<Task, Double> dutiesPerTask = new LinkedHashMap<>();
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			for (Task task : dutyColumn.getDuty().getTasks())
			{
				if (!dutiesPerTask.containsKey(task))
				{
					dutiesPerTask.put(task, 0.0);
				}
				dutiesPerTask.put(task, dutiesPerTask.get(task) + value);
			}
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		for (Task task : dutiesPerTask.keySet())
		{
			double value = dutiesPerTask.get(task);
			double fractionalValue = Math.abs(Math.rint(value) - value);
			if (fractionalValue > PRECISION)
			{
				// Add branching candidate.
				BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
						fractionalValue);
				candidate.addBranchingDecision(new BranchingDecisionDutiesPerTask(true, (int) Math.ceil(value), task));
				candidate
						.addBranchingDecision(new BranchingDecisionDutiesPerTask(false, (int) Math.floor(value), task));
				candidates.add(candidate);
			}
		}
		return candidates;
	}
}