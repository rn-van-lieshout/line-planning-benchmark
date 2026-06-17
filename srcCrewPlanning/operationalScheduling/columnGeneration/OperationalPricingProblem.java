package operationalScheduling.columnGeneration;

import java.time.LocalDate;

import graph.structures.PricingGraph;
import modelling.DutyRoster;
import modelling.Template;
import scheduling.columnGeneration.PricingProblem;

public class OperationalPricingProblem extends PricingProblem
{
	private final DutyRoster roster;

	public OperationalPricingProblem(int index, DutyRoster roster, Template template, LocalDate day, PricingGraph pricingGraph)
	{
		super(index, template, day, pricingGraph);

		this.roster = roster;
	}

	public DutyRoster getRoster()
	{
		return roster;
	}
}
