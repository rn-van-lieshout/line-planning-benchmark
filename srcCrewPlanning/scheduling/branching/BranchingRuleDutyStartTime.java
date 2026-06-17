package scheduling.branching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Constants;
import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleDutyStartTime
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleDutyStartTime(double priority)
	{
		super(priority);
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve number of duties starting in each hour.
		Map<Integer, Double> dutiesStartingPerHour = new LinkedHashMap<>();
		for (int i = 0; i < Constants.HOURS_PER_DAY; i++)
		{
			dutiesStartingPerHour.put(i * Constants.SECONDS_PER_HOUR, 0.0);
		}
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			int startTime = dutyColumn.getDuty().getStartTime();
			int hour = (startTime / Constants.SECONDS_PER_HOUR) * Constants.SECONDS_PER_HOUR;
			if (dutiesStartingPerHour.containsKey(hour))
			{
				dutiesStartingPerHour.put(hour, dutiesStartingPerHour.get(hour) + value);
			}
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		for (int hour : dutiesStartingPerHour.keySet())
		{
			double numDuties = dutiesStartingPerHour.get(hour);
			double fractionalValue = Math.abs(Math.rint(numDuties) - numDuties);
			if (fractionalValue > PRECISION)
			{
				BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
						fractionalValue);
				candidate.addBranchingDecision(
						new BranchingDecisionDutyStartTime(true, (int) Math.ceil(numDuties), hour));
				candidate.addBranchingDecision(
						new BranchingDecisionDutyStartTime(false, (int) Math.floor(numDuties), hour));
				candidates.add(candidate);
			}
		}
		return candidates;
	}
}
