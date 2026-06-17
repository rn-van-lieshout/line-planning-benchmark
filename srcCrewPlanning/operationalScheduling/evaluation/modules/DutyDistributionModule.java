package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.Duty;
import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.GlobalEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.Attribute;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import util.Writer;

public class DutyDistributionModule implements GlobalEvaluationModule
{
	@Override
	public String performFinalEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage)
	{
		// Store output.
		StringBuilder sb = new StringBuilder();
		sb.append("dutyID");
		for (Attribute attribute : attributeStorage.getAttributes())
		{
			sb.append(";" + attribute.getName() + ";" + attribute.getName() + "Satisfied");
		}
		sb.append("\n");

		// Evaluate rosters.
		for (DutyRoster roster : rosters)
		{
			if (roster.getDuties().size() > 0)
			{
				for (Duty duty : roster.getDuties())
				{
					sb.append(duty.getIndex());
					for (Attribute attribute : attributeStorage.getAttributes())
					{
						double score = attribute.evaluateDuty(duty);
						sb.append(";" + Writer.formatDouble(score));
						sb.append(";" + (attribute.satisfiesBound(score) ? 1 : 0));
					}
					sb.append("\n");
				}
			}
		}

		// Output results.
		return sb.toString();
	}
}
