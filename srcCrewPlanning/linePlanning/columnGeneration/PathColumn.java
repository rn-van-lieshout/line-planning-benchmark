package linePlanning.columnGeneration;

import linePlanning.instance.Path;

public class PathColumn extends LinePlanningColumn
{
	private final Path path;

	public PathColumn(Path path)
	{
		super(0, true, false);

		this.path = path;
	}

	public Path getPath()
	{
		return path;
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
		PathColumn other = (PathColumn) obj;
		return other.path.equals(path);
	}

	@Override
	public int hashCode()
	{
		return path.hashCode();
	}

	@Override
	public String toString()
	{
		return path.toString();
	}
}
