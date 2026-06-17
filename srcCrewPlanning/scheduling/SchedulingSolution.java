package scheduling;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modelling.Duty;
import optimisation.columnGeneration.AbstractSolution;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class SchedulingSolution extends AbstractSolution<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	public SchedulingSolution(double objectiveValue, Map<SchedulingColumn, Double> columnMap)
	{
		super(objectiveValue, columnMap);
	}

	public List<Duty> retrieveDuties()
	{
		List<Duty> duties = new ArrayList<>();
		for (SchedulingColumn column : columnMap.keySet())
		{
			// Only retrieve duties.
			if (!(column instanceof DutyColumn))
			{
				continue;
			}
			DutyColumn dutyColumn = (DutyColumn) column;
			duties.add(dutyColumn.getDuty());
		}
		return duties;
	}

	public Map<LocalDate, List<Duty>> retrieveDuties(List<LocalDate> days)
	{
		Map<LocalDate, List<Duty>> dutyMap = new LinkedHashMap<>();
		for (SchedulingColumn column : columnMap.keySet())
		{
			// Only retrieve duties.
			if (!(column instanceof DutyColumn))
			{
				continue;
			}

			// Only do this for the days that are to be fixed.
			DutyColumn dutyColumn = (DutyColumn) column;
			Duty duty = dutyColumn.getDuty();
			LocalDate day = duty.getTemplate().getDay();
			if (!days.contains(day))
			{
				continue;
			}
			if (!dutyMap.containsKey(day))
			{
				dutyMap.put(day, new ArrayList<>());
			}
			dutyMap.get(day).add(duty);
		}
		return dutyMap;
	}
}
