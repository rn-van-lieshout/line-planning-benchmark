package data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.GenericTemplate;
import modelling.Station;
import modelling.Task;
import modelling.Template;

public class FilterUtil
{
	public static List<DutyRoster> filterRosters(List<Station> crewBases, List<DutyRoster> rosters)
	{
		List<DutyRoster> filteredRosters = new ArrayList<>();
		for (DutyRoster roster : rosters)
		{
			if (!crewBases.contains(roster.getCrewBase()))
			{
				continue;
			}
			filteredRosters.add(roster);
		}
		return filteredRosters;
	}
	
	public static List<GenericTemplate> filterGenericTemplates(List<Station> crewBases, List<GenericTemplate> templates)
	{
		List<GenericTemplate> filteredTemplates = new ArrayList<>();
		for (GenericTemplate template : templates)
		{
			if (crewBases.contains(template.getCrewBase()))
			{
				filteredTemplates.add(template);
			}
		}
		return filteredTemplates;
	}

	public static List<Template> filterTemplates(List<Station> crewBases, List<Template> templates)
	{
		List<Template> filteredTemplates = new ArrayList<>();
		for (Template template : templates)
		{
			if (crewBases.contains(template.getCrewBase()))
			{
				filteredTemplates.add(template);
			}
		}
		return filteredTemplates;
	}

	public static List<Duty> filterDuties(List<Station> crewBases, List<Duty> duties)
	{
		List<Duty> filteredDuties = new ArrayList<>();
		dutyLoop: for (Duty duty : duties)
		{
			Template template = duty.getTemplate();
			for (Station station : crewBases)
			{
				if (!template.getCrewBase().equals(station))
				{
					continue;
				}
				filteredDuties.add(duty);
				continue dutyLoop;
			}
		}
		return filteredDuties;
	}

	public static List<Duty> filterDutiesWithTemplates(List<Template> templates, List<Duty> duties)
	{
		List<Duty> filteredDuties = new ArrayList<>();
		for (Duty duty : duties)
		{
			if (templates.contains(duty.getTemplate()))
			{
				filteredDuties.add(duty);
			}
		}
		return filteredDuties;
	}

	public static List<Task> retrieveTasks(List<Duty> duties)
	{
		Set<Task> taskSet = new LinkedHashSet<>();
		for (Duty duty : duties)
		{
			for (Task task : duty.getTasks())
			{
				taskSet.add(task);
			}
		}
		return new ArrayList<>(taskSet); 
	}

	public static List<Connection> filterConnections(List<Task> tasks, List<Connection> connections)
	{
		Set<Connection> filteredConnections = new LinkedHashSet<>();
		for (Connection connection : connections)
		{
			if (connection.getConnectionType().equals(ConnectionType.SOURCE))
			{
				if (containsTask(tasks, connection.getEndBlock()))
				{
					filteredConnections.add(connection);
				}
			}
			if (connection.getConnectionType().equals(ConnectionType.CONNECTION))
			{
				if (containsTask(tasks, connection.getStartBlock()) && containsTask(tasks, connection.getEndBlock()))
				{
					filteredConnections.add(connection);
				}
			}
			if (connection.getConnectionType().equals(ConnectionType.SINK))
			{
				if (containsTask(tasks, connection.getStartBlock()))
				{
					filteredConnections.add(connection);
				}
			}
		}
		return new ArrayList<>(filteredConnections); 
	}

	private static boolean containsTask(List<Task> tasks, int index)
	{
		for (Task task : tasks)
		{
			if (task.getIndex() == index)
			{
				return true;
			}
		}
		return false;
	}
}
