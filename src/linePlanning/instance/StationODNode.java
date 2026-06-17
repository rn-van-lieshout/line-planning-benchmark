package linePlanning.instance;

public class StationODNode extends StationNode
{
	private final boolean isOrigin;

	public StationODNode(Station station, boolean isOrigin)
	{
		super(station);

		this.isOrigin = isOrigin;
	}

	public boolean isOrigin()
	{
		return isOrigin;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isOrigin ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		StationODNode other = (StationODNode) obj;
		if (isOrigin != other.isOrigin)
			return false;
		return true;
	}
}
