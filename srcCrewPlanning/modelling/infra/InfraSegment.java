package modelling.infra;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class InfraSegment
{
	public static List<InfraSegment> fromFile(Path path) throws IOException
	{
		int index = 1;
		try (BufferedReader reader = Files.newBufferedReader(path))
		{
			List<InfraSegment> result = new ArrayList<>();
			while (true)
			{
				String line = reader.readLine();
				if (line == null)
				{
					break;
				}
				if (line.isBlank() || line.strip().startsWith("#"))
				{
					continue;
				}
				result.add(fromLine(line, index));
				index++;
			}
			return Collections.unmodifiableList(result);
		}
	}

	public static InfraSegment fromLine(String line, int index)
	{
		String[] parts = line.strip().split("\\s+", -1);
		if (parts.length < 3)
		{
			throw new IllegalArgumentException("line = '" + line + "'");
		}
		List<String> divisionLocations = new ArrayList<>();
		for (int i = 3; i < parts.length; ++i)
		{
			divisionLocations.add(parts[i]);
		}
		int distanceInMeters = Integer.parseInt(parts[2]);
		return new InfraSegment(index, parts[0], parts[1], distanceInMeters, divisionLocations);
	}

	public static InfraSegment create(List<String> locations, int distanceInMeters)
	{
		if (locations.size() < 2)
		{
			throw new IllegalArgumentException("Expecting at least 2 stations in " + locations);
		}
		if (locations.size() != new LinkedHashSet<>(locations).size())
		{
			throw new IllegalArgumentException("Duplication in " + locations);
		}

		List<String> tmpLocations = new ArrayList<>(locations);
		if (tmpLocations.get(0).compareTo(tmpLocations.get(tmpLocations.size() - 1)) > 0)
		{
			Collections.reverse(tmpLocations);
		}
		// TODO: What to do here?
		return new InfraSegment(-1,
				tmpLocations.get(0),
				tmpLocations.get(tmpLocations.size() - 1),
				distanceInMeters,
				tmpLocations.subList(1, tmpLocations.size() - 1));
	}

	private final int index;
	private final String firstStation;
	private final String lastStation;
	private final int distanceInMeters;
	private final List<String> divisionLocations;

	public InfraSegment(int index, String firstStation, String lastStation, int distanceInMeters,
			List<String> divisionLocations)
	{
		this.index = index;
		this.firstStation = firstStation;
		this.lastStation = lastStation;
		this.distanceInMeters = distanceInMeters;
		this.divisionLocations = Collections.unmodifiableList(new ArrayList<>(divisionLocations));
	}

	public int getIndex()
	{
		return index;
	}

	public final String getFirstStation()
	{
		return this.firstStation;
	}

	public final String getLastStation()
	{
		return this.lastStation;
	}

	public final int getDistanceInMeters()
	{
		return this.distanceInMeters;
	}

	public final List<String> getDivisionLocations()
	{
		return this.divisionLocations;
	}

	public final String getOtherEndLocation(String station)
	{
		if (this.getFirstStation().equals(station))
		{
			return this.getLastStation();
		}
		if (this.getLastStation().equals(station))
		{
			return this.getFirstStation();
		}
		throw new IllegalStateException(this + " vs " + station);
	}

	public final String toSaveString()
	{
		return this.getFirstStation() + "-" + this.getLastStation();
	}

	public final String toFullSaveString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append(this.getFirstStation() + " " + this.getLastStation() + " " + this.getDistanceInMeters());
		for (String location : this.getDivisionLocations())
		{
			buf.append(" " + location);
		}
		return buf.toString();
	}

	@Override
	public String toString()
	{
		return "(" + this.getFirstStation() + "-" + this.getLastStation() + "," + this.getDistanceInMeters() + ")";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + distanceInMeters;
		result = prime * result + ((divisionLocations == null) ? 0 : divisionLocations.hashCode());
		result = prime * result + ((firstStation == null) ? 0 : firstStation.hashCode());
		result = prime * result + ((lastStation == null) ? 0 : lastStation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		InfraSegment other = (InfraSegment) obj;
		if (distanceInMeters != other.distanceInMeters) return false;
		if (divisionLocations == null)
		{
			if (other.divisionLocations != null) return false;
		}
		else if (!divisionLocations.equals(other.divisionLocations)) return false;
		if (firstStation == null)
		{
			if (other.firstStation != null) return false;
		}
		else if (!firstStation.equals(other.firstStation)) return false;
		if (lastStation == null)
		{
			if (other.lastStation != null) return false;
		}
		else if (!lastStation.equals(other.lastStation)) return false;
		return true;
	}
}