package operationalScheduling.sweetAndSour.attributes;

import util.Configuration;

public class DoubleDeck extends TwoValueAttribute
{
	public DoubleDeck()
	{
		super("doubleDeck", true,
				Configuration.getConfiguration().getDoubleProperty("MAX_PERCENTAGE_DOUBLE_DECK"),
				task -> task.getDoubleDeckTime(), connection -> connection.getDoubleDeckTime());
	}
}
