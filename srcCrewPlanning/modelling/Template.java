package modelling;

import java.time.LocalDate;
import java.util.Objects;

public class Template
{
	private final int index;
	private final int crewID;
	private final Station crewBase;
	private final LocalDate day;

	private final int startTime;
	private final int endTime;
	private final int latestStartTime;
	private final int earliestEndTime;

	public Template(int index, int crewID, Station crewBase, LocalDate day, int startTime, int endTime)
	{
		this.index = index;
		this.crewID = crewID;
		this.crewBase = crewBase;
		this.day = day;
		this.startTime = startTime;
		this.endTime = endTime;

		this.latestStartTime = endTime;
		this.earliestEndTime = startTime;
	}

	public Template(int index, int crewID, Station crewBase, LocalDate day, int startTime, int latestStartTime,
			int earliestEndTime, int endTime)
	{
		this.index = index;
		this.crewID = crewID;
		this.crewBase = crewBase;
		this.day = day;
		this.startTime = startTime;
		this.endTime = endTime;

		this.latestStartTime = latestStartTime;
		this.earliestEndTime = earliestEndTime;
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

	public LocalDate getDay()
	{
		return day;
	}

	public int getStartTime()
	{
		return startTime;
	}

	public int getEndTime()
	{
		return endTime;
	}

	public int getLatestStartTime()
	{
		return latestStartTime;
	}

	public int getEarliestEndTime()
	{
		return earliestEndTime;
	}

	public int getWorkload()
	{
		return endTime - startTime;
	}

	public GenericTemplate getGenericTemplate()
	{
		return new GenericTemplate(index, crewID, crewBase, day.getDayOfWeek(), startTime, endTime);
	}

	@Override
	public String toString()
	{
		return index + " " + crewID + " " + crewBase.getIndex() + " " + day + " " + startTime + " " + latestStartTime
				+ " " + earliestEndTime + " " + endTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(crewBase, crewID, day, earliestEndTime, endTime, index, latestStartTime, startTime);
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
		Template other = (Template) obj;
		return Objects.equals(crewBase, other.crewBase) && crewID == other.crewID && Objects.equals(day, other.day)
				&& earliestEndTime == other.earliestEndTime && endTime == other.endTime && index == other.index
				&& latestStartTime == other.latestStartTime && startTime == other.startTime;
	}
}
