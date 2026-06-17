package modelling.constraints.dutyConstraints;

import modelling.Connection;
import modelling.Constants;
import modelling.Duty;

public class MealBreakFunction
{
	public static boolean satisfiesMealBreakConstraint(int timeWithoutBreak)
	{
		return timeWithoutBreak <= Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK;
	}

	public static boolean isFeasible(Duty duty)
	{
		int timeWithoutBreak = 0;
		int timeSinceStart = 0;
		for (Connection connection : duty.getConnections())
		{
			if (connection.hasBreak())
			{
				int timeToBreak = timeWithoutBreak + connection.getTimeBeforeBreak();
				if (timeToBreak <= Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK
						&& timeSinceStart + connection.getTimeBeforeBreak() < Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK)
				{
					timeWithoutBreak = connection.getTimeAfterBreak();
				}
			}
			else
			{
				timeWithoutBreak += connection.getDuration();
			}
			timeSinceStart += connection.getDuration();
		}
		if (timeWithoutBreak > Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK)
		{
			System.out.println(timeWithoutBreak);
		}
		return timeWithoutBreak <= Constants.MAXIMUM_TIME_WITHOUT_MEAL_BREAK;
	}
}
