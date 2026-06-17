package scheduling.columnGeneration;

import java.time.LocalDate;

import graph.structures.PricingGraph;
import modelling.Template;
import optimisation.columnGeneration.pricing.AbstractPricingProblem;
import scheduling.SchedulingInstance;

public class PricingProblem extends AbstractPricingProblem<SchedulingInstance>
{
	private final Template template;
	private final LocalDate day;
	private final PricingGraph pricingGraph;

	public PricingProblem(int index, Template template, LocalDate day, PricingGraph pricingGraph)
	{
		super(index);

		this.template = template;
		this.day = day;
		this.pricingGraph = pricingGraph;
	}

	public Template getTemplate()
	{
		return template;
	}

	public LocalDate getDay()
	{
		return day;
	}

	public PricingGraph getPricingGraph()
	{
		return pricingGraph;
	}
}
