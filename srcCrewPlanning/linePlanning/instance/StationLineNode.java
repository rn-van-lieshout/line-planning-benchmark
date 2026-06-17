package linePlanning.instance;

import modelling.Station;

public class StationLineNode extends StationNode
{
	private final Line line;
	private final boolean isArrival;

	public StationLineNode(Station station, Line line, boolean isArrival)
	{
		super(station);

		this.line = line;
		this.isArrival = isArrival;
	}

	public Line getLine()
	{
		return line;
	}

	public boolean isArrival()
	{
		return isArrival;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isArrival ? 1231 : 1237);
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		StationLineNode other = (StationLineNode) obj;
		if (isArrival != other.isArrival) return false;
		if (line == null)
		{
			if (other.line != null) return false;
		}
		else if (!line.equals(other.line)) return false;
		return true;
	}
}
