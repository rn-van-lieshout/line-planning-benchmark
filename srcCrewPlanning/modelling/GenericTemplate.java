package modelling;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class GenericTemplate
{
	private final int index;
	private final int crewID;
	private final Station crewBase;
	private final DayOfWeek dayOfWeek;
	private final int startTime;
	private final int endTime;

	public GenericTemplate(int index, int crewID, Station crewBase, DayOfWeek dayOfWeek, int startTime, int endTime)
	{
		this.index = index;
		this.crewID = crewID;
		this.crewBase = crewBase;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public int getIndex()
	{
		return index;
	}

	public int getCrewID()
	{
		return crewID;
	}

	public Station getCrewBase()
	{
		return crewBase;
	}

	public DayOfWeek getDayOfWeek()
	{
		return dayOfWeek;
	}

	public int getStartTime()
	{
		return startTime;
	}

	public int getEndTime()
	{
		return endTime;
	}

	public int getRelativeStartTime()
	{
		return startTime % (24 * 3600);
	}

	public int getRelativeEndTime()
	{
		int numDays = Math.floorDiv(startTime, 24 * 3600);
		return endTime - numDays * 24 * 3600;
	}

	public int getWorkload()
	{
		return endTime - startTime;
	}

	public boolean containsTemplate(Template template)
	{
		return crewBase.equals(template.getCrewBase()) && dayOfWeek.equals(template.getDay().getDayOfWeek())
				&& startTime == template.getStartTime() && endTime == template.getEndTime();
	}
	
	public Template getDailyTemplate(LocalDate date)
	{
		return new Template(index, crewID, crewBase, date, startTime, endTime);
	}

	@Override
	public String toString()
	{
		return index + " " + crewID + " " + crewBase.getIndex() + " " + dayOfWeek + " " + startTime + " " + endTime;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((crewBase == null) ? 0 : crewBase.hashCode());
		result = prime * result + crewID;
		result = prime * result + ((dayOfWeek == null) ? 0 : dayOfWeek.hashCode());
		result = prime * result + endTime;
		result = prime * result + index;
		result = prime * result + startTime;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		GenericTemplate other = (GenericTemplate) obj;
		if (crewBase == null)
		{
			if (other.crewBase != null) return false;
		}
		else if (!crewBase.equals(other.crewBase)) return false;
		if (crewID != other.crewID) return false;
		if (dayOfWeek != other.dayOfWeek) return false;
		if (endTime != other.endTime) return false;
		if (index != other.index) return false;
		if (startTime != other.startTime) return false;
		return true;
	}
}
