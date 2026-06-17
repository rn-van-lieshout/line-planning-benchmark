package linePlanning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import graph.structures.digraph.DirectedGraphArc;
import ilog.concert.IloColumn;
import ilog.concert.IloConversion;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import linePlanning.columnGeneration.LineColumn;
import linePlanning.columnGeneration.LinePlanningColumn;
import linePlanning.columnGeneration.LinePlanningPP;
import linePlanning.columnGeneration.PathColumn;
import linePlanning.constraints.LineSegmentCapacityConstraint;
import linePlanning.constraints.PathAssignmentConstraint;
import linePlanning.instance.CGNArc;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import linePlanning.instance.StationNode;
import linePlanning.instance.TravelArc;
import modelling.Constants;
import optimisation.columnGeneration.AbstractConstraint;
import optimisation.columnGeneration.AbstractMasterProblem;
import optimisation.columnGeneration.AbstractSolution;
import util.Pair;

public class LinePlanningMaster extends AbstractMasterProblem<LinePlanningInstance, LinePlanningColumn, LinePlanningPP>
{
	private Map<ODPair, PathAssignmentConstraint> pathAssignmentConstraintMap;
	private Map<TravelArc, LineSegmentCapacityConstraint> lineCapacityConstraintMap;

	public LinePlanningMaster(LinePlanningInstance instance) throws IloException
	{
		super(instance);

		cplex.setOut(null);

		this.pathAssignmentConstraintMap = new LinkedHashMap<>();
		this.lineCapacityConstraintMap = new LinkedHashMap<>();
	}

	@Override
	public void addColumn(LinePlanningColumn column) throws IloException
	{
		// Initialise column.
		IloColumn columnVar = cplex.column(objective, column.getCoefficient());

		// Add to branching constraints.
		for (AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> constraint : branchingConstraints)
		{
			if (constraint.containsColumn(column))
			{
				columnVar = columnVar
						.and(cplex.column(constraintMap.get(constraint), constraint.getCoefficient(column)));
			}
		}

		// Add to regular constraints.
		if (column instanceof PathColumn)
		{
			// Add to OD-specific constraint.
			PathColumn pathColumn = (PathColumn) column;
			ODPair ODPair = pathColumn.getPath().getODPair();
			PathAssignmentConstraint assignmentConstraint = pathAssignmentConstraintMap.get(ODPair);
			if (assignmentConstraint.containsColumn(column))
			{
				columnVar = columnVar.and(cplex.column(constraintMap.get(assignmentConstraint),
						assignmentConstraint.getCoefficient(column)));
			}

			// Add to capacity constraints.
			for (CGNArc arc : pathColumn.getPath().getArcs())
			{
				if (arc instanceof TravelArc)
				{
					TravelArc travelArc = (TravelArc) arc;
					LineSegmentCapacityConstraint constraint = lineCapacityConstraintMap.get(travelArc);
					if (constraint.containsColumn(pathColumn))
					{
						columnVar = columnVar.and(
								cplex.column(constraintMap.get(constraint), constraint.getCoefficient(pathColumn)));
					}
				}
			}
		}
		else
		{
			for (AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> constraint : constraintMap
					.keySet())
			{
				if (constraint.containsColumn(column))
				{
					columnVar = columnVar
							.and(cplex.column(constraintMap.get(constraint), constraint.getCoefficient(column)));
				}
			}
		}

		// Add column to model.
		if (column instanceof LineColumn)
		{
			LineColumn lineColumn = (LineColumn) column;
			columnMap.put(lineColumn, cplex.numVar(columnVar, 0, lineColumn.getLine().getMaxFrequency()));
		}
		else
			if (column instanceof PathColumn)
			{
				PathColumn pathColumn = (PathColumn) column;
				columnMap.put(pathColumn, cplex.numVar(columnVar, 0, Double.MAX_VALUE));
			}
	}

	public void addConstraintWithExistingColumns(
			AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> constraint,
			List<Pair<LinePlanningColumn, IloNumVar>> columns) throws IloException
	{
		// Add all columns to left-hand side.
		IloNumExpr lhs = cplex.constant(0);
		for (Pair<LinePlanningColumn, IloNumVar> pair : columns)
		{
			if (constraint.containsColumn(pair.getKey()))
			{
				lhs = cplex.sum(lhs, cplex.prod(pair.getValue(), constraint.getCoefficient(pair.getKey())));
			}
		}

		IloRange range;
		switch (constraint.getModelConstraintType())
		{
		case EQUALITY:
			range = cplex.addEq(lhs, constraint.getBound());
			break;
		case LESSER:
			range = cplex.addLe(lhs, constraint.getBound());
			break;
		case GREATER:
			range = cplex.addGe(lhs, constraint.getBound());
			break;
		default:
			range = null;
			break;
		}
		constraintMap.put(constraint, range);

		// Add a slack variable.
		constraint.addSlackVariable(this);
	}

	@Override
	public void addConstraint(AbstractConstraint<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> constraint)
			throws IloException
	{
		IloRange range;
		switch (constraint.getModelConstraintType())
		{
		case EQUALITY:
			range = cplex.addEq(cplex.constant(0), constraint.getBound());
			break;
		case LESSER:
			range = cplex.addLe(cplex.constant(0), constraint.getBound());
			break;
		case GREATER:
			range = cplex.addGe(cplex.constant(0), constraint.getBound());
			break;
		default:
			range = null;
			break;
		}
		constraintMap.put(constraint, range);
		if (constraint instanceof PathAssignmentConstraint)
		{
			PathAssignmentConstraint assignmentConstraint = (PathAssignmentConstraint) constraint;
			pathAssignmentConstraintMap.put(assignmentConstraint.getODPair(), assignmentConstraint);
		}
		if (constraint instanceof LineSegmentCapacityConstraint)
		{
			LineSegmentCapacityConstraint capacityConstraint = (LineSegmentCapacityConstraint) constraint;
			lineCapacityConstraintMap.put(capacityConstraint.getTravelArc(), capacityConstraint);
		}

		// Add a slack variable.
		constraint.addSlackVariable(this);
	}

	@Override
	public void updatePricingProblemDuals(LinePlanningPP pricingProblem) throws IloException
	{
		for (ODPair pair : pricingProblem.getODPairs())
		{
			double dual = cplex.getDual(constraintMap.get(pathAssignmentConstraintMap.get(pair)));
			pricingProblem.addODPairDual(pair, dual);
		}
	}

	public void updateCapacityDuals() throws IloException
	{
		for (DirectedGraphArc<StationNode, CGNArc> arc : instance.getCGN().getArcs())
		{
			if (arc.getData() instanceof TravelArc)
			{
				TravelArc travelArc = (TravelArc) arc.getData();
				arc.setDual(cplex.getDual(constraintMap.get(lineCapacityConstraintMap.get(travelArc))), 0);
			}
		}
	}

	@Override
	public AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> applyRestrictedMasterHeuristic(
			int timeLimitSeconds) throws IloException
	{
		// Convert to MILP.
		List<IloConversion> conversions = new ArrayList<>();
		for (LinePlanningColumn column : columnMap.keySet())
		{
			if (column instanceof LineColumn)
			{
				IloConversion conversion = cplex.conversion(columnMap.get(column), IloNumVarType.Int);
				cplex.add(conversion);
				conversions.add(conversion);
			}
		}

		// Solve MILP and store optimality gap.
		setTimeLimit(timeLimitSeconds);
		solve();
		AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> solution = getSolution();

		// Convert back to LP.
		for (IloConversion conversion : conversions)
		{
			cplex.remove(conversion);
		}
		setTimeLimit(Constants.SECONDS_PER_HOUR);
		return solution;
	}

	@Override
	public AbstractSolution<LinePlanningInstance, LinePlanningColumn, LinePlanningPP> getSolution() throws IloException
	{
		// Retrieve all columns with positive coefficients.
		Map<LinePlanningColumn, Double> solutionMap = new LinkedHashMap<>();
		for (Entry<LinePlanningColumn, IloNumVar> entry : columnMap.entrySet())
		{
			double value = cplex.getValue(entry.getValue());
			if (value > 0)
			{
				solutionMap.put(entry.getKey(), value);
			}
		}
		return new LinePlanningSolution(getObjectiveValue(), solutionMap);
	}
}
