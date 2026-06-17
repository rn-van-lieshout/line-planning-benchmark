package operationalScheduling.columnGeneration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import graph.structures.PricingGraph;
import graph.structures.digraph.activation.Activation;
import modelling.DutyRoster;
import operationalScheduling.sweetAndSour.AbstractScoringFunction;
import operationalScheduling.sweetAndSour.ProcessAttributeScores;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.PricingRoutine;
import util.Configuration;

public class OperationalPricingRoutine extends PricingRoutine
{
	private final AbstractScoringFunction scoringFunction;

	public OperationalPricingRoutine(AbstractScoringFunction scoringFunction)
	{
		this.scoringFunction = scoringFunction;
	}

	@Override
	protected List<PricingProblem> generatePricingProblems(SchedulingInstance instance)
	{
		int index = 0;
		List<PricingProblem> pricingProblems = new ArrayList<>();
		for (LocalDate day : instance.getDays())
		{
			PricingGraph graph = instance.getDailyPricingGraph(day);
			for (DutyRoster roster : instance.getRosters())
			{
				if (roster.hasTemplate(day))
				{
					pricingProblems
							.add(new OperationalPricingProblem(index, roster, roster.getTemplate(day), day, graph));
					index++;
				}
			}
		}
		return pricingProblems;
	}

	@Override
	protected void preProcessPricingProblem(SchedulingInstance instance, PricingProblem pricingProblem)
	{
		@SuppressWarnings("unused")
		OperationalPricingProblem operationalProblem = (OperationalPricingProblem) pricingProblem;

		// Chose the proper activation function.
		Activation.getActivation().setActivationFunction(
				new RosterActivationFunction(pricingProblem.getTemplate(), operationalProblem.getRoster()));

		// Set weights to connection costs.
		pricingProblem.getPricingGraph().resetSecondWeights();

		// Reset template-specific duals.
		pricingProblem.resetFixedDual();

		// Place penalties on arcs.
		if (Configuration.getConfiguration().getBooleanProperty("USE_PENALTIES"))
		{
			ProcessAttributeScores.processAttributeScores(scoringFunction, operationalProblem);
		}
	}
}
