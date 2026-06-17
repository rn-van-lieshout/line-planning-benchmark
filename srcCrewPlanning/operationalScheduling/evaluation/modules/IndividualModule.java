package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.DailyEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.Attribute;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import operationalScheduling.sweetAndSour.attributes.DutyLength;
import util.Writer;

public class IndividualModule extends DailyEvaluationModule
{
	int time = 1;
	int crewID;

	public IndividualModule(AttributeVectorStorage attributeStorage, int crewID)
	{
		this.crewID = crewID;

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
			for (DutyRoster roster : rosters)
			{
				if (roster.getCrewID() == crewID)
				{
					double score = attribute.evaluateAttributeVector(
							attributeStorage.getAttributeVector(attribute, roster.getCrewID()));
					if (attribute instanceof DutyLength)
					{
						sb.append(Writer.formatDouble(score / 3600.0) + ";");
					}
					else
					{
						sb.append(Writer.formatDouble(score) + ";");
					}
					break;
				}
			}
		}

		// Increment days.
		time++;
		sb.append("\n");
	}
}
