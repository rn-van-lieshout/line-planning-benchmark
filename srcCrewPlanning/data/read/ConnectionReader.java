package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import modelling.Connection;
import modelling.Connection.ConnectionType;

public class ConnectionReader
{
	public static List<Connection> readConnections(String file) throws IOException
	{
		List<Connection> arcs = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			arcs.add(createConnection(line));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		return arcs;
	}

	private static Connection createConnection(String line)
	{
		String[] values = line.split("\\s+");

		int index = Integer.valueOf(values[0]);
		ConnectionType connectionType;
		switch (values[1])
		{
		case "SOURCE":
			connectionType = ConnectionType.SOURCE;
			break;
		case "SINK":
			connectionType = ConnectionType.SINK;
			break;
		case "CONNECTION":
			connectionType = ConnectionType.CONNECTION;
			break;
		default:
			connectionType = null;
			break;
		}

		int startBlock = Integer.valueOf(values[2]);
		int endBlock = Integer.valueOf(values[3]);
		int timeBeforeBreak = Integer.valueOf(values[4]);
		int timeAfterBreak = Integer.valueOf(values[5]);
		int duration = Integer.valueOf(values[6]);
		int cost = Integer.valueOf(values[7]);

		int refWorkingTime = Integer.valueOf(values[8]);
		int typeATime = Integer.valueOf(values[9]);
		int aggressionTime = Integer.valueOf(values[10]);
		int doubleDeckTime = Integer.valueOf(values[11]);

		List<Integer> passengerTasks = new ArrayList<>();
		for (int i = 12; i < values.length; i++)
		{
			passengerTasks.add(Integer.valueOf(values[i]));
		}
		
		return new Connection(index, connectionType, startBlock, endBlock, timeBeforeBreak, timeAfterBreak, duration,
				cost, refWorkingTime, typeATime, aggressionTime, doubleDeckTime, passengerTasks);
	}
}
