package util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import modelling.Connection;
import modelling.Task;
import modelling.Template;

public class MapUtil
{
	public static Map<Integer, Task> createTaskMap(Collection<Task> objects)
	{
		Map<Integer, Task> map = new LinkedHashMap<>();
		for (Task object : objects)
		{
			map.put(object.getIndex(), object);
		}
		return map;
	}
	
	public static Map<Integer, Connection> createConnectionMap(Collection<Connection> objects)
	{
		Map<Integer, Connection> map = new LinkedHashMap<>();
		for (Connection object : objects)
		{
			map.put(object.getIndex(), object);
		}
		return map;
	}
	
	public static Map<Integer, Template> createTemplateMap(Collection<Template> objects)
	{
		Map<Integer, Template> map = new LinkedHashMap<>();
		for (Template object : objects)
		{
			map.put(object.getIndex(), object);
		}
		return map;
	}
}
