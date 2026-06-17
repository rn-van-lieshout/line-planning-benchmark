package linePlanning.instance;

public class LinePlanningParameters
{
	// Parameters.
	public final static int PERIOD = 60;
	public static int TRANSFER_PENALTY = 15;
	public final static int DWELL_TIME = 2;
	public final static int TURNAROUND_TIME = 30;
	public final static int CAPACITY = 1500;
	public static double LINE_ACTIVATION_COST = 1;
	public final static double MISSED_DEMAND_PENALTY_FACTOR = 3.0;

	// Types.
	public final static int SPRINTER = 0;
	public final static int TWIJFEL = 1;
	public final static int INTERCITY = 2;
}
