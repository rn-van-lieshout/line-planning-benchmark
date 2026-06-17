package scheduling.branching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modelling.Connection;
import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleConnection
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleConnection(double priority)
	{
		super(priority);
	}

	@Override
	public List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> getBranchingCandidates(
			BAPNode<SchedulingInstance, SchedulingColumn, PricingProblem> parent)
	{
		// Precision.
		final double PRECISION = Configuration.getConfiguration().getDoubleProperty("PRECISION");

		// Retrieve connection values.
		Map<Connection, Double> connectionMap = new LinkedHashMap<>();
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			for (Connection connection : dutyColumn.getDuty().getConnections())
			{
				if (!connectionMap.containsKey(connection))
				{
					connectionMap.put(connection, 0.0);
				}
				connectionMap.put(connection, connectionMap.get(connection) + value);
			}
		}

		// Construct list of branching candidates.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		for (Connection connection : connectionMap.keySet())
		{
			double value = connectionMap.get(connection);
			double fractionalValue = Math.abs(Math.rint(value) - value);
			if (fractionalValue > PRECISION)
			{
				// Add branching candidate.
				Set<Connection> connections = new LinkedHashSet<>();
				connections.add(connection);
				int boundHigh = (int) Math.ceil(value);
				int boundLow = (int) Math.floor(value);
				BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
						fractionalValue);
				candidate.addBranchingDecision(new BranchingDecisionConnections(true, boundHigh, connections));
				candidate.addBranchingDecision(new BranchingDecisionConnections(false, boundLow, connections));
				candidates.add(candidate);
			}
		}
		return candidates;
	}
}
