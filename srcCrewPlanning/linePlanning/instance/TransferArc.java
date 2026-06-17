package linePlanning.instance;

import modelling.Station;

public class TransferArc extends CGNArc
{
	private final Line from;
	private final Line to;
	private final Station station;
	private int transferTime;
	private final int transferPenalty;

	public TransferArc(Line from, Line to, Station station, int transferTime, int transferPenalty)
	{
		this.from = from;
		this.to = to;
		this.station = station;
		this.transferTime = transferTime;
		this.transferPenalty = transferPenalty;
	}

	public Line getFrom()
	{
		return from;
	}

	public Line getTo()
	{
		return to;
	}

	public Station getStation()
	{
		return station;
	}

	@Override
	public int getDuration()
	{
		return transferTime + transferPenalty;
	}

	public int getTransferTime()
	{
		return transferTime;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((station == null) ? 0 : station.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + transferPenalty;
		result = prime * result + transferTime;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TransferArc other = (TransferArc) obj;
		if (station == null)
		{
			if (other.station != null) return false;
		}
		else if (!station.equals(other.station)) return false;
		if (from == null)
		{
			if (other.from != null) return false;
		}
		else if (!from.equals(other.from)) return false;
		if (to == null)
		{
			if (other.to != null) return false;
		}
		else if (!to.equals(other.to)) return false;
		if (transferPenalty != other.transferPenalty) return false;
		if (transferTime != other.transferTime) return false;
		return true;
	}

	public void setTransferTime(int transferTime)
	{
		this.transferTime = transferTime;
	}
}
