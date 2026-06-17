package modelling.constraints.rosterConstraints;

import modelling.Constants;
import modelling.Duty;
import modelling.Template;

public class ForwardRotation
{
	public static int minimumStartTime(Duty duty, Template template)
	{
		if (Math.max(duty.getTemplate().getStartTime(), template.getStartTime()) > Constants.MAXIMUM_START_EARLY_DUTY)
		{
			return template.getStartTime();
		}
		if (template.getStartTime() < duty.getTemplate().getStartTime())
		{
			return template.getStartTime();
		}
		return Math.max(template.getStartTime(), duty.getStartTime());
	}

	public static int maximumEndTime(Duty duty, Template template)
	{
		if (Math.min(duty.getTemplate().getEndTime(), template.getEndTime()) < Constants.MINIMUM_END_LATE_DUTY)
		{
			return template.getEndTime();
		}
		if (template.getEndTime() > duty.getTemplate().getEndTime())
		{
			return template.getEndTime();
		}
		return Math.min(template.getEndTime(), duty.getEndTime());
	}
}
