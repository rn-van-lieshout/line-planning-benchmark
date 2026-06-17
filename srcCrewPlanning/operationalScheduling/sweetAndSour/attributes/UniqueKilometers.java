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

public class UniqueKilometers extends Attribute
{
	public UniqueKilometers()
	{
		// TODO: What should be the upper bound here?
		super("uniqueKilometers", 0, 2 * 1000, false, Configuration.getConfiguration().getIntProperty("MIN_UNIQUE_KILOMETERS"));
	}

	@Override
	public double evaluateDuty(Duty duty)
	{
		// Retrieve unique segments.
		Set<InfraSegment> uniqueSegments = new LinkedHashSet<>();
		CoveredSegmentStorage storage = CoveredSegmentStorage.getCoveredSegmentStorage();
		for (Task task : duty.getTasks())
		{
			uniqueSegments.addAll(storage.getAllSegmentsOf(duty.getTemplate().getDay(), task.getIndex()));
		}
		for (Connection connection : duty.getConnections())
		{
			for (int passengerTask : connection.getPassengerTasks())
			{
				uniqueSegments.addAll(storage.getAllSegmentsOf(duty.getTemplate().getDay(), passengerTask));
			}
		}

		// Compute unique kilometers.
		double km = 0.0;
		for (InfraSegment segment : uniqueSegments)
		{
			km += segment.getDistanceInMeters() / 1000.0;
		}
		return km;
	}

	@Override
	public void modifyPricingProblem(OperationalPricingProblem pricingProblem, double score)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int getAttributeVectorLength()
	{
		return CoveredSegmentStorage.getCoveredSegmentStorage().getInfraGraph().getEdges().size() + 1;
	}

	@Override
	public void addDuty(Duty duty, AttributeVector attributeVector)
	{
		CoveredSegmentStorage storage = CoveredSegmentStorage.getCoveredSegmentStorage();
		for (Task task : duty.getTasks())
		{
			for (InfraSegment segment : storage.getAllSegmentsOf(duty.getTemplate().getDay(), task.getIndex()))
			{
				attributeVector.increaseValue(segment.getIndex(), 1);
				if (attributeVector.getValue(segment.getIndex()) == 1)
				{
					attributeVector.increaseValue(0, segment.getDistanceInMeters());
				}
			}
		}
		for (Connection connection : duty.getConnections())
		{
			for (int passengerTask : connection.getPassengerTasks())
			{
				for (InfraSegment segment : storage.getAllSegmentsOf(duty.getTemplate().getDay(), passengerTask))
				{
					attributeVector.increaseValue(segment.getIndex(), 1);
					if (attributeVector.getValue(segment.getIndex()) == 1)
					{
						attributeVector.increaseValue(0, segment.getDistanceInMeters());
					}
				}
			}
		}
	}

	@Override
	public void removeDuty(Duty duty, AttributeVector attributeVector)
	{
		CoveredSegmentStorage storage = CoveredSegmentStorage.getCoveredSegmentStorage();
		for (Task task : duty.getTasks())
		{
			for (InfraSegment segment : storage.getAllSegmentsOf(duty.getTemplate().getDay(), task.getIndex()))
			{
				attributeVector.decreaseValue(segment.getIndex(), 1);
				if (attributeVector.getValue(segment.getIndex()) == 0)
				{
					attributeVector.decreaseValue(0, segment.getDistanceInMeters());
				}
			}
		}
		for (Connection connection : duty.getConnections())
		{
			for (int passengerTask : connection.getPassengerTasks())
			{
				for (InfraSegment segment : storage.getAllSegmentsOf(duty.getTemplate().getDay(), passengerTask))
				{
					attributeVector.decreaseValue(segment.getIndex(), 1);
					if (attributeVector.getValue(segment.getIndex()) == 0)
					{
						attributeVector.decreaseValue(0, segment.getDistanceInMeters());
					}
				}
			}
		}
	}

	@Override
	public double evaluateAttributeVector(AttributeVector attributeVector)
	{
		return (double) attributeVector.getValue(0) / 1000.0;
	}
}
