package operationalScheduling.sweetAndSour.attributes;

import util.Configuration;

public class Aggression extends TwoValueAttribute
{
	public Aggression()
	{
		super("aggression", true, Configuration.getConfiguration().getDoubleProperty("MAX_PERCENTAGE_AGGRESSION"),
				task -> task.getAggressionTime(), connection -> connection.getAggressionTime());
	}
}