package scheduling.heuristic;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import graph.algorithms.spprc.BidirectionalLabelling;
import graph.structures.digraph.DirectedGraphArc;
import graph.structures.digraph.activation.Activation;
import modelling.Connection;
import modelling.Task;
import optimisation.BAP.AbstractReducedCostFixing;
import optimisation.BAP.AbstractReducedCostFixingDecision;
import optimisation.columnGeneration.AbstractMasterProblem;
import optimisation.columnGeneration.pricing.AbstractPricingProblemSolver;
import optimisation.columnGeneration.pricing.AbstractPricingRoutine;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;
import scheduling.columnGeneration.TemplateActivationFunction;
import scheduling.columnGeneration.labelling.LabellingSolver;
import scheduling.columnGeneration.labelling.SchedulingREF;

public class ReducedCostFixing extends AbstractReducedCostFixing<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final double FIXING_THRESHOLD;

	public ReducedCostFixing(double fixingThreshold)
	{
		this.FIXING_THRESHOLD = fixingThreshold;
	}

	@Override
	public AbstractReducedCostFixingDecision<SchedulingInstance, SchedulingColumn, PricingProblem> applyReducedCostFixing(
			AbstractMasterProblem<SchedulingInstance, SchedulingColumn, PricingProblem> masterProblem,
			AbstractPricingRoutine<SchedulingInstance, SchedulingColumn, PricingProblem> pricingRoutine,
			AbstractPricingProblemSolver<SchedulingInstance, SchedulingColumn, PricingProblem> pricingProblemSolver,
			SchedulingInstance instance, double gap)
	{
		Set<Connection> connections = new LinkedHashSet<>();
		LocalDate date = instance.getDays().get(0);

		if (gap <= FIXING_THRESHOLD)
		{
			// We adjust the threshold to account for the fixed dual per template.
			for (PricingProblem pricingProblem : pricingRoutine.getPricingProblems())
			{
				// Chose the proper activation function.
				Activation.getActivation()
						.setActivationFunction(new TemplateActivationFunction(pricingProblem.getTemplate()));
				double fixedDual = pricingProblem.getFixedDual();
				Task sourceNode = pricingProblem.getPricingGraph().getSourceNode();
				Task sinkNode = pricingProblem.getPricingGraph().getSinkNode();
				double threshold = gap + fixedDual;

				BidirectionalLabelling<Task, Connection> spprc = ((LabellingSolver) pricingProblemSolver).getSpprc();
				spprc.applyLabelling(pricingProblem.getPricingGraph(), sourceNode, sinkNode, new SchedulingREF(),
						threshold, false, true);
				for (DirectedGraphArc<Task, Connection> arc : spprc.getArcs())
				{
					connections.add(arc.getData());
				}
			}
		}
		else
		{
			// Simply returns the connections from the instance.
			connections.addAll(instance.getConnections(date));
		}

		// Return fixing decision.
		return new ConnectionReducedCostFixingDecision(date, connections);
	}
}
