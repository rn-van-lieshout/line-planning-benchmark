package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.DailyEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;

public class CoveredTasksModule extends DailyEvaluationModule
{
	public CoveredTasksModule()
	{
		sb = new StringBuilder();
		sb.append("day;covered;uncovered\n");
	}

	@Override
	public void performDailyEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage, LocalDate day)
	{
		// Check whether all tasks are covered.
		int totalUncovered = 0;
		int totalCovered = 0;

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
		sb.append(day.toString() + ";" + totalCovered + ";" + totalUncovered + "\n");
	}
}
