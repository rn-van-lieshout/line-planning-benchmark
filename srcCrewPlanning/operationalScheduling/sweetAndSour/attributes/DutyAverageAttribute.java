package operationalScheduling.sweetAndSour.attributes;

import modelling.Duty;

public abstract class DutyAverageAttribute extends Attribute
{
	public DutyAverageAttribute(String name, double minimum, double maximum, boolean isUpperBound, double bound)
	{
		super(name, minimum, maximum, isUpperBound, bound);
	}

	@Override
	public int getAttributeVectorLength()
	{
		return 1;
	}

	@Override
	public void addDuty(Duty duty, AttributeVector attributeVector)
	{
		attributeVector.increaseNumDuties();
		attributeVector.increaseValue(0, evaluateDuty(duty));
	}

	@Override
	public void removeDuty(Duty duty, AttributeVector attributeVector)
	{
		attributeVector.decreaseNumDuties();
		attributeVector.decreaseValue(0, evaluateDuty(duty));
	}

	@Override
	public double evaluateAttributeVector(AttributeVector attributeVector)
	{
		return attributeVector.getNumDuties() > 0
				? (double) attributeVector.getValue(0) / attributeVector.getNumDuties()
				: 0.0;
	}
}
