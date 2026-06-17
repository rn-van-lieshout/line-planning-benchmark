package scheduling.checker;

import java.time.LocalDate;

import modelling.Constants;
import modelling.Duty;
import scheduling.SchedulingInstance;

public class FeasibilityChecker
{
	public static void checkInstance(String folder, LocalDate day, String solution)
	{
		// Read in instance.

		// Read in duties.

		// Check duties.

		// Check task coverage.

		// Compute cost.
	}

	private static boolean checkDutyLength(SchedulingInstance instance, Duty duty)
	{
		return duty.getWorkload() <= Constants.MAXIMUM_DUTY_LENGTH;
	}
}
