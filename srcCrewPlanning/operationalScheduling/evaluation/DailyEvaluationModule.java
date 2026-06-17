package operationalScheduling.evaluation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;

public abstract class DailyEvaluationModule
{
	protected StringBuilder sb;

	public abstract void performDailyEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage, LocalDate day);

	public String printEvaluation()
	{
		return sb.toString();
	}
}
