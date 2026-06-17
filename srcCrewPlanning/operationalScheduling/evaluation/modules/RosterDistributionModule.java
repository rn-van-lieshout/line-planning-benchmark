package operationalScheduling.evaluation.modules;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import modelling.DutyRoster;
import modelling.Task;
import operationalScheduling.evaluation.GlobalEvaluationModule;
import operationalScheduling.sweetAndSour.attributes.Attribute;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import operationalScheduling.sweetAndSour.attributes.DutyLength;
import util.Writer;

public class RosterDistributionModule implements GlobalEvaluationModule
{
	@Override
	public String performFinalEvaluation(List<DutyRoster> rosters, Map<LocalDate, Map<Integer, Task>> taskMap,
			AttributeVectorStorage attributeStorage)
	{
		// Store output.
		StringBuilder sb = new StringBuilder();
		sb.append("rosterID;station;numDuties");
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
				sb.append(roster.getCrewID() + ";" + roster.getCrewBase().getName() + ";" + roster.getDuties().size());

				for (Attribute attribute : attributeStorage.getAttributes())
				{
					double score = attribute.evaluateAttributeVector(
							attributeStorage.getAttributeVector(attribute, roster.getCrewID()));
					if (attribute instanceof DutyLength)
					{
						sb.append(";" + Writer.formatDouble(score / 3600.0));
					}
					else
					{
						sb.append(";" + Writer.formatDouble(score));
					}
					sb.append(";" + (attribute.satisfiesBound(score) ? 1 : 0));
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}

}
