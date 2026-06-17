package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.GlobalEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;

public class InstanceModule implements GlobalEvaluationModule
{
	@Override
	public String performFinalEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage)
	{
		// Store output.
		StringBuilder sb = new StringBuilder();
		sb.append("days;tasks;rosters;templates\n");

		// Compute characteristics.
		sb.append(taskMap.size());
		int numTasks = 0;
		for (Map<Integer, Task> tasks : taskMap.values())
		{
			numTasks += tasks.size();
		}
		sb.append(";" + numTasks);
		sb.append(";" + rosters.size());
		int numTemplates = 0;
		for (DutyRoster roster : rosters)
		{
			numTemplates += roster.getTemplates().size();
		}
		sb.append(";" + numTemplates);

		// Output results.
		return sb.toString();
	}
}
