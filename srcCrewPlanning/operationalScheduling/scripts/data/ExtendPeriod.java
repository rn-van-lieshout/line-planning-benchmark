package operationalScheduling.scripts.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import data.read.StationReader;
import data.read.TaskReader;
import data.read.TemplateReader;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import util.Writer;

public class ExtendPeriod
{
	public static void main(String[] args) throws IOException
	{
		// Number of days to include.
		int numDays = 49;
		String sourceFolder = "C:\\Users\\bever\\OneDrive - Erasmus University Rotterdam\\Documents\\Research\\CrewPlanning2";

		// Read stations.
		List<Station> stations = StationReader.readStations(sourceFolder + "\\data\\" + "stations.txt");

		// Read data.
		LocalDate date = LocalDate.parse("2021-11-22");
		int index = 100 * 1000;
		for (int i = 1; i <= numDays; i++)
		{
			// Copy data of day x to day x + 49.
			LocalDate originalDate = date.minusDays(numDays);
			System.out.println(date);
			
			// Initialise a new data folder.
			String folder = sourceFolder + "\\data\\" + date.toString();
			new File(folder).mkdir();

			// Directly copy connections and crews duties.
			String originalFolder = sourceFolder + "\\data\\" + originalDate.toString();
			Files.copy(Paths.get(originalFolder + "\\connections.txt"), Paths.get(folder + "\\connections.txt"),
					StandardCopyOption.REPLACE_EXISTING);
			Files.copy(Paths.get(originalFolder + "\\crews_duties.txt"), Paths.get(folder + "\\crews_duties.txt"),
					StandardCopyOption.REPLACE_EXISTING);

			// Copy tasks.
			List<Task> originalTasks = TaskReader.readTasks(originalFolder + "\\tasks.txt", stations);
			List<Task> newTasks = new ArrayList<>();
			for (Task task : originalTasks)
			{
				Task newTask = new Task(task.getIndex(), task.cover(), date, task.getStartTime(), task.getEndTime(),
						task.getStartStation(),
						task.getEndStation(), task.getRefWorkingTime(), task.getTypeATime(), task.getAggressionTime(),
						task.getDoubleDeckTime());
				newTasks.add(newTask);
			}
			Writer.write(newTasks, folder + "\\tasks.txt");

			// Copy templates.
			List<Template> templates = TemplateReader.readTemplates(originalFolder + "\\templates.txt", stations);
			List<Template> newTemplates = new ArrayList<>();
			for (Template template : templates)
			{
				Template newTemplate = new Template(template.getIndex(), template.getCrewID(), template.getCrewBase(),
						date, template.getStartTime(), template.getEndTime());
				newTemplates.add(newTemplate);
			}
			Writer.write(newTemplates, folder + "\\templates.txt");

			// Initialise a new roster folder.
			String originalRosterFolder = sourceFolder + "\\roster\\" + originalDate.toString();
			folder = sourceFolder + "\\roster\\" + date.toString();
			new File(folder).mkdir();

			// Copy the templates in the rosters.
			templates = TemplateReader.readTemplates(originalRosterFolder + "\\templates.txt", stations);
			newTemplates = new ArrayList<>();
			for (Template template : templates)
			{
				Template newTemplate = new Template(index, template.getCrewID(), template.getCrewBase(), date,
						template.getStartTime(), template.getEndTime());
				newTemplates.add(newTemplate);
				index++;
			}
			Writer.write(newTemplates, folder + "\\templates.txt");

			// Increment the day.
			date = date.plusDays(1);
		}
	}
}
