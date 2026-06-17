package linePlanning.columnGeneration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import linePlanning.instance.CGNArc;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import linePlanning.instance.StationODNode;
import optimisation.columnGeneration.pricing.AbstractPricingProblem;

public class LinePlanningPP extends AbstractPricingProblem<LinePlanningInstance>
{
	private final LinePlanningInstance instance;
	private final StationODNode origin;
	private final Set<ODPair> ODPairs;
	private final Map<ODPair, Double> dualMap;
	private final Map<ODPair, Map<CGNArc, Double>> arcDualMap;

	public LinePlanningPP(int index, LinePlanningInstance instance, StationODNode origin, Set<ODPair> ODPairs)
	{
		super(index);

		this.instance = instance;
		this.origin = origin;
		this.ODPairs = ODPairs;
		this.dualMap = new LinkedHashMap<>();
		this.arcDualMap = new LinkedHashMap<>();
		for (ODPair ODPair : ODPairs)
		{
			arcDualMap.put(ODPair, new LinkedHashMap<>());
		}
	}

	public LinePlanningInstance getInstance()
	{
		return instance;
	}

	public StationODNode getOrigin()
	{
		return origin;
	}

	public Set<ODPair> getODPairs()
	{
		return ODPairs;
	}

	public boolean containsODPair(ODPair ODPair)
	{
		return ODPairs.contains(ODPair);
	}

	public void addODPairDual(ODPair ODPair, double dual)
	{
		dualMap.put(ODPair, dual);
	}

	public Map<ODPair, Double> getDualMap()
	{
		return dualMap;
	}

	public void addArcDual(ODPair ODPair, CGNArc arc, double dual)
	{
		if (!arcDualMap.get(ODPair).containsKey(arc))
		{
			arcDualMap.get(ODPair).put(arc, 0.0);
		}
		arcDualMap.get(ODPair).put(arc, arcDualMap.get(ODPair).get(arc) + dual);
	}

	public Map<CGNArc, Double> getArcDualMap(ODPair ODPair)
	{
		return arcDualMap.get(ODPair);
	}
}
