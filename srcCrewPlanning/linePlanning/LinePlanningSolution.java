package linePlanning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import linePlanning.columnGeneration.LineColumn;
import linePlanning.columnGeneration.LinePlanningColumn;
import linePlanning.columnGeneration.LinePlanningPP;
import linePlanning.columnGeneration.PathColumn;
import linePlanning.instance.Line;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import linePlanning.instance.Path;
import optimisation.columnGeneration.AbstractSolution;
import util.Pair;

public class LinePlanningSolution extends AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPP>
{
	public LinePlanningSolution(double objectiveValue, Map<LinePlanningColumn, Double> columnMap)
	{
		super(objectiveValue, columnMap);
	}

	public Map<Line, Integer> getLineFrequencyMap()
	{
		Map<Line, Integer> lineMap = new LinkedHashMap<>();
		for (Entry<LinePlanningColumn, Double> entry : columnMap.entrySet())
		{
			if (entry.getKey() instanceof LineColumn)
			{
				Line line = ((LineColumn) entry.getKey()).getLine();
				int freq = (int) Math.round(entry.getValue());
				lineMap.put(line, freq);
			}
		}
		return lineMap;
	}

	public Set<Line> getLines()
	{
		Set<Line> lines = new LinkedHashSet<>();
		for (LinePlanningColumn column : columnMap.keySet())
		{
			if (column instanceof LineColumn)
			{
				lines.add(((LineColumn) column).getLine());
			}
		}
		return lines;
	}

	public Map<ODPair, Set<Pair<Path, Double>>> getPathsPerOD()
	{
		Map<ODPair, Set<Pair<Path, Double>>> pathMap = new LinkedHashMap<>();
		for (Entry<LinePlanningColumn, Double> entry : columnMap.entrySet())
		{
			if (entry.getKey() instanceof PathColumn)
			{
				Path path = ((PathColumn) entry.getKey()).getPath();
				ODPair ODPair = path.getODPair();
				if (!pathMap.containsKey(ODPair))
				{
					pathMap.put(ODPair, new LinkedHashSet<>());
				}
				pathMap.get(ODPair).add(new Pair<>(path, entry.getValue()));
			}
		}
		return pathMap;
	}

	public double getTravelTime()
	{
		double travelTime = 0;
		for (Entry<LinePlanningColumn, Double> entry : columnMap.entrySet())
		{
			if (entry.getKey() instanceof PathColumn)
			{
				Path path = ((PathColumn) entry.getKey()).getPath();
				travelTime += path.getLength() * entry.getValue();
			}
		}
		return travelTime;
	}
}
