package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.GlobalEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;

public class GlobalCoveredTasksModule implements GlobalEvaluationModule
{
	@Override
	public String performFinalEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage)
	{
		// Compute number of covered and uncovered tasks.
		int totalUncovered = 0;
		int totalCovered = 0;

		for (LocalDate day : taskMap.keySet())
		{
			Map<Task, Integer> taskCoveredMap = new LinkedHashMap<>();
			for (Task task : taskMap.get(day).values())
			{
				if (task.cover())
				{
					taskCoveredMap.put(task, 0);
				}
			}
			for (DutyRoster roster : rosters)
			{
				if (roster.hasDuty(day))
				{
					for (Task task : roster.getDuty(day).getTasks())
					{
						if (task.cover())
						{
							taskCoveredMap.put(task, taskCoveredMap.get(task) + 1);
						}
					}
				}
			}

			for (Entry<Task, Integer> entry : taskCoveredMap.entrySet())
			{
				if (entry.getValue() == 0)
				{
					totalUncovered++;
				}
				else
				{
					totalCovered++;
				}
			}
		}

		// Output as String.
		StringBuilder sb = new StringBuilder();
		sb.append("covered;uncovered\n");
		sb.append(totalCovered + ";" + totalUncovered + "\n");
		return sb.toString();
	}
}
