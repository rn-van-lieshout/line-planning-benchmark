package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.DailyEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.Attribute;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import util.Writer;

public class TemporalEvaluationModule extends DailyEvaluationModule
{
	int time = 1;

	public TemporalEvaluationModule(AttributeVectorStorage attributeStorage)
	{
		sb = new StringBuilder();
		sb.append("t;");
		for (Attribute attribute : attributeStorage.getAttributes())
		{
			sb.append(attribute.getName() + ";");
		}
		sb.append("\n");
	}

	@Override
	public void performDailyEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage, LocalDate day)
	{
		sb.append(time + ";");
		for (Attribute attribute : attributeStorage.getAttributes())
		{
			int satisfied = 0;
			int total = 0;
			for (DutyRoster roster : rosters)
			{
				if (roster.getDuties().size() > 0)
				{
					total++;
					double score = attribute.evaluateAttributeVector(
							attributeStorage.getAttributeVector(attribute, roster.getCrewID()));
					if (attribute.satisfiesBound(score))
					{
						satisfied++;
					}
				}
			}

			double score = (double) 100.0 * satisfied / total;
			sb.append(Writer.formatDouble(score) + ";");
		}

		time++;
		sb.append("\n");
	}
}
