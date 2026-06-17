package modelling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DutyRoster extends TemplateRoster
{
	private final Map<LocalDate, Duty> dutyMap;

	public DutyRoster(int crewID, Station crewBase)
	{
		super(crewID, crewBase);
		this.dutyMap = new LinkedHashMap<>();
	}

	public void addDuty(LocalDate day, Duty duty)
	{
		dutyMap.put(day, duty);
	}

	public boolean hasDuty(LocalDate day)
	{
		return dutyMap.containsKey(day);
	}

	public Duty getDuty(LocalDate day)
	{
		return dutyMap.get(day);
	}

	public Collection<Duty> getDuties()
	{
		return dutyMap.values();
	}

	@Override
	public String toString()
	{
		String result = "ENTRIES: " + dutyMap.size();
		for (Entry<LocalDate, Duty> entry : dutyMap.entrySet())
		{
			result += "\n" + templateMap.get(entry.getKey());
			result += "\n" + entry.getValue();
		}
		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dutyMap == null) ? 0 : dutyMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DutyRoster other = (DutyRoster) obj;
		if (dutyMap == null)
		{
			if (other.dutyMap != null) return false;
		}
		else if (!dutyMap.equals(other.dutyMap)) return false;
		return true;
	}
}
