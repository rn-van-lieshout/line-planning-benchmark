package timeTabling;

import java.util.ArrayList;
import java.util.List;

import modelling.Station;

public class ScheduledRollingStockUnit
{
	private final Station startStation;
	private final List<ScheduledLine> scheduledLines;

	public ScheduledRollingStockUnit(Station startStation)
	{
		this.startStation = startStation;
		this.scheduledLines = new ArrayList<>();
	}

	public Station getStartStation()
	{
		return startStation;
	}

	public Station getLastStation()
	{
		if (scheduledLines.isEmpty())
		{
			return startStation;
		}
		return scheduledLines.get(scheduledLines.size() - 1).getLine().getEndStation();
	}

	public int getEndTime()
	{
		if (scheduledLines.isEmpty())
		{
			return 0;
		}
		return scheduledLines.get(scheduledLines.size() - 1).getEndTime();
	}

	public List<ScheduledLine> getScheduledLines()
	{
		return scheduledLines;
	}

	public void addScheduledLine(ScheduledLine scheduledLine)
	{
		scheduledLines.add(scheduledLine);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scheduledLines == null) ? 0 : scheduledLines.hashCode());
		result = prime * result + ((startStation == null) ? 0 : startStation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ScheduledRollingStockUnit other = (ScheduledRollingStockUnit) obj;
		if (scheduledLines == null)
		{
			if (other.scheduledLines != null) return false;
		}
		else if (!scheduledLines.equals(other.scheduledLines)) return false;
		if (startStation == null)
		{
			if (other.startStation != null) return false;
		}
		else if (!startStation.equals(other.startStation)) return false;
		return true;
	}
}
