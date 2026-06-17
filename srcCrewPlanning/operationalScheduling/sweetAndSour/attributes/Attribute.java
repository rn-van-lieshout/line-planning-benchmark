package operationalScheduling.sweetAndSour.attributes;

import modelling.Duty;
import operationalScheduling.columnGeneration.OperationalPricingProblem;

public abstract class Attribute
{
	private final String name;
	private final double minimum;
	private final double maximum;
	protected final boolean isUpperBound;
	protected final double bound;

	public Attribute(String name, double minimum, double maximum, boolean isUpperBound, double bound)
	{
		this.name = name;
		this.minimum = minimum;
		this.maximum = maximum;
		this.isUpperBound = isUpperBound;
		this.bound = bound;
	}

	public String getName()
	{
		return name;
	}

	public double getMinimum()
	{
		return minimum;
	}
	
	public double getMaximum()
	{
		return maximum;
	}
	
	public boolean isUpperBound()
	{
		return isUpperBound;
	}

	public double getBound()
	{
		return bound;
	}

	public boolean satisfiesBound(double score)
	{
		return isUpperBound ? score <= bound : score >= bound;
	}

	public abstract double evaluateDuty(Duty duty);

	public abstract int getAttributeVectorLength();

	public abstract void modifyPricingProblem(OperationalPricingProblem pricingProblem, double score);

	public abstract void addDuty(Duty duty, AttributeVector attributeVector);

	public abstract void removeDuty(Duty duty, AttributeVector attributeVector);

	public abstract double evaluateAttributeVector(AttributeVector attributeVector);
}
