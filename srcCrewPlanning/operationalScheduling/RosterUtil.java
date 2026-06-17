package operationalScheduling;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import modelling.Constants;
import modelling.Duty;
import modelling.DutyRoster;
import modelling.Template;
import operationalScheduling.sweetAndSour.attributes.Attribute;
import operationalScheduling.sweetAndSour.attributes.AttributeVectorStorage;
import util.Configuration;

public class RosterUtil
{
	public static void fixDuties(List<DutyRoster> rosters, List<Duty> duties)
	{
		AttributeVectorStorage storage = AttributeVectorStorage.getAttributeVectorStorage();
		int numDutiesRollingWindow = Configuration.getConfiguration().getIntProperty("NUM_DUTIES_ROLLING_WINDOW");
		boolean useRollingWindow = Configuration.getConfiguration().getBooleanProperty("USE_ROLLING_WINDOW");

		// Place duties in rosters.
		for (Duty duty : duties)
		{
			for (DutyRoster roster : rosters)
			{
				if (duty.getCrewID() == roster.getCrewID())
				{
					// When using a rolling window evaluation and the maximum number of duties has
					// been reached, remove the last duty in the window.
					if (useRollingWindow)
					{
						int numDuties = 0;
						for (Attribute attribute : storage.getAttributes())
						{
							numDuties = storage.getAttributeVector(attribute, duty.getCrewID()).getNumDuties();
							break;
						}

						if (numDuties == numDutiesRollingWindow)
						{
							Duty dutyToRemove = findDutyToRemove(roster, numDutiesRollingWindow,
									duty.getTemplate().getDay().minusDays(1));
							for (Attribute attribute : storage.getAttributes())
							{
								attribute.removeDuty(dutyToRemove,
										storage.getAttributeVector(attribute, duty.getCrewID()));
							}
						}
					}

					// Add the duty to be added.
					roster.addDuty(duty.getTemplate().getDay(), duty);
					for (Attribute attribute : storage.getAttributes())
					{
						attribute.addDuty(duty, storage.getAttributeVector(attribute, duty.getCrewID()));
					}
					break;
				}
			}
		}
	}

	public static Duty findDutyToRemove(DutyRoster roster, int numDuties, LocalDate lastDate)
	{
		int count = 0;
		LocalDate currentDate = lastDate;
		Duty duty = null;
		while (count < numDuties)
		{
			if (roster.hasDuty(currentDate))
			{
				count++;
				duty = roster.getDuty(currentDate);
			}
			currentDate = currentDate.minusDays(1);
		}
		return duty;
	}

	public static List<DutyRoster> convertCyclicToIndividual(List<DutyRoster> cyclicRosters, int numWeeks,
			int templateShift, LocalDate startDate)
	{
		// Initialise list of individual rosters.
		List<DutyRoster> individualRosters = new ArrayList<>();

		// Split up non-empty cyclic rosters into individual ones.
		for (DutyRoster cyclicRoster : cyclicRosters)
		{
			if (cyclicRoster.getTemplates().size() == 0)
			{
				continue;
			}
			individualRosters.addAll(createIndividualRosters(cyclicRoster, startDate));
		}

		// Expand templates of rosters and ensure each roster has a minimum number of
		// weeks.
		for (DutyRoster roster : individualRosters)
		{
			expandTemplateWidth(roster, templateShift);
			setMinimumLength(roster, numWeeks, startDate);
		}

		// Set unique template IDs.
		ensureUniqueIndices(individualRosters);

		// Return rosters.
		return individualRosters;
	}

	public static List<DutyRoster> createIndividualRosters(DutyRoster cyclicRoster, LocalDate startDate)
	{
		int numWeeks = cyclicRoster.getNumWeeks();
		LocalDate endDate = startDate.plusWeeks(numWeeks - 1).plusDays(6);

		List<DutyRoster> individualRosters = new ArrayList<>();
		for (int i = 0; i < numWeeks; i++)
		{
			DutyRoster individualRoster = new DutyRoster(cyclicRoster.getCrewID(), cyclicRoster.getCrewBase());
			for (Template template : cyclicRoster.getTemplates())
			{
				LocalDate shiftedDate = template.getDay().plusWeeks(i);
				if (shiftedDate.isAfter(endDate))
				{
					shiftedDate = shiftedDate.minusWeeks(numWeeks);
				}

				// Make a new template.
				Template newTemplate = new Template(template.getIndex(), template.getCrewID(), template.getCrewBase(),
						shiftedDate, template.getStartTime(), template.getEndTime());
				individualRoster.addTemplate(shiftedDate, newTemplate);
			}

			// Set the correct number of weeks.
			individualRoster.setNumWeeks(cyclicRoster.getNumWeeks());
			individualRosters.add(individualRoster);
		}
		return individualRosters;
	}

	public static void setMinimumLength(DutyRoster roster, int minNumWeeks, LocalDate startDate)
	{
		// If necessary, copy weeks of the roster to ensure sufficient length.
		int numWeeks = roster.getNumWeeks();
		if (numWeeks < minNumWeeks)
		{
			for (int week = numWeeks + 1; week <= minNumWeeks; week++)
			{
				int shiftedWeek = (week - 1) % numWeeks + 1;
				for (int day = 0; day < Constants.DAYS_PER_WEEK; day++)
				{
					// Day of interest.
					LocalDate date = startDate;
					date = date.plusWeeks(week - 1);
					date = date.plusDays(day);

					// The same day, but shifted by numWeeks.
					LocalDate previousDate = startDate;
					previousDate = previousDate.plusWeeks(shiftedWeek - 1);
					previousDate = previousDate.plusDays(day);

					// Add template, if any.
					if (roster.hasTemplate(previousDate))
					{
						Template previous = roster.getTemplate(previousDate);
						Template template = new Template(previous.getIndex(), previous.getCrewID(),
								previous.getCrewBase(), date, previous.getStartTime(), previous.getEndTime());
						roster.addTemplate(date, template);
					}
				}
			}

			// Set the correct number of weeks.
			roster.setNumWeeks(minNumWeeks);
		}
	}

	public static void expandTemplateWidth(DutyRoster roster, int shift)
	{
		for (Template template : roster.getTemplates())
		{
			int startTime = Math.max(0, template.getStartTime() - shift);
			int endTime = template.getEndTime() + shift;
			roster.addTemplate(template.getDay(),
					new Template(template.getIndex(), template.getCrewID(), template.getCrewBase(), template.getDay(),
							startTime, endTime));
		}
	}

	public static void ensureMinimumTemplateLength(List<DutyRoster> rosters, int minimumLength)
	{
		for (DutyRoster roster : rosters)
		{
			for (Template template : roster.getTemplates())
			{
				if (template.getWorkload() < minimumLength)
				{
					int diff = minimumLength - template.getWorkload();
					roster.addTemplate(template.getDay(),
							new Template(template.getIndex(), template.getCrewID(), template.getCrewBase(),
									template.getDay(),
									template.getStartTime() - (diff / 2), template.getEndTime() + (diff / 2)));
				}
			}
		}
	}

	public static void ensureUniqueIndices(List<DutyRoster> rosters)
	{
		int crewID = 1;
		int templateID = 1;
		for (DutyRoster roster : rosters)
		{
			// Set crew ID.
			roster.setCrewID(crewID);

			// Set template ID.
			for (Template template : roster.getTemplates())
			{
				roster.addTemplate(template.getDay(),
						new Template(templateID, crewID, template.getCrewBase(), template.getDay(),
								template.getStartTime(), template.getEndTime()));
				templateID++;
			}
			crewID++;
		}
	}
}
