package modelling;

public class Constants
{
	// Time units.
	public final static int SECONDS_PER_MINUTE = 60;
	public final static int MINUTES_PER_HALF_HOUR = 30;
	public final static int MINUTES_PER_HOUR = 60;
	public final static int HOURS_PER_DAY = 24;
	public final static int DAYS_PER_WEEK = 7;

	public final static int SECONDS_PER_HALF_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HALF_HOUR;
	public final static int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
	public final static int SECONDS_PER_DAY = SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY;

	// Maximum duty length.
	public static final int MAXIMUM_DUTY_LENGTH = 9 * SECONDS_PER_HOUR + Constants.SECONDS_PER_HALF_HOUR;

	// Meal break constraint.
	public final static int MAXIMUM_TIME_WITHOUT_MEAL_BREAK = 5 * SECONDS_PER_HOUR + SECONDS_PER_HALF_HOUR;
	public final static int MINIMUM_MEAL_BREAK_LENGTH = SECONDS_PER_HALF_HOUR;

	// Minimum rest time between duties.
	public final static int MIN_REST_TIME = 12 * SECONDS_PER_HOUR;

	// Forward rotation parameters.
	public final static int MAXIMUM_START_EARLY_DUTY = 10 * SECONDS_PER_HOUR;
	public final static int MINIMUM_END_LATE_DUTY = 20 * SECONDS_PER_HOUR;
}
