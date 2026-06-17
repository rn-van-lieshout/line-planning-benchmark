package operationalScheduling.columnGeneration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleMultipleDutyDiving
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final List<LocalDate> fixingDays;

	public BranchingRuleMultipleDutyDiving(double priority, List<LocalDate> fixingDays)
	{
		super(priority);
		this.fixingDays = fixingDays;
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Select highest value duty.
		double highestValue = 0;
		DutyColumn highestValueDuty = null;
		Set<DutyColumn> dutyColumns = new LinkedHashSet<>();

		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			if (!(column instanceof DutyColumn))
			{
				continue;
			}

			DutyColumn dutyColumn = (DutyColumn) column;
			if (!fixingDays.contains(dutyColumn.getDuty().getTemplate().getDay()))
			{
				continue;
			}

			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			if (value > highestValue && value < 1 - Configuration.getConfiguration().getDoubleProperty("PRECISION"))
			{
				highestValue = value;
				highestValueDuty = dutyColumn;
			}
			if (value >= Configuration.getConfiguration().getDoubleProperty("FIX_THRESHOLD")
					&& value < 1 - Configuration.getConfiguration().getDoubleProperty("PRECISION"))
			{
				dutyColumns.add(dutyColumn);
			}
		}
		if (highestValue < Configuration.getConfiguration().getDoubleProperty("FIX_THRESHOLD"))
		{
			dutyColumns.add(highestValueDuty);
		}

		// Return a single branching candidate.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
				highestValue);
		candidate.addBranchingDecision(new BranchingDecisionMultipleDuty(true, dutyColumns));
		candidates.add(candidate);
		return candidates;
	}
}
