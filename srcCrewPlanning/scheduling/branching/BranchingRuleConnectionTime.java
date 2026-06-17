package scheduling.branching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Connection;
import modelling.Connection.ConnectionType;
import optimisation.BAP.AbstractBranchingRule;
import optimisation.BAP.BAPNode;
import optimisation.BAP.BranchingCandidate;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;

public class BranchingRuleConnectionTime
		extends AbstractBranchingRule<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public BranchingRuleConnectionTime(double priority)
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
		Map<Integer, Map<Connection, Double>> incomingConnectionsPerTask = new LinkedHashMap<>();
		Map<Integer, Map<Connection, Double>> outgoingConnectionsPerTask = new LinkedHashMap<>();
		for (SchedulingColumn column : parent.getSolution().getColumnMap().keySet())
		{
			DutyColumn dutyColumn = (DutyColumn) column;
			double value = parent.getSolution().getColumnMap().get(dutyColumn);
			for (Connection connection : dutyColumn.getDuty().getConnections())
			{
				if (!connection.getConnectionType().equals(ConnectionType.SINK))
				{
					int endBlock = connection.getEndBlock();
					if (!incomingConnectionsPerTask.containsKey(endBlock))
					{
						incomingConnectionsPerTask.put(endBlock, new LinkedHashMap<>());
					}
					if (!incomingConnectionsPerTask.get(endBlock).containsKey(connection))
					{
						incomingConnectionsPerTask.get(endBlock).put(connection, 0.0);
					}
					incomingConnectionsPerTask.get(endBlock).put(connection,
							incomingConnectionsPerTask.get(endBlock).get(connection) + value);
				}

				if (!connection.getConnectionType().equals(ConnectionType.SOURCE))
				{
					int startBlock = connection.getStartBlock();
					if (!outgoingConnectionsPerTask.containsKey(startBlock))
					{
						outgoingConnectionsPerTask.put(startBlock, new LinkedHashMap<>());
					}
					if (!outgoingConnectionsPerTask.get(startBlock).containsKey(connection))
					{
						outgoingConnectionsPerTask.get(startBlock).put(connection, 0.0);
					}
					outgoingConnectionsPerTask.get(startBlock).put(connection,
							outgoingConnectionsPerTask.get(startBlock).get(connection) + value);
				}
			}
		}

		// Construct list of branching candidates for incoming arcs.
		List<BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem>> candidates = new ArrayList<>();
		boolean[] array =
		{ false, true };
		for (boolean incoming : array)
		{
			Map<Integer, Map<Connection, Double>> connectionMap = incoming ? incomingConnectionsPerTask
					: outgoingConnectionsPerTask;
			for (int block : connectionMap.keySet())
			{
				// Sort connections to this block by increasing duration (i.e., increasing
				// transfer time).
				List<Connection> sortedConnections = new ArrayList<>();
				sortedConnections.addAll(connectionMap.get(block).keySet());
				sortedConnections.sort((a, b) -> a.getDuration() - b.getDuration());

				// Find the most fractional connection duration.
				double value = 0;
				double mostFractionalValue = 0;
				double mostFractionalSum = 0;
				int mostFractionalDuration = 0;
				int duration = sortedConnections.get(0).getDuration();
				for (int i = 0; i < sortedConnections.size(); i++)
				{
					Connection connection = sortedConnections.get(i);
					int nextDuration = connection.getDuration();
					if (duration != nextDuration)
					{
						double fractionalValue = Math.abs(Math.rint(value) - value);
						if (fractionalValue > mostFractionalValue)
						{
							mostFractionalValue = fractionalValue;
							mostFractionalSum = value;
							mostFractionalDuration = duration;
						}
					}
					value += connectionMap.get(block).get(connection);
					duration = nextDuration;
					if (i == sortedConnections.size() - 1)
					{
						double fractionalValue = Math.abs(Math.rint(value) - value);
						if (fractionalValue > mostFractionalValue)
						{
							mostFractionalValue = fractionalValue;
							mostFractionalSum = value;
							mostFractionalDuration = duration;
						}
					}
				}
				if (mostFractionalValue > PRECISION)
				{
					// Add branching candidate.
					BranchingCandidate<SchedulingInstance, SchedulingColumn, PricingProblem> candidate = new BranchingCandidate<>(
							mostFractionalValue);
					int boundHigh = (int) Math.ceil(mostFractionalSum);
					int boundLow = (int) Math.floor(mostFractionalSum);
					candidate.addBranchingDecision(new BranchingDecisionConnectionTime(true, boundHigh, block, incoming,
							mostFractionalDuration));
					candidate.addBranchingDecision(new BranchingDecisionConnectionTime(false, boundLow, block, incoming,
							mostFractionalDuration));
					candidates.add(candidate);
				}
			}
		}
		return candidates;
	}
}
