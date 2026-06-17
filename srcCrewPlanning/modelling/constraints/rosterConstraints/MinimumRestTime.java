package modelling.constraints.rosterConstraints;

import modelling.Constants;
import modelling.Duty;
import modelling.Template;

public class MinimumRestTime
{
	public static int minimumStartTime(Duty duty, Template template)
	{
		if (duty.getEndTime() + Constants.MIN_REST_TIME > template.getStartTime() + Constants.SECONDS_PER_DAY)
		{
			return (duty.getEndTime() + Constants.MIN_REST_TIME) % Constants.SECONDS_PER_DAY;
		}
		return template.getStartTime();
	}
}
