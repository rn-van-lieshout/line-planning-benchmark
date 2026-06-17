package linePlanning.columnGeneration;

import linePlanning.instance.Line;

public class LineColumn extends LinePlanningColumn
{
	private final Line line;

	public LineColumn(Line line)
	{
		super(line.getCost(), false, true);

		this.line = line;
	}

	public Line getLine()
	{
		return line;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		return result;
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
		LineColumn other = (LineColumn) obj;
		if (line == null)
		{
			if (other.line != null)
				return false;
		}
		else
			if (!line.equals(other.line))
				return false;
		return true;
	}

	@Override
	public String toString()
	{
		return line.toString();
	}
}
