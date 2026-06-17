package scheduling.columnGeneration.labelling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import graph.algorithms.spprc.AbstractResourceCompletionTest;
import graph.algorithms.spprc.BidirectionalLabelling;
import graph.structures.Path;
import modelling.Connection;
import modelling.Task;
import optimisation.columnGeneration.AbstractMasterProblem;
import optimisation.columnGeneration.pricing.AbstractPricingProblemSolver;
import scheduling.ConstructDuty;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import util.Configuration;
import util.Pair;

public class LabellingSolver extends AbstractPricingProblemSolver<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final List<AbstractResourceCompletionTest<Task, Connection>> completionBounds;

	public LabellingSolver()
	{
		this.completionBounds = new ArrayList<>();
	}

	public void addCompletionBound(AbstractResourceCompletionTest<Task, Connection> completionBound)
	{
		completionBounds.add(completionBound);
	}

	public BidirectionalLabelling<Task, Connection> getSpprc()
	{
		BidirectionalLabelling<Task, Connection> labelling = new BidirectionalLabelling<>();
		for (AbstractResourceCompletionTest<Task, Connection> completionBound : completionBounds)
		{
			labelling.addCompletionBound(completionBound);
		}
		return labelling;
	}

	@Override
	public List<Pair<SchedulingColumn, Double>> generateColumns(
			AbstractMasterProblem<SchedulingInstance, SchedulingColumn, PricingProblem> masterProblem,
			PricingProblem pricingProblem, double reducedCostThreshold, boolean enumerateColumns)
	{
		// We adjust the threshold to account for the fixed dual per template.
		double fixedDual = pricingProblem.getFixedDual();
		Task sourceNode = pricingProblem.getPricingGraph().getSourceNode();
		Task sinkNode = pricingProblem.getPricingGraph().getSinkNode();
		double threshold = reducedCostThreshold + fixedDual;

		BidirectionalLabelling<Task, Connection> spprc = getSpprc();

		if (enumerateColumns)
		{
			int maxNumberColumns = Configuration.getConfiguration().getIntProperty("MAX_ENUMERATED_COLUMNS");
			spprc.applyLabelling(pricingProblem.getPricingGraph(), sourceNode, sinkNode, new SchedulingREF(), threshold,
					false, true);
			spprc.applyLabellingFixedArcs(pricingProblem.getPricingGraph(), sourceNode, sinkNode, new SchedulingREF(),
					threshold, true, false, spprc.getArcs(), maxNumberColumns);
		}
		else
		{
			spprc.applyLabelling(pricingProblem.getPricingGraph(), sourceNode, sinkNode, new SchedulingREF(), threshold,
					false, false);
		}

		List<Path<Task, Connection>> paths = spprc.getPaths();
		for (Path<Task, Connection> path : paths)
		{
			path.addWeight(-fixedDual);
		}
		List<Pair<SchedulingColumn, Double>> columns = ConstructDuty.constructDutyColumns(pricingProblem, paths);
		if (!enumerateColumns)
		{
			columns.sort(new ReducedCostComparator());
			columns = columns.subList(0, Math.min(columns.size(),
					Configuration.getConfiguration().getIntProperty("MAX_COLS_PER_PRICING_PROBLEM")));
		}
		return columns;
	}

	private class ReducedCostComparator implements Comparator<Pair<SchedulingColumn, Double>>
	{
		@Override
		public int compare(Pair<SchedulingColumn, Double> o1, Pair<SchedulingColumn, Double> o2)
		{
			return Double.compare(o1.getValue(), o2.getValue());
		}
	}
}
