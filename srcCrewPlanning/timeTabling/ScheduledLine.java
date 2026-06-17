package timeTabling;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import linePlanning.instance.Line;
import linePlanning.instance.Track;

public class ScheduledLine
{
	private final Line line;
	private final int startTime;
	private final int endTime;
	private final Map<Track, Integer> departureTimes;
	private final Map<Track, Integer> arrivalTimes;

	public ScheduledLine(Line line, int startTime, int endTime, Map<Track, Integer> departureTimes,
			Map<Track, Integer> arrivalTimes)
	{
		this.line = line;
		this.startTime = startTime;
		this.endTime = endTime;
		this.departureTimes = departureTimes;
		this.arrivalTimes = arrivalTimes;
	}

	public Line getLine()
	{
		return line;
	}

	public int getStartTime()
	{
		return startTime;
	}

	public int getEndTime()
	{
		return endTime;
	}

	public int getDepartureTime(Track arc)
	{
		return departureTimes.get(arc);
	}

	public int getArrivalTime(Track arc)
	{
		return arrivalTimes.get(arc);
	}
	
	public Set<ScheduledTrack> getScheduledTracks()
	{
		Set<ScheduledTrack> scheduledTracks = new LinkedHashSet<>();
		for (Track track : departureTimes.keySet())
		{
			ScheduledTrack scheduledTrack = new ScheduledTrack(line, track, departureTimes.get(track), arrivalTimes.get(track));
			scheduledTracks.add(scheduledTrack);
		}
		return scheduledTracks;
	}

	public ScheduledLine shiftScheduledLine(int shift)
	{
		int newStartTime = startTime + shift;
		int newEndTime = endTime + shift;
		Map<Track, Integer> newDepartureTimes = new LinkedHashMap<>();
		Map<Track, Integer> newArrivalTimes = new LinkedHashMap<>();
		for (Track arc : departureTimes.keySet())
		{
			newDepartureTimes.put(arc, departureTimes.get(arc) + shift);
			newArrivalTimes.put(arc, arrivalTimes.get(arc) + shift);
		}
		return new ScheduledLine(line, newStartTime, newEndTime, newDepartureTimes, newArrivalTimes);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arrivalTimes == null) ? 0 : arrivalTimes.hashCode());
		result = prime * result + ((departureTimes == null) ? 0 : departureTimes.hashCode());
		result = prime * result + endTime;
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		result = prime * result + startTime;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ScheduledLine other = (ScheduledLine) obj;
		if (arrivalTimes == null)
		{
			if (other.arrivalTimes != null) return false;
		}
		else if (!arrivalTimes.equals(other.arrivalTimes)) return false;
		if (departureTimes == null)
		{
			if (other.departureTimes != null) return false;
		}
		else if (!departureTimes.equals(other.departureTimes)) return false;
		if (endTime != other.endTime) return false;
		if (line == null)
		{
			if (other.line != null) return false;
		}
		else if (!line.equals(other.line)) return false;
		if (startTime != other.startTime) return false;
		return true;
	}
}
