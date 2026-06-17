package operationalScheduling.sweetAndSour.attributes;

import java.util.Arrays;

public class AttributeVector
{
	private int numDuties;
	private final double[] values;

	public AttributeVector(int size)
	{
		this.numDuties = 0;
		this.values = new double[size];
	}
	
	public int getNumDuties()
	{
		return numDuties;
	}
	
	public void increaseNumDuties()
	{
		numDuties++;
	}
	
	public void decreaseNumDuties()
	{
		numDuties--;
	}
	
	public void increaseValue(int index, double value)
	{
		values[index] += value;
	}
	
	public void decreaseValue(int index, double value)
	{
		values[index] -= value;
	}

	public double[] getValues()
	{
		return values;
	}

	public double getValue(int index)
	{
		return values[index];
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + numDuties;
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AttributeVector other = (AttributeVector) obj;
		if (numDuties != other.numDuties) return false;
		if (!Arrays.equals(values, other.values)) return false;
		return true;
	}
}
