package scheduling.columnGeneration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import modelling.Duty;
import modelling.Task;
import optimisation.columnGeneration.columnManagement.AbstractColumnSelector;
import scheduling.SchedulingInstance;
import util.Configuration;
import util.Pair;

public class BucketSelector extends AbstractColumnSelector<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	@Override
	public List<Pair<SchedulingColumn, Double>> selectColumns(List<Pair<SchedulingColumn, Double>> generatedColumns)
	{
		// Initialise list of duties.
		List<Pair<SchedulingColumn, Double>> columns = new ArrayList<>();
		final int NUMBER_BUCKETS = Configuration.getConfiguration().getIntProperty("NUMBER_BUCKETS");

		// Sort columns.
		generatedColumns.sort((a, b) -> Double.compare(a.getValue(), b.getValue()));
		generatedColumns = generatedColumns.subList(0, Math.min(generatedColumns.size(), 5000));

		List<Set<Task>> buckets = new ArrayList<>();
		for (int k = 0; k < NUMBER_BUCKETS; k++)
		{
			buckets.add(new LinkedHashSet<>());
		}

		for (int i = 0; i < generatedColumns.size(); i++)
		{
			DutyColumn column = (DutyColumn) generatedColumns.get(i).getKey();
			Duty duty = column.getDuty();
			Set<Task> tasks = duty.getTasks();
			for (int k = 0; k < NUMBER_BUCKETS; k++)
			{
				boolean add = true;
				for (Task task : tasks)
				{
					if (buckets.get(k).contains(task))
					{
						add = false;
						break;
					}
				}
				if (add)
				{
					buckets.get(k).addAll(tasks);
					columns.add(generatedColumns.get(i));
					break;
				}
			}
		}
		return columns;
	}
}