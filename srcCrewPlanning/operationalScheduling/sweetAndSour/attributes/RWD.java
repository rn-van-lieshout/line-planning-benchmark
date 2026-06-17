package operationalScheduling.sweetAndSour.attributes;

import java.util.LinkedHashSet;
import java.util.Set;

import modelling.Connection;
import modelling.Duty;
import modelling.Task;
import modelling.infra.CoveredSegmentStorage;
import modelling.infra.InfraSegment;
import operationalScheduling.columnGeneration.OperationalPricingProblem;
import util.Configuration;

public class RWD extends DutyAverageAttribute
{
	public RWD()
	{
		super("RWD", 1.0, 6.0, true, Configuration.getConfiguration().getDoubleProperty("MAX_RWD"));
	}

	@Override
	public double evaluateDuty(Duty duty)
	{
		// Compute repeated and unique kms.
		double totalKilometers = 0.0;
		Set<InfraSegment> uniqueSegments = new LinkedHashSet<>();
		CoveredSegmentStorage storage = CoveredSegmentStorage.getCoveredSegmentStorage();
		for (Task task : duty.getTasks())
		{
			for (InfraSegment segment : storage.getAllSegmentsOf(duty.getTemplate().getDay(), task.getIndex()))
			{
				totalKilometers += segment.getDistanceInMeters();
				uniqueSegments.add(segment);
			}
		}
		for (Connection connection : duty.getConnections())
		{
			for (int passengerTask : connection.getPassengerTasks())
			{
				for (InfraSegment segment : storage.getAllSegmentsOf(duty.getTemplate().getDay(), passengerTask))
				{
					totalKilometers += segment.getDistanceInMeters();
					uniqueSegments.add(segment);
				}
			}
		}

		// Compute unique kilometers.
		double uniqueKilometers = 0.0;
		for (InfraSegment segment : uniqueSegments)
		{
			uniqueKilometers += segment.getDistanceInMeters();
		}
		return uniqueKilometers > 0 ? (totalKilometers / uniqueKilometers) : 0;
	}

	@Override
	public void modifyPricingProblem(OperationalPricingProblem pricingProblem, double score)
	{
		// TODO Auto-generated method stub
	}
}
