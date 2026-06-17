package data.crewsReaders;

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
		List<Connection> connections = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		int block = 6;

		// Skip lines until "BASE.TASK.CONNECTIONS" is reached.
		String line = bufferedReader.readLine();
		while (!line.trim().equals("BASE.TASK.CONNECTIONS"))
		{
			line = bufferedReader.readLine();
		}

		// Read in the source connections.
		int numConnections = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numConnections; i++)
		{
			String[] data = new String[block];
			for (int j = 0; j < block; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			connections.add(readConnection(data, ConnectionType.SOURCE));
		}

		// Read in the task connections.
		bufferedReader.readLine();
		numConnections = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numConnections; i++)
		{
			String[] data = new String[block];
			for (int j = 0; j < block; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			connections.add(readConnection(data, ConnectionType.CONNECTION));
		}

		// Read in the sink connections.
		bufferedReader.readLine();
		numConnections = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numConnections; i++)
		{
			String[] data = new String[block];
			for (int j = 0; j < block; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			connections.add(readConnection(data, ConnectionType.SINK));
		}

		// Now repeat the above for all 'INITIAL.SET' connections.

		// Read in the source connections.
		bufferedReader.readLine();
		numConnections = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numConnections; i++)
		{
			String[] data = new String[block];
			for (int j = 0; j < block; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			connections.add(readConnection(data, ConnectionType.SOURCE));
		}

		// Read in the task connections.
		bufferedReader.readLine();
		numConnections = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numConnections; i++)
		{
			String[] data = new String[block];
			for (int j = 0; j < block; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			connections.add(readConnection(data, ConnectionType.CONNECTION));
		}

		// Read in the sink connections.
		bufferedReader.readLine();
		numConnections = Integer.valueOf(bufferedReader.readLine());
		for (int i = 0; i < numConnections; i++)
		{
			String[] data = new String[block];
			for (int j = 0; j < block; j++)
			{
				data[j] = bufferedReader.readLine();
			}
			connections.add(readConnection(data, ConnectionType.SINK));
		}

		bufferedReader.close();
		return connections;
	}

	private static Connection readConnection(String[] data, ConnectionType connectionType)
	{
		String[] line = data[0].split("\\s+");
		int startBlock = Integer.valueOf(line[0]);
		int endBlock = Integer.valueOf(line[1]);
		int timeBeforeBreak = Integer.valueOf(line[2]);
		int timeAfterBreak = Integer.valueOf(line[3]);

		line = data[2].split("\\s+");
		int duration = Integer.valueOf(line[1]);
		int cost = Integer.valueOf(line[2]);

		int refWorkingTime = Integer.valueOf(line[3]);
		int typeATime = Integer.valueOf(line[5]);
		int aggressionTime = Integer.valueOf(line[6]);
		int doubleDeckTime = Integer.valueOf(line[7]);

		List<Integer> passengerTasks = new ArrayList<>();

		// Indices will be set later.
		Connection connection = new Connection(0, connectionType, startBlock, endBlock, timeBeforeBreak,
				timeAfterBreak, duration, cost, refWorkingTime, typeATime, aggressionTime, doubleDeckTime,
				passengerTasks);
		return connection;
	}
}
