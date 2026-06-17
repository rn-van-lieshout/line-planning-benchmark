package linePlanning.scripts.tools;

import java.io.IOException;

import ilog.concert.IloException;
import linePlanning.data.ReadNSInstance;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.solvers.PTNSolver;
import linePlanning.solvers.LinePlanningSolution;
import util.Configuration;

public class WriteNL
{
	public static void main(String[] args) throws IOException, IloException
	{
		String stationFile = "data/stations_new.csv";
		String trackFile = "data/tracks_new.csv";
		String forbiddenTurnFile = "data/illegalTurns.csv";
		String connectionsFileIC = "data/connections_all.csv";
		String changeTypeFile = "data/allowedTypeChanges.csv";
		String odFile = "data/od_all.csv";

		// Settings.
		boolean randstadOnly = false;
		boolean allowIC = true;
		boolean allowSPR = false;

		Configuration.initialiseConfiguration("settings.properties");

		LinePlanningInstance instance = ReadNSInstance.readNSInstance(stationFile, trackFile, connectionsFileIC,
				forbiddenTurnFile, changeTypeFile, odFile, randstadOnly, allowIC, allowSPR);
		instance.writeInstance("instances");
	}
}
