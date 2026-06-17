package linePlanning.scripts.tools;

import java.io.IOException;
import java.util.List;

import ilog.concert.IloException;
import linePlanning.data.ReadSwissInstance;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.lineGeneration.Line;
import linePlanning.lineGeneration.LinePoolGenerator;
import util.Configuration;

public class WriteSwiss
{

	public static void main(String[] args) throws IOException, IloException
	{
		boolean small = false;

		String stationFile = small ? "dataSwiss/stationsWithPopulationsSmall.txt" : "dataSwiss/stationsWithPopulations.txt";
		String connectionsFile = small ? "dataSwiss/connectionsSmall.txt" : "dataSwiss/connections.txt";
		String odFile = small ? "dataSwiss/odSmall.txt" : "dataSwiss/od.txt";
		String illegalTurnsFile = "dataSwiss/IllegalTurns.txt";
		String trackFile = "dataSwiss/tracks-with-geoshapes.csv";

		// Settings.
		Configuration.initialiseConfiguration("settings.properties");
		System.out.println("Read swiss");
		LinePlanningInstance instance = ReadSwissInstance.readSwissInstance(stationFile,
			connectionsFile, odFile, illegalTurnsFile, trackFile,small);

		instance.writeInstance("instances");
	}
}
