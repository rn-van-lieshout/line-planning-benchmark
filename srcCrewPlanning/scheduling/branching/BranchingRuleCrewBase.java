package scheduling.branching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Station;
import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleCrewBase
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleCrewBase(double priority)
	{
		super(priority);
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve total duty value per crew base.
		Map<Station, Double> dutiesPerCrewBase = new LinkedHashMap<>();
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			Station station = dutyColumn.getDuty().getTemplate().getCrewBase();
			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			if (!dutiesPerCrewBase.containsKey(station))
			{
				dutiesPerCrewBase.put(station, 0.0);
			}
			dutiesPerCrewBase.put(station, dutiesPerCrewBase.get(station) + value);
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		for (Station station : dutiesPerCrewBase.keySet())
		{
			double value = dutiesPerCrewBase.get(station);
			double fractionalValue = Math.abs(Math.rint(value) - value);
			if (fractionalValue > PRECISION)
			{
				// Add branching candidate.
				BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
						fractionalValue);
				candidate.addBranchingDecision(new BranchingDecisionCrewBase(true, (int) Math.ceil(value), station));
				candidate.addBranchingDecision(new BranchingDecisionCrewBase(false, (int) Math.floor(value), station));
				candidates.add(candidate);
			}
		}
		return candidates;
	}
}
