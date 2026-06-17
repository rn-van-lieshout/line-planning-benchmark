package data.convertData;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import data.crewsReaders.BlockReader;
import data.crewsReaders.ConnectionReader;
import data.crewsReaders.PathReader;
import data.crewsReaders.StationReader;
import data.crewsReaders.TemplateReader;
import modelling.Connection;
import modelling.Connection.ConnectionType;
import modelling.Duty;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import util.Writer;

public class ConvertData
{
	public static void convertCrewsData(String inputFolder, String outputFolder, LocalDate day) throws IOException
	{
		// Read stations.
		List<Station> stations = StationReader.readStations(inputFolder + "configuration.data");

		// Read data.
		List<Template> templates = TemplateReader.readTemplates(inputFolder + "templates.data", stations, day);
		List<Task> tasks = BlockReader.readTasks(inputFolder + "debug-blocks.data", inputFolder + "initial-blocks.data",
				stations, templates, day);
		List<Connection> connections = ConnectionReader.readConnections(inputFolder + "connections.data");
		List<Duty> duties = PathReader.readDuties(inputFolder + "initial-paths.data", templates, tasks);

		// Correct workload and remove duplicates.
		Set<Connection> includedConnections = new LinkedHashSet<>();
		for (Connection connection : connections)
		{
			// Add block duration to connection duration to ensure workload is
			// correctly set.
			int duration = connection.getDuration();
			if (connection.getConnectionType().equals(ConnectionType.CONNECTION))
			{
				// Duration is difference in end times.
				duration = tasks.get(connection.getEndBlock() - 1).getEndTime()
						- tasks.get(connection.getStartBlock() - 1).getEndTime();
			}
			else if (connection.getConnectionType().equals(ConnectionType.SOURCE))
			{
				// Duration is travel time plus duration of start task.
				duration = connection.getDuration() + tasks.get(connection.getEndBlock() - 1).getDuration();
			}

			// Correctly set duration after break.
			int timeAfterBreak = connection.getTimeAfterBreak();
			if (timeAfterBreak >= 0 && (connection.getConnectionType().equals(ConnectionType.CONNECTION)
					|| connection.getConnectionType().equals(ConnectionType.SOURCE)))
			{
				timeAfterBreak += tasks.get(connection.getEndBlock() - 1).getDuration();
			}

			// Avoid including duplicates.
			Connection correctConnection = new Connection(connection.getIndex(), connection.getConnectionType(),
					connection.getStartBlock(), connection.getEndBlock(), connection.getTimeBeforeBreak(),
					timeAfterBreak, duration, connection.getCost(), connection.getRefWorkingTime(),
					connection.getTypeATime(), connection.getAggressionTime(), connection.getDoubleDeckTime(),
					connection.getPassengerTasks());
			if (!includedConnections.contains(correctConnection))
			{
				includedConnections.add(correctConnection);

			}
		}

		// Give each connection an index.
		int index = 1;
		List<Connection> correctConnections = new ArrayList<>();
		for (Connection connection : includedConnections)
		{
			Connection indexedConnection = new Connection(index, connection.getConnectionType(),
					connection.getStartBlock(), connection.getEndBlock(), connection.getTimeBeforeBreak(),
					connection.getTimeAfterBreak(), connection.getDuration(), connection.getCost(),
					connection.getRefWorkingTime(), connection.getTypeATime(), connection.getAggressionTime(),
					connection.getDoubleDeckTime(), connection.getPassengerTasks());
			correctConnections.add(indexedConnection);
			index++;
		}

		// Make a folder.
		String dayFolderLocation = outputFolder + day;
		new File(dayFolderLocation).mkdir();

		// Write data.
		Writer.write(tasks, dayFolderLocation + "/tasks.txt");
		Writer.write(correctConnections, dayFolderLocation + "/connections.txt");
		Writer.write(templates, dayFolderLocation + "/templates.txt");

		// Write duties in the task format, since we do not have connection information
		// on Crews duties.
		StringBuilder stringBuilder = new StringBuilder();
		for (Duty duty : duties)
		{
			stringBuilder.append(duty.toTaskString() + "\n");
		}
		Writer.write(stringBuilder.toString(), dayFolderLocation + "/crews_duties.txt");
	}
}
