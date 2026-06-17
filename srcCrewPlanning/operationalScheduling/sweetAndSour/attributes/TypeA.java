package operationalScheduling.sweetAndSour.attributes;

import util.Configuration;

public class TypeA extends TwoValueAttribute
{
	public TypeA()
	{
		super("typeA", false, Configuration.getConfiguration().getDoubleProperty("MIN_PERCENTAGE_TYPE_A"),
				task -> task.getTypeATime(), connection -> connection.getTypeATime());
	}
}
