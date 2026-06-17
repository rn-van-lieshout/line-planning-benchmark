package timeTabling;

public class Activity
{
	private final Event from;
	private final Event to;
	private final int lb;
	private final int ub;
	private final double weight;
	private final String type;

	public Activity(Event from, Event to, int lb, int ub, double weight, String type)
	{
		super();
		this.from = from;
		this.to = to;
		this.lb = lb;
		this.ub = ub;
		this.weight = weight;
		this.type = type;
	}

	public Event getFrom()
	{
		return from;
	}

	public Event getTo()
	{
		return to;
	}

	public int getLb()
	{
		return lb;
	}

	public int getUb()
	{
		return ub;
	}

	public double getWeight()
	{
		return weight;
	}

	public int getSpan()
	{
		return ub - lb;
	}

	public String getType()
	{
		return type;
	}

	public double getPeriodicity()
	{
		return Evaluator.gcd(from.getInterval(), to.getInterval());
	}

	@Override
	public String toString()
	{
		return "Activity [from=" + from + ", to=" + to + ", lb=" + lb + ", ub=" + ub + ", weight=" + weight + ", type="
				+ type + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + lb;
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ub;
		long temp;
		temp = Double.doubleToLongBits(weight);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Activity other = (Activity) obj;
		if (from == null)
		{
			if (other.from != null) return false;
		}
		else if (!from.equals(other.from)) return false;
		if (lb != other.lb) return false;
		if (to == null)
		{
			if (other.to != null) return false;
		}
		else if (!to.equals(other.to)) return false;
		if (type == null)
		{
			if (other.type != null) return false;
		}
		else if (!type.equals(other.type)) return false;
		if (ub != other.ub) return false;
		if (Double.doubleToLongBits(weight) != Double.doubleToLongBits(other.weight)) return false;
		return true;
	}
}
