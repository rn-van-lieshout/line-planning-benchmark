package scheduling.columnGeneration.labelling;

import java.util.Map;

import graph.algorithms.spprc.AbstractResourceCompletionTest;
import graph.algorithms.spprc.Label;
import modelling.Connection;
import modelling.Constants;
import modelling.Task;

public class BreakResourceTest extends AbstractResourceCompletionTest<Task, Connection>
{
	private final Map<Integer, Map<Task, Integer>> forwardTimeCompletionMap;
	private final Map<Integer, Map<Task, Integer>> backwardTimeCompletionMap;
	private final Map<Task, Integer> forwardBreakCompletionMap;
	private final Map<Task, Integer> backwardBreakCompletionMap;

	public BreakResourceTest(Map<Integer, Map<Task, Integer>> forwardTimeCompletionMap,
			Map<Integer, Map<Task, Integer>> backwardTimeCompletionMap, Map<Task, Integer> forwardBreakCompletionMap,
			Map<Task, Integer> backwardBreakCompletionMap)
	{
		this.forwardTimeCompletionMap = forwardTimeCompletionMap;
		this.backwardTimeCompletionMap = backwardTimeCompletionMap;
		this.forwardBreakCompletionMap = forwardBreakCompletionMap;
		this.backwardBreakCompletionMap = backwardBreakCompletionMap;
	}

	@Override
	public boolean isFeasible(Label<Task, Connection> label, boolean forward)
	{
		Task nextNode = forward ? label.getArc().getTo() : label.getArc().getFrom();
		int depot = label.getResourceVector()[SchedulingREF.INDEX_DEPOT];
		Map<Task, Integer> timeCompletionMap = forward ? forwardTimeCompletionMap.get(depot)
				: backwardTimeCompletionMap.get(depot);
		Map<Task, Integer> breakCompletionMap = forward ? forwardBreakCompletionMap : backwardBreakCompletionMap;
		if (!timeCompletionMap.containsKey(nextNode))
		{
			return true;
		}

		// First case: We move to the nearest break.
		int timeWithoutBreak = breakCompletionMap.get(nextNode)
				+ label.getResourceVector()[SchedulingREF.INDEX_WORKLOAD];

		// Second case: We already had a break, and go to the depot without a new break.
		if (label.getResourceVector()[SchedulingREF.INDEX_HAD_BREAK] == 1)
		{
			int timeAfterBreak = label.getResourceVector()[SchedulingREF.INDEX_WITHOUT_BREAK]
					+ timeCompletionMap.get(nextNode);
			timeWithoutBreak = Math.min(timeWithoutBreak, timeAfterBreak);
		}
		return timeWithoutBreak <= Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK;
	}
}
