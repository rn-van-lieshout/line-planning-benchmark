package modelling.infra;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class InfraGraph
{
	public static InfraGraph fromFile(Path segmentPath) throws IOException
	{
		return new InfraGraph(InfraSegment.fromFile(segmentPath));
	}

	private final List<String> vertices;
	private final List<InfraSegment> edges;
	private final Map<String, List<InfraSegment>> incidentEdgesOf;

	public InfraGraph(List<InfraSegment> infraSegments)
	{
		Map<String, List<InfraSegment>> incidentEdgesOf = new TreeMap<>();
		List<InfraSegment> edges = new ArrayList<>();

		for (InfraSegment infraSegment : infraSegments)
		{
			edges.add(infraSegment);
			for (String v : List.of(infraSegment.getFirstStation(), infraSegment.getLastStation()))
			{
				if (!incidentEdgesOf.containsKey(v))
				{
					incidentEdgesOf.put(v, new ArrayList<>());
				}
				incidentEdgesOf.get(v).add(infraSegment);
			}
		}

		this.vertices = Collections.unmodifiableList(new ArrayList<>(incidentEdgesOf.keySet()));
		this.edges = Collections.unmodifiableList(edges);
		for (String v : incidentEdgesOf.keySet())
		{
			incidentEdgesOf.put(v, Collections.unmodifiableList(incidentEdgesOf.get(v)));
		}
		this.incidentEdgesOf = Collections.unmodifiableMap(incidentEdgesOf);

		for (String u : this.getVertices())
		{
			Set<String> seenNeighbours = new LinkedHashSet<>();
			for (InfraSegment infraSegment : this.getIncidentEdgesOf(u))
			{
				String v = infraSegment.getOtherEndLocation(u);
				if (seenNeighbours.contains(v))
				{
					throw new IllegalStateException();
				}
				seenNeighbours.add(v);
			}
		}
	}

	public final List<String> getVertices()
	{
		return this.vertices;
	}

	public final List<InfraSegment> getEdges()
	{
		return this.edges;
	}

	public final boolean contains(String station)
	{
		return this.incidentEdgesOf.containsKey(station);
	}

	public final List<InfraSegment> getIncidentEdgesOf(String vertex)
	{
		if (!this.contains(vertex))
		{
			throw new NoSuchElementException("Vertex = " + vertex);
		}
		return this.incidentEdgesOf.get(vertex);
	}

	public final Optional<InfraSegment> getInfraSegment(String a, String b)
	{
		if (!this.contains(a) || !this.contains(b))
		{
			throw new NoSuchElementException(a + "; " + b);
		}
		for (InfraSegment infraSegment : this.getIncidentEdgesOf(a))
		{
			if (infraSegment.getOtherEndLocation(a).equals(b))
			{
				return Optional.of(infraSegment);
			}
		}
		return Optional.empty();
	}
}