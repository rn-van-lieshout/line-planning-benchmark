package scheduling.columnGeneration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import modelling.Duty;
import modelling.Task;
import optimisation.columnGeneration.columnManagement.AbstractColumnSelector;
import scheduling.SchedulingInstance;
import util.Configuration;
import util.Pair;

public class DisjointSelector extends AbstractColumnSelector<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	@Override
	public List<Pair<SchedulingColumn, Double>> selectColumns(List<Pair<SchedulingColumn, Double>> generatedColumns)
	{
		// Initialise list of duties.
		List<Pair<SchedulingColumn, Double>> columns = new ArrayList<>();
		final double DISJOINT_THRESHOLD = Configuration.getConfiguration().getDoubleProperty("DISJOINT_THRESHOLD");

		// Sort columns.
		generatedColumns.sort(new ReducedCostComparator());
		generatedColumns = generatedColumns.subList(0, Math.min(generatedColumns.size(), 5000));

		candidateLoop:
		for (int i = 0; i < generatedColumns.size(); i++)
		{
			// Similarity criterion.
			DutyColumn column = (DutyColumn) generatedColumns.get(i).getKey();
			Duty duty = column.getDuty();
			for (Pair<SchedulingColumn, Double> otherColumn : columns)
			{
				Duty otherDuty = ((DutyColumn) otherColumn.getKey()).getDuty();
				if (computeSimilarity(duty, otherDuty) > DISJOINT_THRESHOLD)
				{
					continue candidateLoop;
				}
			}
			columns.add(generatedColumns.get(i));
		}
		return columns;
	}

	private static double computeSimilarity(Duty firstDuty, Duty secondDuty)
	{
		Set<Task> tasks = new LinkedHashSet<>();
		tasks.addAll(firstDuty.getTasks());
		tasks.addAll(secondDuty.getTasks());
		int numTasks = tasks.size();

		int numTasksShared = 0;
		for (Task task : firstDuty.getTasks())
		{
			if (secondDuty.getTasks().contains(task))
			{
				numTasksShared++;
			}
		}
		double similarity = (double) numTasksShared / numTasks;
		return similarity;
	}

	private class ReducedCostComparator implements Comparator<Pair<SchedulingColumn, Double>>
	{
		@Override
		public int compare(Pair<SchedulingColumn, Double> o1, Pair<SchedulingColumn, Double> o2)
		{
			return Double.compare(o1.getValue(), o2.getValue());
		}
	}
}
