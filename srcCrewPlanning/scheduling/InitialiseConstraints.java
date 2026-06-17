package scheduling;

import java.time.LocalDate;
import java.util.List;

import graph.structures.PricingGraph;
import graph.structures.digraph.DirectedGraphArc;
import modelling.Connection;
import modelling.Constants;
import modelling.DutyRoster;
import modelling.Station;
import modelling.Task;
import modelling.Template;
import operationalScheduling.columnGeneration.RosterActivationFunction;
import optimisation.columnGeneration.AbstractConstraint.ConstraintType;
import scheduling.columnGeneration.TemplateActivationFunction;
import scheduling.constraints.CliqueConstraint;
import scheduling.constraints.CoverConstraint;
import scheduling.constraints.EarlyRotationConstraint;
import scheduling.constraints.LateRotationConstraint;
import scheduling.constraints.TemplateAssignmentConstraint;
import scheduling.constraints.sharingSweetAndSour.SharingAggressionConstraint;
import scheduling.constraints.sharingSweetAndSour.SharingDoubleDeckerConstraint;
import scheduling.constraints.sharingSweetAndSour.SharingDutyLengthConstraint;
import scheduling.constraints.sharingSweetAndSour.SharingTypeAConstraint;
import util.Pair;

public class InitialiseConstraints
{
	public static void initialiseCoverConstraints(SchedulingInstance problem)
	{
		for (LocalDate day : problem.getDays())
		{
			for (Task task : problem.getTasks(day))
			{
				if (task.cover())
				{
					CoverConstraint constraint = new CoverConstraint(task);
					problem.addConstraint(constraint);
					problem.addCoverConstraint(day, constraint);
				}
			}
		}
	}

	public static void initialisePartitionConstraints(SchedulingInstance problem)
	{
		for (LocalDate day : problem.getDays())
		{
			for (Task task : problem.getTasks(day))
			{
				if (task.cover())
				{
					CoverConstraint constraint = new CoverConstraint(ConstraintType.EQUALITY, task);
					problem.addConstraint(constraint);
					problem.addCoverConstraint(day, constraint);
				}
			}
		}
	}

	public static void initialiseTemplateAssignmentConstraints(SchedulingInstance instance)
	{
		for (LocalDate day : instance.getDays())
		{
			for (Template template : instance.getTemplates(day))
			{
				TemplateAssignmentConstraint constraint = new TemplateAssignmentConstraint(template);
				instance.addConstraint(constraint);
				instance.placeInGroupedConstraintMap(day, template, constraint);
			}
		}
	}

	public static void initialiseCliqueConstraints(SchedulingInstance instance,
			List<Pair<Template, Template>> templatePairs)
	{
		for (Pair<Template, Template> pair : templatePairs)
		{
			Template firstTemplate = pair.getKey();
			Template secondTemplate = pair.getValue();
			LocalDate firstDay = firstTemplate.getDay();
			LocalDate secondDay = secondTemplate.getDay();

			// Check whether a rest time conflict is possible.
			if (firstTemplate.getEndTime() + Constants.MIN_REST_TIME - Constants.SECONDS_PER_DAY <= secondTemplate
					.getStartTime())
			{
				continue;
			}

			// Retrieve activation functions.
			TemplateActivationFunction firstActivation = new TemplateActivationFunction(firstTemplate);
			TemplateActivationFunction secondActivation = new TemplateActivationFunction(secondTemplate);

			// Iterate over sink arcs.
			PricingGraph firstPricingGraph = instance.getDailyPricingGraph(firstDay);
			PricingGraph secondPricingGraph = instance.getDailyPricingGraph(secondDay);
			int previousEndTime = Integer.MAX_VALUE;
			sinkLoop:
			for (DirectedGraphArc<Task, Connection> sinkArc : firstPricingGraph
					.getInArcs(firstPricingGraph.getSinkNode()))
			{
				if (!firstActivation.isActiveArc(sinkArc))
				{
					continue;
				}

				// Retrieve end time.
				int endTime = ArcTime.getEndTime(sinkArc);
				if (endTime == previousEndTime)
				{
					continue;
				}

				// Iterate over source arcs of second day.
				for (DirectedGraphArc<Task, Connection> sourceArc : secondPricingGraph
						.getOutArcs(secondPricingGraph.getSourceNode()))
				{
					if (!secondActivation.isActiveArc(sourceArc))
					{
						continue;
					}

					// Retrieve start time.
					int startTime = ArcTime.getStartTime(sourceArc);

					// Check whether a violation is possible.
					if (endTime + Constants.MIN_REST_TIME - Constants.SECONDS_PER_DAY <= startTime)
					{
						continue;
					}

					// Add constraint.
					CliqueConstraint constraint = new CliqueConstraint(firstTemplate, secondTemplate, endTime,
							endTime + Constants.MIN_REST_TIME - Constants.SECONDS_PER_DAY);
					if (instance.getConstraints().contains(constraint))
					{
						continue sinkLoop;
					}
					instance.addConstraint(constraint);
					// TODO: Is it correct to place the constraint in this map twice?
					instance.placeInGroupedConstraintMap(firstDay, firstTemplate, constraint);
					instance.placeInGroupedConstraintMap(secondDay, secondTemplate, constraint);

					previousEndTime = endTime;
					continue sinkLoop;
				}
			}
		}
	}

	public static void initialiseCliqueConstraints(SchedulingInstance instance)
	{
		// Iterate over rosters.
		for (DutyRoster roster : instance.getRosters())
		{
			// Iterate over each pair of subsequent days in the instance.
			for (int t = 0; t < instance.getDays().size() - 1; t++)
			{
				LocalDate firstDay = instance.getDays().get(t);
				LocalDate secondDay = instance.getDays().get(t + 1);

				// Only initialise constraints when the roster contains templates for both days.
				if (!roster.hasTemplate(firstDay) || !roster.hasTemplate(secondDay))
				{
					continue;
				}

				// Check whether a rest time conflict is possible.
				if (roster.getTemplate(firstDay).getEndTime() + Constants.MIN_REST_TIME
						- Constants.SECONDS_PER_DAY <= roster.getTemplate(secondDay).getStartTime())
				{
					continue;
				}

				// Retrieve activation functions.
				RosterActivationFunction firstActivation = new RosterActivationFunction(roster.getTemplate(firstDay),
						roster);
				RosterActivationFunction secondActivation = new RosterActivationFunction(roster.getTemplate(secondDay),
						roster);

				// Iterate over sink arcs.
				PricingGraph firstPricingGraph = instance.getDailyPricingGraph(firstDay);
				PricingGraph secondPricingGraph = instance.getDailyPricingGraph(secondDay);
				int previousEndTime = Integer.MAX_VALUE;
				sinkLoop:
				for (DirectedGraphArc<Task, Connection> sinkArc : firstPricingGraph
						.getInArcs(firstPricingGraph.getSinkNode()))
				{
					if (!firstActivation.isActiveArc(sinkArc))
					{
						continue;
					}

					// Retrieve end time.
					int endTime = ArcTime.getEndTime(sinkArc);
					if (endTime == previousEndTime)
					{
						continue;
					}

					// Iterate over source arcs of second day.
					for (DirectedGraphArc<Task, Connection> sourceArc : secondPricingGraph
							.getOutArcs(secondPricingGraph.getSourceNode()))
					{
						if (!secondActivation.isActiveArc(sourceArc))
						{
							continue;
						}

						// Retrieve start time.
						int startTime = ArcTime.getStartTime(sourceArc);

						// Check whether a violation is possible.
						if (endTime + Constants.MIN_REST_TIME - Constants.SECONDS_PER_DAY <= startTime)
						{
							continue;
						}

						// Add constraint.
						CliqueConstraint constraint = new CliqueConstraint(roster.getTemplate(firstDay),
								roster.getTemplate(secondDay), endTime,
								endTime + Constants.MIN_REST_TIME - Constants.SECONDS_PER_DAY);
						if (instance.getConstraints().contains(constraint))
						{
							continue sinkLoop;
						}
						instance.addConstraint(constraint);
						// TODO: Is it correct to place the constraint in this map twice?
						instance.placeInGroupedConstraintMap(firstDay, roster.getTemplate(firstDay), constraint);
						instance.placeInGroupedConstraintMap(secondDay, roster.getTemplate(secondDay), constraint);

						previousEndTime = endTime;
						continue sinkLoop;
					}
				}
			}
		}
	}

	public static void initialiseEarlyRotationConstraints(SchedulingInstance instance)
	{
		// If templates A and B are early, and B starts after A, duty B should
		// start after duty A.
		// Iterate over rosters.
		for (DutyRoster roster : instance.getRosters())
		{
			// Iterate over each pair of subsequent days in the instance.
			for (int t = 0; t < instance.getDays().size() - 1; t++)
			{
				LocalDate firstDay = instance.getDays().get(t);
				LocalDate secondDay = instance.getDays().get(t + 1);

				// Only initialise constraints when the roster contains templates for both days.
				if (!roster.hasTemplate(firstDay) || !roster.hasTemplate(secondDay))
				{
					continue;
				}

				// Check whether both templates are early.
				if (roster.getTemplate(firstDay).getStartTime() > Constants.MAXIMUM_START_EARLY_DUTY
						|| roster.getTemplate(secondDay).getStartTime() > Constants.MAXIMUM_START_EARLY_DUTY)
				{
					continue;
				}

				// Check whether second template starts after first.
				if (roster.getTemplate(secondDay).getStartTime() < roster.getTemplate(firstDay).getStartTime())
				{
					continue;
				}

				// Retrieve activation functions.
				RosterActivationFunction firstActivation = new RosterActivationFunction(roster.getTemplate(firstDay),
						roster);

				// Iterate over source arcs.
				PricingGraph firstPricingGraph = instance.getDailyPricingGraph(firstDay);
				for (DirectedGraphArc<Task, Connection> sourceArc : firstPricingGraph
						.getOutArcs(firstPricingGraph.getSourceNode()))
				{
					if (!firstActivation.isActiveArc(sourceArc))
					{
						continue;
					}

					// Retrieve start time.
					int startTime = ArcTime.getStartTime(sourceArc);
					if (startTime > Constants.MAXIMUM_START_EARLY_DUTY)
					{
						continue;
					}

					// Add constraint.
					EarlyRotationConstraint constraint = new EarlyRotationConstraint(roster.getTemplate(firstDay),
							roster.getTemplate(secondDay), startTime);
					if (instance.getConstraints().contains(constraint))
					{
						continue;
					}

					instance.addConstraint(constraint);
					// TODO: Is it correct to place the constraint in this map twice?
					instance.placeInGroupedConstraintMap(firstDay, roster.getTemplate(firstDay), constraint);
					instance.placeInGroupedConstraintMap(secondDay, roster.getTemplate(secondDay), constraint);
				}
			}
		}
	}

	public static void initialiseLateRotationConstraints(SchedulingInstance instance)
	{
		// If templates A and B are late, and B ends before A, duty B should end
		// before duty A.
		// Iterate over rosters.
		for (DutyRoster roster : instance.getRosters())
		{
			// Iterate over each pair of subsequent days in the instance.
			for (int t = 0; t < instance.getDays().size() - 1; t++)
			{
				LocalDate firstDay = instance.getDays().get(t);
				LocalDate secondDay = instance.getDays().get(t + 1);

				// Only initialise constraints when the roster contains templates for both days.
				if (!roster.hasTemplate(firstDay) || !roster.hasTemplate(secondDay))
				{
					continue;
				}

				// Check whether both templates are early.
				if (roster.getTemplate(firstDay).getEndTime() < Constants.MINIMUM_END_LATE_DUTY
						|| roster.getTemplate(secondDay).getStartTime() < Constants.MINIMUM_END_LATE_DUTY)
				{
					continue;
				}

				// Check whether second template ends before first.
				if (roster.getTemplate(secondDay).getEndTime() > roster.getTemplate(firstDay).getEndTime())
				{
					continue;
				}

				// Retrieve activation functions.
				RosterActivationFunction firstActivation = new RosterActivationFunction(roster.getTemplate(firstDay),
						roster);

				// Iterate over source arcs.
				PricingGraph firstPricingGraph = instance.getDailyPricingGraph(firstDay);
				for (DirectedGraphArc<Task, Connection> sinkArc : firstPricingGraph
						.getInArcs(firstPricingGraph.getSinkNode()))
				{
					if (!firstActivation.isActiveArc(sinkArc))
					{
						continue;
					}

					// Retrieve end time.
					int endTime = ArcTime.getEndTime(sinkArc);
					if (endTime < Constants.MINIMUM_END_LATE_DUTY)
					{
						continue;
					}

					// Add constraint.
					LateRotationConstraint constraint = new LateRotationConstraint(roster.getTemplate(firstDay),
							roster.getTemplate(secondDay), endTime);
					if (instance.getConstraints().contains(constraint))
					{
						continue;
					}
					instance.addConstraint(constraint);
					// TODO: Is it correct to place the constraint in this map twice?
					instance.placeInGroupedConstraintMap(firstDay, roster.getTemplate(firstDay), constraint);
					instance.placeInGroupedConstraintMap(secondDay, roster.getTemplate(secondDay), constraint);
				}
			}
		}
	}

	public static void initialiseSharingSweetAndSourConstraints(SchedulingInstance instance)
	{
		// Add a Sharing-Sweet-and-Sour constraint for each crew base and each
		// attribute.
		for (Station crewBase : instance.getCrewBases())
		{
			// Initialise constraints.
			SharingDutyLengthConstraint dutyLengthConstraint = new SharingDutyLengthConstraint(crewBase);
			instance.addConstraint(dutyLengthConstraint);

			SharingTypeAConstraint typeAConstraint = new SharingTypeAConstraint(crewBase);
			instance.addConstraint(typeAConstraint);

			SharingAggressionConstraint aggressionConstraint = new SharingAggressionConstraint(crewBase);
			instance.addConstraint(aggressionConstraint);

			SharingDoubleDeckerConstraint doubleDeckerConstraint = new SharingDoubleDeckerConstraint(crewBase);
			instance.addConstraint(doubleDeckerConstraint);

			// Store them for all templates.
			for (LocalDate day : instance.getDays())
			{
				for (Template template : instance.getTemplates(day))
				{
					if (!template.getCrewBase().equals(crewBase))
					{
						continue;
					}
					instance.placeInGroupedConstraintMap(day, template, dutyLengthConstraint);
					instance.placeInGroupedConstraintMap(day, template, typeAConstraint);
					instance.placeInGroupedConstraintMap(day, template, aggressionConstraint);
					instance.placeInGroupedConstraintMap(day, template, doubleDeckerConstraint);
				}
			}
		}
	}
}
