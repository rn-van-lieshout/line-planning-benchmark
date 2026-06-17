package timeTabling;

import java.util.Objects;

import linePlanning.instance.Line;
import linePlanning.instance.Track;

public class ScheduledTrack
{
	private final Line line;
	private final Track track;
	private final int departureTime;
	private final int arrivalTime;

	public ScheduledTrack(Line line, Track track, int departureTime, int arrivalTime)
	{
		this.line = line;
		this.track = track;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
	}

	public Line getLine()
	{
		return line;
	}

	public Track getTrack()
	{
		return track;
	}

	public int getDepartureTime()
	{
		return departureTime;
	}

	public int getArrivalTime()
	{
		return arrivalTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(arrivalTime, departureTime, line, track);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScheduledTrack other = (ScheduledTrack) obj;
		return arrivalTime == other.arrivalTime && departureTime == other.departureTime
				&& Objects.equals(line, other.line) && Objects.equals(track, other.track);
	}
}
