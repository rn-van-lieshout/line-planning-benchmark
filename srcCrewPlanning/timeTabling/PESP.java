package timeTabling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraphArc;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class PESP
{
	public static int DWELL_TIME_SLACK = 2;
	public static int TravelTimeSlack = 0;
	public static int TurnAroundTime = 10;
	public static int TurnAroundSlack = 60;

	private final EAN ean;
	private final int T;
	private final int transferPenalty;

	private Set<Cycle> cycleBasis;
	private IloCplex model;
	private Map<Activity, IloNumVar> actToX;
	private Map<Cycle, IloNumVar> cycleToQ;

	private Map<Event, IloNumVar> eventToPi;
	private Map<Activity, IloNumVar> actToZ;

	private double objective;
	private EAN travelCostNetwork; // the timetable stored in an EAN, with all tensions on the arcs
	private Map<Activity, Double> tensions;
	private Map<Event, Integer> potentials;

	public PESP(EAN ean, int t, int transferPenalty, double incurredTransferPenalty) throws IloException
	{
		this.ean = ean;
		this.T = t;
		this.transferPenalty = transferPenalty;
		System.out.println("\nInitialized Event Activity Network with " + ean.getNumberOfNodes() + " events and "
				+ ean.getNumberOfArcs() + " activities");
		computeCycleBasis();
		model = new IloCplex();
		initXAndObjective(incurredTransferPenalty);
		initQ();
		initPi();
		initZ();
		addLinkingConstraints(false);
		// model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 1.0e-9);
		// model.setParam(IloCplex.DoubleParam.TimeLimit, 3600);
	}

	private void computeCycleBasis()
	{
		cycleBasis = new HashSet<>();
		EAN mst = ean.getMST();
		System.out.println("Found minimum spanning forest with " + mst.getNumberOfNodes() + " events and "
				+ mst.getNumberOfArcs() + " activities");
		for (DirectedGraphArc<Event, Activity> arc : ean.getArcs())
		{
			if (!mst.getArcs().contains(arc))
			{
				Activity a = arc.getData();
				// we can find a cycle by adding a to the tree
				Cycle c = new Cycle(mst.getCycle(a), T);
				cycleBasis.add(c);
				c.setGenerator(a);
			}
		}
	}

	public boolean solve(long timeLimitPESP) throws IloException
	{
//		model.setOut(null);
		model.setParam(IloCplex.DoubleParam.TimeLimit, timeLimitPESP);
		boolean feasible = model.solve();
		if (feasible)
		{
			objective = model.getObjValue();
		}
		tensions = new HashMap<>();
		travelCostNetwork = new EAN();
		for (Event e : ean.getNodes())
		{
			travelCostNetwork.addNode(e);
		}
		for (DirectedGraphArc<Event, Activity> arc : ean.getArcs())
		{
			Activity act = arc.getData();
			double tensionval = model.getValue(actToX.get(act));
			if (arc.getData().getType().equals("transfer"))
			{
				travelCostNetwork.addArc(arc.getFrom(), arc.getTo(), act, tensionval + transferPenalty);
			}
			else
			{
				travelCostNetwork.addArc(arc.getFrom(), arc.getTo(), act, tensionval);
			}
			tensions.put(arc.getData(), tensionval);
		}
		model.clearModel();
		model.end();
		computeNodePotentials();
		return feasible;
	}

	private void computeNodePotentials() throws IloException
	{
		model = new IloCplex();
		initPi();
		initZ();
		addLinkingConstraints(true);
		model.setOut(null);
		boolean feasible = model.solve();
		if (!feasible)
		{
			throw new Error("something is seriously wrong");
		}
		potentials = new HashMap<>();
		for (Event e : ean.getNodes())
		{
			int time = (int) Math.round(model.getValue(eventToPi.get(e)));
			potentials.put(e, time);
		}
		model.clearModel();
		model.end();
	}

	private void initPi() throws IloException
	{
		eventToPi = new HashMap<>();
		for (Event e : ean.getNodes())
		{
			eventToPi.put(e, model.intVar(0, T));
		}
	}

	private void initZ() throws IloException
	{
		actToZ = new HashMap<>();
		for (DirectedGraphArc<Event, Activity> a : ean.getArcs())
		{
			actToZ.put(a.getData(), model.intVar(0, Integer.MAX_VALUE));
		}
	}

	private void addLinkingConstraints(boolean useSolution) throws IloException
	{
		for (DirectedGraphArc<Event, Activity> a : ean.getArcs())
		{
			IloNumExpr lhs = model.diff(eventToPi.get(a.getTo()), eventToPi.get(a.getFrom()));
			lhs = model.sum(lhs, model.prod(actToZ.get(a.getData()), a.getData().getPeriodicity()));
			if (useSolution)
			{
				model.addEq(lhs, tensions.get(a.getData()));
			}
			else
			{
				model.addEq(lhs, actToX.get(a.getData()));
			}
		}
	}

	public EAN getTravelCostNetwork()
	{
		return travelCostNetwork;
	}

	public double getObjective()
	{
		return objective;
	}

	private void initQ() throws IloException
	{
		cycleToQ = new HashMap<>();
		for (Cycle c : cycleBasis)
		{
			IloNumVar q_c = model.intVar(c.getLower(), c.getUpper());
			cycleToQ.put(c, q_c);
			IloNumExpr lhs = model.constant(0);
			for (Activity a : c.getForward())
			{
				lhs = model.sum(lhs, actToX.get(a));
			}
			for (Activity a : c.getBackward())
			{
				lhs = model.diff(lhs, actToX.get(a));
			}
			model.addEq(lhs, model.prod(q_c, T));
		}

	}

	private void initXAndObjective(double incurredTransferPenalty) throws IloException
	{
		actToX = new HashMap<>();
		IloNumExpr obj = model.constant(incurredTransferPenalty);
		for (DirectedGraphArc<Event, Activity> arc : ean.getArcs())
		{
			Activity a = arc.getData();
			IloNumVar x_a = model.numVar(a.getLb(), a.getUb());
			actToX.put(a, x_a);
			obj = model.sum(obj, model.prod(a.getWeight(), x_a));
		}
		model.addMinimize(obj);
	}

	public EAN getEAN()
	{
		return ean;
	}

	public Map<Activity, Double> getTensions()
	{
		return tensions;
	}

	public Map<Event, Integer> getPotentials()
	{
		return potentials;
	}

}
