package scheduling.columnGeneration;

import modelling.Duty;

public class DutyColumn extends SchedulingColumn
{
	private final Duty duty;

	public DutyColumn(double cost, Duty duty)
	{
		super(cost, true, true);

		this.duty = duty;
	}

	public Duty getDuty()
	{
		return duty;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof DutyColumn)
		{
			return ((DutyColumn) o).getDuty().equals(duty);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return duty.hashCode();
	}

	@Override
	public String toString()
	{
		return duty.toString();
	}
}
