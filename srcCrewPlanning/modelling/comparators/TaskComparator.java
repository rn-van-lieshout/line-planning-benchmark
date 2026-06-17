package modelling.comparators;

import java.util.Comparator;

import modelling.Task;

public class TaskComparator implements Comparator<Task>
{
	@Override
	public int compare(Task a, Task b)
	{
		if (a.getDay().isEqual(b.getDay()))
		{
			return a.getStartTime() - b.getStartTime();
		}
		return a.getDay().compareTo(b.getDay());
	}
}