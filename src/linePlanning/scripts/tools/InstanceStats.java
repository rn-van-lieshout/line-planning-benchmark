package linePlanning.scripts.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import linePlanning.instance.LinePlanningInstance;
import linePlanning.instance.ODPair;
import linePlanning.lineGeneration.LinePoolGenerator;
import linePlanning.routeGeneration.RouteGenerator;
import util.Configuration;

/**
 * Computes per-instance statistics and prints the LaTeX table for the paper.
 * Run from the project root (where settings.properties and instances/ live).
 */
public class InstanceStats
{
	private static final String DATA_FOLDER = "instances";

	private static final String[] INSTANCE_FOLDERS = {
		"Randstad-IC", "Randstad-IC-SPR", "Nederland-IC", "Nederland-IC-SPR", "SmallSwitzerland", "Switzerland"
	};
	private static final String[] TABLE_NAMES = {
		"Randstad-IC", "Randstad-IC-SPR", "Nederland-IC", "Nederland-IC-SPR", "Switzerland-Small", "Switzerland-Full"
	};
	private static final String[] SIZES = { "small", "medium", "large" };

	public static void main(String[] args) throws IOException
	{
		Configuration.initialiseConfiguration("settings.properties");

		int n = INSTANCE_FOLDERS.length;
		int[]   stations  = new int[n];
		int[]   conns     = new int[n];
		int[]   odDense   = new int[n];
		int[]   odSparse  = new int[n];
		int[][] linePool  = new int[n][3];
		int[][] routeSet  = new int[n][3];

		for (int i = 4; i < n; i++)
		{
			String folder = DATA_FOLDER + "/" + INSTANCE_FOLDERS[i];
			System.out.println("Processing " + INSTANCE_FOLDERS[i] + "...");

			LinePlanningInstance instance = new LinePlanningInstance(
				folder,
				folder + "/ODMatrix/0.txt",
				folder + "/budget/medium.txt",
				folder + "/transferPenalty/medium.txt");

			// if(i>=4) {
			// 	instance.writeODMatrices(folder);
			// }
			stations[i] = countStationsInConnections(instance);
			conns[i]    = instance.getConnections().size();
			odDense[i]  = instance.getODPairs().size();
			odSparse[i] = countLines(folder + "/ODMatrix/3-false.txt");

			// Line pool sizes
			for (int s = 0; s < 3; s++)
			{
				instance.setLineGenerationFile(folder + "/LineGeneration/" + SIZES[s] + ".txt");
				LinePoolGenerator lpg = new LinePoolGenerator(instance);
				linePool[i][s] = lpg.getLines().size();
			}

			// Route set sizes: sum of routes across all OD pairs
			for (int s = 0; s < 3; s++)
			{
				String routeFile = folder + "/RouteGeneration/" + SIZES[s] + ".txt";
				RouteGenerator rg = new RouteGenerator(instance, routeFile);
				int total = 0;
				for (ODPair od : instance.getODPairs())
					total += rg.getRoutes(od, false).size();
				routeSet[i][s] = total;
			}
		}

		printTable(stations, conns, odDense, odSparse, linePool, routeSet);
	}

	private static void printTable(int[] stations, int[] conns, int[] odDense, int[] odSparse,
		int[][] linePool, int[][] routeSet)
	{
		System.out.println();
		System.out.println("\\begin{table}[h]");
		System.out.println("\\centering");
		System.out.println("\\caption{Instance statistics. The OD column reports the number of OD pairs in the dense and sparse matrices. The line pool and route set columns report sizes for the small, medium, and large variants.}");
		System.out.println("\\label{tab:instances}");
		System.out.println("\\small");
		System.out.println("\\begin{tabular}{lrrrrrrrrrr}");
		System.out.println("\\toprule");
		System.out.println("& & & \\multicolumn{2}{c}{OD pairs} & \\multicolumn{3}{c}{Line pool} & \\multicolumn{3}{c}{Route set} \\\\");
		System.out.println("\\cmidrule(lr){4-5} \\cmidrule(lr){6-8} \\cmidrule(lr){9-11}");
		System.out.println("Instance & $|S|$ & $|C|$ & Dense & Sparse & S & M & L & S & M & L \\\\");
		System.out.println("\\midrule");

		for (int i = 0; i < TABLE_NAMES.length; i++)
		{
			System.out.printf("%-20s & %4d & %4d & %5d & %5d & %4d & %4d & %4d & %6d & %6d & %6d \\\\%n",
				TABLE_NAMES[i],
				stations[i], conns[i],
				odDense[i], odSparse[i],
				linePool[i][0], linePool[i][1], linePool[i][2],
				routeSet[i][0], routeSet[i][1], routeSet[i][2]);
			if (i == 3)
				System.out.println("\\midrule");
		}

		System.out.println("\\bottomrule");
		System.out.println("\\end{tabular}");
		System.out.println("\\end{table}");
	}

	private static int countStationsInConnections(LinePlanningInstance instance)
	{
		java.util.Set<linePlanning.instance.Station> inConn = new java.util.HashSet<>();
		for (linePlanning.PTN.Connection c : instance.getConnections())
		{
			inConn.add(c.getFrom());
			inConn.add(c.getTo());
		}
		return inConn.size();
	}

	private static int countLines(String path) throws IOException
	{
		int count = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(path)))
		{
			while (br.readLine() != null) count++;
		}
		return count;
	}
}
