package modelling;

public class Station
{
	private final int index;
	private final String name;

	public Station(int index, String name)
	{
		this.index = index;
		this.name = name;
	}

	public int getIndex()
	{
		return index;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return index + " " + name;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Station other = (Station) obj;
		if (index != other.index) return false;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		return true;
	}
}
