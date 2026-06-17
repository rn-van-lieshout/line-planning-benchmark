package operationalScheduling.evaluation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;

public interface GlobalEvaluationModule
{
	public abstract String performFinalEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage);
}
