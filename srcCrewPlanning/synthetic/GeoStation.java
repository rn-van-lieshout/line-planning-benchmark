package synthetic;

import modelling.Station;
import util.Writer;

public class GeoStation extends Station
{
	private double x;
	private double y;
	private double population;
	private boolean isLarge;

	public GeoStation(int index, String name, double x, double y, double population, boolean isLarge)
	{
		super(index, name);

		this.x = x;
		this.y = y;
		this.population = population;
		this.isLarge = isLarge;
	}

	public double getX()
	{
		return x;
	}

	public void setX(double x)
	{
		this.x = x;
	}

	public double getY()
	{
		return y;
	}

	public void setY(double y)
	{
		this.y = y;
	}

	public double getPopulation()
	{
		return population;
	}

	public void setPopulation(double population)
	{
		this.population = population;
	}

	public boolean isLarge()
	{
		return isLarge;
	}

	public void setLarge(boolean isLarge)
	{
		this.isLarge = isLarge;
	}

	public double getDistance(GeoStation station)
	{
		return Math.sqrt(Math.pow(station.x - x, 2) + Math.pow(station.y - y, 2));
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		String result = "";
		result += super.getIndex() + " " + super.getName() + " " + Writer.formatDouble(x) + " " + Writer.formatDouble(y)
				+ " " + Writer.formatDouble(population) + " " + isLarge;
		return result;
	}
}
