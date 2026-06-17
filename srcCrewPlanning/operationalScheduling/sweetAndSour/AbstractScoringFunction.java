package operationalScheduling.sweetAndSour;

import operationalScheduling.sweetAndSour.attributes.Attribute;

public abstract class AbstractScoringFunction
{
	public abstract double getPenalty(Attribute attribute, double score);
}
