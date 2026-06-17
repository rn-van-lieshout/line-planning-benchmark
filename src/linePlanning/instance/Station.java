package linePlanning.instance;

import java.util.Objects;

import linePlanning.PTN.Connection;

public class Station
{
	private final int index;
	private final String name;
	private final String nameFull;
	private final boolean terminalIC;
	private final boolean terminalSPR;
	private final int type; // 0=Sprinter, 1=Twijfel, 2=IC,
	private final boolean inRandstad;
	private final double latitude;
	private final double longitude;

	public Station(int index, String name, String nameFull, boolean terminalIC, boolean terminalSPR, int type,
			boolean inRandstad, double latitude, double longitude)
	{
		super();

		this.index = index;
		this.name = name;
		this.nameFull = nameFull;
		this.terminalIC = terminalIC;
		this.terminalSPR = terminalSPR;
		this.type = type;
		this.inRandstad = inRandstad;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public int getIndex()
	{
		return index;
	}

	public String getName()
	{
		return name;
	}

	public String getNameFull()
	{
		return nameFull;
	}

	public boolean isTerminalIC()
	{
		return terminalIC;
	}

	public boolean isTerminalSPR()
	{
		return terminalSPR;
	}

	public int getType()
	{
		return type;
	}

	public boolean isInRandstad()
	{
		return inRandstad;
	}

	public double getLatitude()
	{
		return latitude;
	}

	public double getLongitude()
	{
		return longitude;
	}

	@Override
	public String toString()
	{
		String result = index + " " + name + " " + nameFull.replaceAll("\\s+", "") + " " + terminalIC + " "
				+ terminalSPR + " " + type + " " + inRandstad + " " + latitude + " " + longitude;
		return result;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(index);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Station other = (Station) obj;
		return inRandstad == other.inRandstad && index == other.index
				&& Double.doubleToLongBits(latitude) == Double.doubleToLongBits(other.latitude)
				&& Double.doubleToLongBits(longitude) == Double.doubleToLongBits(other.longitude)
				&& Objects.equals(name, other.name) && Objects.equals(nameFull, other.nameFull)
				&& terminalIC == other.terminalIC && terminalSPR == other.terminalSPR && type == other.type;
	}

	public boolean canServeAsTerminal(Connection connection)
	{
		if (connection.getType() == LinePlanningParameters.SPRINTER && terminalSPR)
		{
			return true;
		}
		if (connection.getType() == LinePlanningParameters.INTERCITY && terminalIC)
		{
			return true;
		}
		return false;
	}
}