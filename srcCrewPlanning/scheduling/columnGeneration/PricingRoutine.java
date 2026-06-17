package scheduling.columnGeneration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import graph.structures.PricingGraph;
import graph.structures.digraph.activation.Activation;
import modelling.Template;
import optimisation.columnGeneration.pricing.AbstractPricingRoutine;
import scheduling.SchedulingInstance;

public class PricingRoutine extends AbstractPricingRoutine<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	@Override
	protected void preProcessPricingProblems(SchedulingInstance instance)
	{
		for (LocalDate day : instance.getDays())
		{
			// Set weights to connection costs.
			instance.getDailyPricingGraph(day).resetWeights();
		}
	}

	@Override
	protected List<PricingProblem> generatePricingProblems(SchedulingInstance instance)
	{
		List<PricingProblem> pricingProblems = new ArrayList<>();
		for (LocalDate day : instance.getDays())
		{
			PricingGraph graph = instance.getDailyPricingGraph(day);
			for (Template template : instance.getTemplates(day))
			{
				pricingProblems.add(new PricingProblem(pricingProblems.size(), template, day, graph));
			}
		}
		return pricingProblems;
	}

	@Override
	protected void preProcessPricingProblem(SchedulingInstance instance, PricingProblem pricingProblem)
	{
		// Chose the proper activation function. Note that this is a thread-local class.
		Activation.getActivation().setActivationFunction(new TemplateActivationFunction(pricingProblem.getTemplate()));

		// Reset template-specific duals.
		pricingProblem.resetFixedDual();
	}
}
