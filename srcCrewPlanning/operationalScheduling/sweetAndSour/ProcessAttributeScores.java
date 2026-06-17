package operationalScheduling.sweetAndSour;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import modelling.DutyRoster;
import operationalScheduling.columnGeneration.OperationalPricingProblem;
import operationalScheduling.sweetAndSour.attributes.Attribute;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;

public class ProcessAttributeScores
{
	public static void processAttributeScores(AbstractScoringFunction scoringFunction,
			OperationalPricingProblem pricingProblem)
	{
		// Retrieve roster.
		DutyRoster roster = pricingProblem.getRoster();

		// Retrieve attributes.
		AttributeVectorStorage storage = AttributeVectorStorage.getAttributeVectorStorage();
		Collection<Attribute> attributes = storage.getAttributes();

		// Compute attribute scores.
		Map<Attribute, Double> penaltyMap = new LinkedHashMap<>();
		for (Attribute attribute : attributes)
		{
			double evaluation = attribute.evaluateAttributeVector(
					storage.getAttributeVector(attribute, roster.getCrewID()));
			penaltyMap.put(attribute,
					scoringFunction.getPenalty(attribute, evaluation));
		}

		// Adjust pricing graph.
		for (Attribute attribute : attributes)
		{
			// No need to place penalty when there is no violation.
			if (penaltyMap.get(attribute) == 0)
			{
				continue;
			}
			attribute.modifyPricingProblem(pricingProblem, penaltyMap.get(attribute));
		}
	}
}
