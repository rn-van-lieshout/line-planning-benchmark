package scheduling.columnGeneration.labelling;

import java.util.Map;

import graph.algorithms.spprc.AbstractResourceCompletionTest;
import graph.algorithms.spprc.Label;
import modelling.Connection;
import modelling.Constants;
import modelling.Task;

public class LengthResourceTest extends AbstractResourceCompletionTest<Task, Connection>
{
	private final Map<Integer, Map<Task, Integer>> forwardCompletionBoundMap;
	private final Map<Integer, Map<Task, Integer>> backwardCompletionBoundMap;

	public LengthResourceTest(Map<Integer, Map<Task, Integer>> forwardCompletionBoundMap,
			Map<Integer, Map<Task, Integer>> backwardCompletionBoundMap)
	{
		this.forwardCompletionBoundMap = forwardCompletionBoundMap;
		this.backwardCompletionBoundMap = backwardCompletionBoundMap;
	}

	@Override
	public boolean isFeasible(Label<Task, Connection> label, boolean forward)
	{
		Task nextNode = forward ? label.getArc().getTo() : label.getArc().getFrom();
		int depot = label.getResourceVector()[SchedulingREF.INDEX_DEPOT];
		Map<Task, Integer> completionBoundMap = forward ? forwardCompletionBoundMap.get(depot)
				: backwardCompletionBoundMap.get(depot);
		if (!completionBoundMap.containsKey(nextNode))
		{
			return true;
		}
		int dutyLength = label.getResourceVector()[SchedulingREF.INDEX_WORKLOAD] + completionBoundMap.get(nextNode);

		// Check if the duty length is allowed.
		return dutyLength <= Constants.MAXIMUM_DUTY_LENGTH;
	}
}
