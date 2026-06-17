package scheduling.constraints;

import modelling.Duty;
import modelling.Template;
import optimisation.columnGeneration.AbstractConstraint;
import scheduling.SchedulingInstance;
import scheduling.columnGeneration.DutyColumn;
import scheduling.columnGeneration.PricingProblem;
import scheduling.columnGeneration.SchedulingColumn;

public class TemplateAssignmentConstraint
		extends AbstractConstraint<SchedulingInstance, SchedulingColumn, PricingProblem>
{
	private final Template template;

	public TemplateAssignmentConstraint(Template template)
	{
		super(ConstraintType.EQUALITY, 1);
		this.template = template;
	}

	public Template getTemplate()
	{
		return template;
	}

	@Override
	public boolean containsColumn(SchedulingColumn column)
	{
		if (column instanceof DutyColumn)
		{
			DutyColumn c = (DutyColumn) column;
			Duty duty = c.getDuty();
			return duty.getTemplate().equals(template);
		}
		return false;
	}

	@Override
	public double getCoefficient(SchedulingColumn column)
	{
		return 1;
	}

	@Override
	public void updateGenericDuals(SchedulingInstance instance, double dual)
	{
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(PricingProblem pricingProblem, double dual)
	{
		if (pricingProblem.getTemplate().equals(template))
		{
			pricingProblem.addFixedDual(dual);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TemplateAssignmentConstraint other = (TemplateAssignmentConstraint) obj;
		if (template == null)
		{
			if (other.template != null)
				return false;
		}
		else
			if (!template.equals(other.template))
				return false;
		return true;
	}
}
