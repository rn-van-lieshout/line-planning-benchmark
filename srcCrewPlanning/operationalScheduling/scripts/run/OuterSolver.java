package operationalScheduling.scripts.run;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import data.read.CrewReader;
import data.read.StationReader;
import data.read.TemplateReader;
import ilog.concert.IloException;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Template;
import operationalScheduling.RosterUtil;
import operationalScheduling.sweetAndSour.attributes.Aggression;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import operationalScheduling.sweetAndSour.attributes.DoubleDeck;
import operationalScheduling.sweetAndSour.attributes.DutyLength;
import operationalScheduling.sweetAndSour.attributes.TypeA;
import util.Configuration;
import util.Logger;
import util.Writer;

public class OuterSolver
{
	public static void main(String[] args) throws IOException, IloException
	{
		// Initialise settings.
		try
		{
//			int index = Integer.valueOf(args[0]);
			int index = 0;
			Configuration.initialiseConfiguration("custom_" + index + ".properties");
		}
		catch (Exception e)
		{
			// Do nothing.
		}
		Configuration configuration = Configuration.getConfiguration();

		// Initialise an output folder and write settings.
		String outputFolder = configuration.getStringProperty("OUTPUT_FOLDER");
		new File(outputFolder).mkdir();
		Writer.write(configuration.getAllProperties(), outputFolder + "/settings.properties");

		// Retrieve date settings.
		LocalDate startDateHistory = configuration.getDateProperty("START_DATE_HISTORY");
		LocalDate endDateHistory = configuration.getDateProperty("END_DATE_HISTORY");
		LocalDate startDateSolver = configuration.getDateProperty("START_DATE_SOLVER");
		LocalDate endDateSolver = configuration.getDateProperty("END_DATE_SOLVER");

		// Initialise a roster for each crew member.
		String dataFolder = configuration.getStringProperty("DATA_FOLDER");
		String rosterFolder = configuration.getStringProperty("ROSTER_FOLDER");
		List<Station> stations = StationReader.readStations(dataFolder + "stations.txt");
		Map<Integer, Station> crewMap = CrewReader.readCrew(rosterFolder + "crew.txt", stations);

		List<DutyRoster> rosters = new ArrayList<>();
		for (Entry<Integer, Station> crewMember : crewMap.entrySet())
		{
			rosters.add(new DutyRoster(crewMember.getKey(), crewMember.getValue()));
		}

		// Read in templates for each day of the period.
		// TODO: What should the period be here?
		LocalDate date = startDateSolver;
		while (!date.isAfter(endDateSolver))
		{
			// Read in templates.
			List<Template> templates = TemplateReader.readTemplates(rosterFolder + date + "/templates.txt", stations);
			for (Template template : templates)
			{
				for (DutyRoster roster : rosters)
				{
					if (roster.getCrewID() != template.getCrewID())
					{
						continue;
					}
					roster.addTemplate(date, template);
					break;
				}
			}

			// Increment date.
			date = date.plusDays(1);
		}

		// Read in duties from history period.
		date = startDateHistory;
		while (!date.isAfter(endDateHistory))
		{
			// TODO: Read in duties.

			// Increment date.
			date = date.plusDays(1);
		}

		// Initialise Sharing-Sweet-and-Sour attributes.
		AttributeVectorStorage storage = AttributeVectorStorage.getAttributeVectorStorage();
		storage.addAttribute(new DutyLength());
		storage.addAttribute(new TypeA());
		storage.addAttribute(new Aggression());
		storage.addAttribute(new DoubleDeck());

		// Iterate over planning period in steps determined by solver settings.
		date = startDateSolver;
		while (!date.isAfter(endDateSolver))
		{
			// Apply inner solver.
			List<LocalDate> days = new ArrayList<>();
			for (int i = 0; i < configuration.getIntProperty("NUM_DAYS_SOLVE"); i++)
			{
				LocalDate day = date.plusDays(i);

				// Do not add date when it is after the last day of the solver period. This may
				// happen when NUM_DAYS_SOLVE is bigger than 1.
				if (day.isAfter(endDateSolver))
				{
					break;
				}
				days.add(day);
			}
			Map<LocalDate, List<Duty>> dutyMap = InnerSolver.applyInnerSolver(days, rosters);

			// Place duties in rosters.
			for (LocalDate day : days)
			{
				RosterUtil.fixDuties(rosters, dutyMap.get(day));
			}

			// Increment date.
			date = date.plusDays(configuration.getIntProperty("NUM_DAYS_FIX"));
		}

		// Write logger.
		Writer.write(Logger.getLogger().getOutput(), configuration.getStringProperty("OUTPUT_FOLDER") + "/log.csv");
	}
}
