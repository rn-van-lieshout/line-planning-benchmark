package scheduling.branching;

import java.util.ArrayList;
import java.util.List;

import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleNumberDuties
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleNumberDuties(double priority)
	{
		super(priority);
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve total duty value.
		double numDuties = 0;
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			numDuties += parent.getSolution().getColumnMap().get(dutyColumn);
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		double fractionalValue = Math.abs(Math.rint(numDuties) - numDuties);
		if (fractionalValue > PRECISION)
		{
			// Add branching candidate.
			BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
					fractionalValue);
			candidate.addBranchingDecision(new BranchingDecisionNumberDuties(true, (int) Math.ceil(numDuties)));
			candidate.addBranchingDecision(new BranchingDecisionNumberDuties(false, (int) Math.floor(numDuties)));
			candidates.add(candidate);
		}
		return candidates;
	}
}