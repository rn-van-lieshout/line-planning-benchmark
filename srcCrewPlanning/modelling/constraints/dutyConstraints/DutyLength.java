package modelling.constraints.dutyConstraints;

import modelling.Constants;

public class DutyLength
{
	public static boolean isFeasibleEndTime(int startTime, int endTime)
	{
		int duration = endTime - startTime;
		if (duration < 0)
		{
			duration += Constants.HOURS_PER_DAY * Constants.SECONDS_PER_HOUR;
		}
		return duration <= Constants.MAXIMUM_DUTY_LENGTH;
	}
}