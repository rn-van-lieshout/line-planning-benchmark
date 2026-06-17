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

public class BranchingRuleDutiesPerHour
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleDutiesPerHour(double priority)
	{
		super(priority);
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve number of duties active in each hour.
		Map<Integer, Double> dutiesPerHour = new LinkedHashMap<>();
		for (int i = 0; i < Constants.HOURS_PER_DAY; i++)
		{
			dutiesPerHour.put(i * Constants.SECONDS_PER_HOUR, 0.0);
		}
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			int startTime = dutyColumn.getDuty().getStartTime();
			int endTime = dutyColumn.getDuty().getEndTime();
			for (int hour : dutiesPerHour.keySet())
			{
				if (startTime < hour + Constants.SECONDS_PER_HOUR && hour <= endTime)
				{
					dutiesPerHour.put(hour, dutiesPerHour.get(hour) + value);
				}
			}
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		for (int hour : dutiesPerHour.keySet())
		{
			double numDuties = dutiesPerHour.get(hour);
			double fractionalValue = Math.abs(Math.rint(numDuties) - numDuties);
			if (fractionalValue > PRECISION)
			{
				// Add branching candidate.
				BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
						fractionalValue);
				candidate.addBranchingDecision(
						new BranchingDecisionDutiesPerHour(true, (int) Math.ceil(numDuties), hour));
				candidate.addBranchingDecision(
						new BranchingDecisionDutiesPerHour(false, (int) Math.floor(numDuties), hour));
				candidates.add(candidate);
			}
		}
		return candidates;
	}
}