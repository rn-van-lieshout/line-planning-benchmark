package linePlanning.instance;

public class Index
{
	private static int CONNECTION_INDEX = 0;
	private static int TRACK_INDEX = 0;
	private static int STATION_INDEX = 0;
	private static int LINE_INDEX = 0;

	public static int getStationIndex()
	{
		int value = STATION_INDEX;
		STATION_INDEX++;
		return value;
	}

	public static int getTrackIndex()
	{
		int value = TRACK_INDEX;
		TRACK_INDEX++;
		return value;
	}

	public static int getConnectionIndex()
	{
		int value = CONNECTION_INDEX;
		CONNECTION_INDEX++;
		return value;
	}
	
	public static int getLineIndex()
	{
		int value = LINE_INDEX;
		LINE_INDEX++;
		return value;
	}
}
