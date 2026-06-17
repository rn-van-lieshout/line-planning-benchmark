package modelling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateRoster
{
	protected int crewID;
	protected final Station crewBase;
	protected final Map<LocalDate, Template> templateMap;
	protected int numWeeks = 0;

	public TemplateRoster(int crewID, Station crewBase)
	{
		this.crewID = crewID;
		this.crewBase = crewBase;
		this.templateMap = new LinkedHashMap<>();
	}

	public int getCrewID()
	{
		return crewID;
	}

	public void setCrewID(int crewID)
	{
		this.crewID = crewID;
	}

	public Station getCrewBase()
	{
		return crewBase;
	}

	public void setNumWeeks(int numWeeks)
	{
		this.numWeeks = numWeeks;
	}

	public int getNumWeeks()
	{
		return numWeeks;
	}

	public void addTemplate(LocalDate day, Template template)
	{
		templateMap.put(day, template);
	}

	public boolean hasTemplate(LocalDate day)
	{
		return templateMap.containsKey(day);
	}

	public Template getTemplate(LocalDate day)
	{
		return templateMap.get(day);
	}

	public Collection<Template> getTemplates()
	{
		return templateMap.values();
	}

	public void removeTemplate(LocalDate date)
	{
		templateMap.remove(date);
	}

	@Override
	public String toString()
	{
		String result = "ENTRIES: " + templateMap.size();
		for (Template template : templateMap.values())
		{
			result += "\n" + template;
		}
		return result;
	}
}
