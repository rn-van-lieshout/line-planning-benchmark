package linePlanning.constraints;

import graph.structures.digraph.DirectedGraphArc;
import linePlanning.columnGeneration.LineColumn;
import linePlanning.columnGeneration.LinePlanningColumn;
import linePlanning.columnGeneration.LinePlanningPP;
import linePlanning.columnGeneration.PathColumn;
import linePlanning.instance.CGNArc;
import linePlanning.instance.Line;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.StationNode;
import linePlanning.instance.TravelArc;
import optimisation.columnGeneration.AbstractConstraint;

public class LineSegmentCapacityConstraint
		extends AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP>
{
	private final TravelArc travelArc;

	public LineSegmentCapacityConstraint(TravelArc travelArc)
	{
		super(ConstraintType.LESSER, 0);

		this.travelArc = travelArc;
	}

	public TravelArc getTravelArc()
	{
		return travelArc;
	}

	public Line getLine()
	{
		return travelArc.getLine();
	}

	@Override
	public boolean containsColumn(LinePlanningColumn column)
	{
		if (column instanceof LineColumn)
		{
			LineColumn lineColumn = (LineColumn) column;
			return lineColumn.getLine().equals(travelArc.getLine());

		}
		if (column instanceof PathColumn)
		{
			PathColumn pathColumn = (PathColumn) column;
			return pathColumn.getPath().getArcs().contains(travelArc);
		}
		return false;
	}

	@Override
	public double getCoefficient(LinePlanningColumn column)
	{
		if (column instanceof LineColumn)
		{
			LineColumn lineColumn = (LineColumn) column;
			return -1 * lineColumn.getLine().getCapacity();

		}
		return 1;
	}
	
	@Override
	public void updateGenericDuals(LinePlanningInstance instance, double dual)
	{
		// Do nothing.
	}

	@Override
	public void updatePricingProblemDuals(LinePlanningPP pricingProblem, double dual)
	{
		for (DirectedGraphArc<StationNode, CGNArc> arc : pricingProblem.getInstance().getCGN().getArcs())
		{
			if (!arc.getData().equals(travelArc))
			{
				continue;
			}
			arc.addDual(dual, 0);
			break;
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((travelArc == null) ? 0 : travelArc.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		LineSegmentCapacityConstraint other = (LineSegmentCapacityConstraint) obj;
		if (travelArc == null)
		{
			if (other.travelArc != null) return false;
		}
		else if (!travelArc.equals(other.travelArc)) return false;
		return true;
	}
}
