package linePlanning.instance;

import modelling.Station;

public class DwellArc extends CGNArc
{
	private final Line line;
	private final Station station;
	private int dwellTime;

	public DwellArc(Line line, Station station, int dwellTime)
	{
		this.line = line;
		this.station = station;
		this.dwellTime = dwellTime;
	}

	public Line getLine()
	{
		return line;
	}

	public double getCapacity()
	{
		return line.getCapacity();
	}

	public Station getStation()
	{
		return station;
	}

	public int getDuration()
	{
		return dwellTime;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + dwellTime;
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		result = prime * result + ((station == null) ? 0 : station.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DwellArc other = (DwellArc) obj;
		if (dwellTime != other.dwellTime) return false;
		if (line == null)
		{
			if (other.line != null) return false;
		}
		else if (!line.equals(other.line)) return false;
		if (station == null)
		{
			if (other.station != null) return false;
		}
		else if (!station.equals(other.station)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "DwellArc [line=" + line + ", station=" + station + ", dwellTime=" + dwellTime + "]";
	}

	public void setDwellTime(int dwellTime)
	{
		this.dwellTime = dwellTime;
	}

}
