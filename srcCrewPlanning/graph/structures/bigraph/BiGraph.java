package graph.structures.bigraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple adjacency-list implementation of an weighted undirected bipartite
 * graph. The vertex set consists of V and U, left-hand side and right-hand side
 * vertices, respectively.
 * 
 * @author Bart van Rossum.
 */
public class BiGraph
{
	private final int numU;
	private final int numV;

	private final List<List<Integer>> adjacencyList;
	private final List<List<Double>> weightList;

	public BiGraph(int numU, int numV)
	{
		this.numU = numU;
		this.numV = numV;

		this.adjacencyList = new ArrayList<>();
		this.weightList = new ArrayList<>();
		for (int u = 0; u < numU; u++)
		{
			adjacencyList.add(new ArrayList<>());
			weightList.add(new ArrayList<>());
		}
	}

	public void addEdge(int u, int v)
	{
		// Default weight is 1.
		addEdge(u, v, 1);
	}

	public void addEdge(int u, int v, double weight)
	{
		adjacencyList.get(u).add(v);
		weightList.get(u).add(weight);
	}

	public int getNumU()
	{
		return numU;
	}

	public int getNumV()
	{
		return numV;
	}
	
	public void removeEdge(int u, int v)
	{
		int index = adjacencyList.get(u).indexOf(v);
		if (index == -1)
		{
			throw new IllegalArgumentException("Edge is not contained in the bigraph.");
		}
		adjacencyList.get(u).remove(index);
		weightList.get(u).remove(index);
	}

	public boolean containsEdge(int u, int v)
	{
		return adjacencyList.get(u).contains(v);
	}

	public double getWeight(int u, int v)
	{
		int index = adjacencyList.get(u).indexOf(v);
		if (index == -1)
		{
			throw new IllegalArgumentException("Edge is not contained in the bigraph.");
		}
		return weightList.get(u).get(index);
	}
	
	public int getDegree(int u)
	{
		return adjacencyList.get(u).size();
	}

	public Iterable<Integer> getNeighbours(int u)
	{
		return adjacencyList.get(u);
	}

	public double[][] getWeightMatrix()
	{
		double[][] weightMatrix = new double[numU][numV];
		for (int u = 0; u < numU; u++)
		{
			for (int v = 0; v < numV; v++)
			{
				// Default weight of missing edges is a large value.
				weightMatrix[u][v] = 1000 * 1000;
			}

			for (int i = 0; i < adjacencyList.get(u).size(); i++)
			{
				weightMatrix[u][adjacencyList.get(u).get(i)] = weightList.get(u).get(i);
			}
		}
		return weightMatrix;
	}
}
