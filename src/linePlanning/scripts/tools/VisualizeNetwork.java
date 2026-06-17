package linePlanning.scripts.tools;

import linePlanning.data.Reader;
import linePlanning.PTN.Connection;
import linePlanning.instance.Station;
import linePlanning.instance.Track;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a TikZ picture of the track network for a given instance.
 * Output is a standalone .tex file that can be compiled with pdflatex,
 * or \input{} into a larger LaTeX document.
 *
 * Station type colours:
 *   type 0 (Sprinter-only) -> gray, small node
 *   type 1 (Twijfel)       -> blue, medium node
 *   type 2 (IC)            -> red, large node
 *
 * Note: the Station fields named "latitude" and "longitude" in the code are
 * actually (longitude, latitude) in geographic terms for the Dutch instances
 * (first value ~3-7 = easting, second ~50-53 = northing). This script maps
 * them correctly: x <- getLatitude(), y <- getLongitude().
 */
public class VisualizeNetwork
{
    // Canvas width in cm; height is derived to preserve geographic aspect ratio.
    private static final double CANVAS_WIDTH_CM = 15.0;

    public static void main(String[] args) throws IOException
    {
        String[] instanceFolders = {
            "instances/Randstad-IC",
            "instances/Randstad-IC-SPR",
            "instances/Nederland-IC",
            "instances/Nederland-IC-SPR",
            "instances/Switzerland",
            "instances/SmallSwitzerland"
        };

        for (String folder : instanceFolders)
        {
            String instanceName = folder.substring(folder.lastIndexOf('/') + 1);
            String outputFile = "instances/" + instanceName + "_network.tex";
            boolean swiss = instanceName.contains("Switzerland");
            generateTikZ(folder, instanceName, outputFile, swiss, swiss);
            System.out.println("Written: " + outputFile);
        }
    }

    public static void generateTikZ(String instanceFolder, String instanceName, String outputFile, boolean swapXY, boolean icOnly) throws IOException
    {
        // Load stations and tracks using existing Reader methods.
        List<Station> stations = Reader.readStations(instanceFolder + "/stations.txt");

        Map<String, Station> stationMap = new LinkedHashMap<>();
        for (Station s : stations)
            stationMap.put(s.getName(), s);

        // Load corrected coordinates if available (format: code;lat;lon, first line is header).
        Map<String, double[]> coordOverride = new LinkedHashMap<>();
        java.io.File coordFile = new java.io.File(instanceFolder + "/coordinates.txt");
        if (coordFile.exists())
        {
            List<String> lines = Files.readAllLines(Paths.get(coordFile.getPath()));
            for (int i = 1; i < lines.size(); i++)  // skip header
            {
                String[] parts = lines.get(i).split(";");
                if (parts.length >= 3)
                    coordOverride.put(parts[0].trim(),
                        new double[]{ Double.parseDouble(parts[1].trim()),   // lat
                                      Double.parseDouble(parts[2].trim()) }); // lon
            }
        }

        List<Track> tracks = Reader.readTracks(instanceFolder + "/tracks.txt", stationMap);

        // Build track index map for connection loading.
        Map<Integer, Track> trackIndexMap = new LinkedHashMap<>();
        for (Track t : tracks)
            trackIndexMap.put(t.getIndex(), t);

        // Collect tracks that appear in at least one connection.
        Set<Integer> connectionTrackIndices = new HashSet<>();
        List<Connection> connections = Reader.readConnections(instanceFolder + "/connections.txt", stationMap, trackIndexMap);
        for (Connection c : connections)
            for (Track t : c.getTrackPath())
                connectionTrackIndices.add(t.getIndex());

        // Deduplicate bidirectional tracks: keep only one representative per undirected edge.
        Set<String> seenEdges = new HashSet<>();
        Set<String> uniqueEdges = new HashSet<>();
        for (Track t : tracks)
        {
            if (!connectionTrackIndices.contains(t.getIndex())) continue;
            String a = t.getFrom().getName();
            String b = t.getTo().getName();
            String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
            if (!seenEdges.contains(key))
            {
                seenEdges.add(key);
                uniqueEdges.add(t.getFrom().getName() + "|" + t.getTo().getName());
            }
        }

        // Compute cosLat correction from average latitude of stations.
        double avgLat = stations.stream()
                .mapToDouble(s -> stationY(s, swapXY, coordOverride))
                .average().orElse(0.0);
        double cosLat = Math.cos(Math.toRadians(avgLat));

        StringBuilder sb = new StringBuilder();

        // Standalone preamble so the file can be compiled directly.
        sb.append("\\documentclass[tikz,border=5pt]{standalone}\n");
        sb.append("\\usepackage{tikz}\n");
        sb.append("\\usetikzlibrary{arrows.meta}\n");
        sb.append("\\usepackage{xcolor}\n");
        sb.append("\\definecolor{tol_blue}{HTML}{4477AA}\n");
        sb.append("\\definecolor{tol_red}{HTML}{EE6677}\n");
        sb.append("\\definecolor{tol_green}{HTML}{228833}\n");
        sb.append("\\definecolor{tol_orange}{HTML}{CCBB44}\n");
        sb.append("\\begin{document}\n");
        // Raw coordinates are used; cosLat is applied via the x unit so TikZ preserves aspect ratio.
        sb.append(String.format(Locale.US, "\\begin{tikzpicture}[x=%.6fcm,y=1cm]%n", cosLat));
        sb.append(String.format(Locale.US, "%% Instance: %s%n", instanceName));

        // Style definitions.
        sb.append("\\tikzset{\n");
        sb.append("  spr/.style={circle, draw=gray,     fill=gray,     inner sep=0pt, minimum size=0.5pt},\n");
        sb.append("  twi/.style={circle, draw=tol_blue, fill=tol_blue, inner sep=0pt, minimum size=0.75pt},\n");
        sb.append("  ic/.style ={circle, draw=tol_red,  fill=tol_red,  inner sep=0pt, minimum size=1.0pt},\n");
        sb.append("  track/.style={black, line width=0.4pt},\n");
        sb.append("  border/.style={gray, line width=0.3pt},\n");
        sb.append("}\n\n");

        // Draw border polygon if a border.json file exists.
        String borderFile = instanceFolder + "/border.json";
        if (new java.io.File(borderFile).exists())
        {
            String json = new String(Files.readAllBytes(Paths.get(borderFile)));
            // Split into rings by tracking bracket depth. A ring is an array whose direct
            // children are [lon, lat] pairs (i.e. the next '[' after opening contains a number).
            final double MAX_SEGMENT = 0.3;
            Pattern pointPattern = Pattern.compile("\\[\\s*([\\d.eE+-]+)\\s*,\\s*([\\d.eE+-]+)\\s*\\]");
            List<List<double[]>> rings = new ArrayList<>();
            int depth = 0;
            int ringStart = -1;
            int ringDepth = -1;
            for (int i = 0; i < json.length(); i++)
            {
                char c = json.charAt(i);
                if (c == '[')
                {
                    depth++;
                    // Detect ring opening: an '[' whose first non-whitespace child is '[' followed by a digit or '-'
                    if (ringStart < 0)
                    {
                        int j = i + 1;
                        while (j < json.length() && json.charAt(j) <= ' ') j++;
                        if (j < json.length() && json.charAt(j) == '[')
                        {
                            int k = j + 1;
                            while (k < json.length() && json.charAt(k) <= ' ') k++;
                            char first = k < json.length() ? json.charAt(k) : 0;
                            if (Character.isDigit(first) || first == '-')
                            {
                                ringStart = i;
                                ringDepth = depth;
                            }
                        }
                    }
                }
                else if (c == ']')
                {
                    if (ringStart >= 0 && depth == ringDepth)
                    {
                        List<double[]> ring = new ArrayList<>();
                        Matcher pm = pointPattern.matcher(json.substring(ringStart, i + 1));
                        while (pm.find())
                            ring.add(new double[]{ Double.parseDouble(pm.group(1)), Double.parseDouble(pm.group(2)) });
                        if (!ring.isEmpty()) rings.add(ring);
                        ringStart = -1;
                        ringDepth = -1;
                    }
                    depth--;
                }
            }

            // Compute station bounding box to filter out distant rings.
            double stMinX = stations.stream().mapToDouble(s -> stationX(s, swapXY, coordOverride)).min().getAsDouble();
            double stMaxX = stations.stream().mapToDouble(s -> stationX(s, swapXY, coordOverride)).max().getAsDouble();
            double stMinY = stations.stream().mapToDouble(s -> stationY(s, swapXY, coordOverride)).min().getAsDouble();
            double stMaxY = stations.stream().mapToDouble(s -> stationY(s, swapXY, coordOverride)).max().getAsDouble();
            double margin = 2.0;

            sb.append("% Border\n");
            for (List<double[]> ring : rings)
            {
                double rMinX = ring.stream().mapToDouble(p -> p[0]).min().getAsDouble();
                double rMaxX = ring.stream().mapToDouble(p -> p[0]).max().getAsDouble();
                double rMinY = ring.stream().mapToDouble(p -> p[1]).min().getAsDouble();
                double rMaxY = ring.stream().mapToDouble(p -> p[1]).max().getAsDouble();
                if (rMaxX < stMinX - margin || rMinX > stMaxX + margin
                        || rMaxY < stMinY - margin || rMinY > stMaxY + margin) continue;

                for (int i = 0; i < ring.size(); i++)
                {
                    double[] p1 = ring.get(i);
                    double[] p2 = ring.get((i + 1) % ring.size());
                    double dx = p2[0] - p1[0], dy = p2[1] - p1[1];
                    if (Math.sqrt(dx * dx + dy * dy) > MAX_SEGMENT) continue;
                    sb.append(String.format(Locale.US, "  \\draw[border] (%.4f,%.4f) -- (%.4f,%.4f);%n",
                            p1[0], p1[1], p2[0], p2[1]));
                }
            }
            sb.append("\n");
        }

        // Draw tracks first (so nodes render on top).
        sb.append("% Tracks\n");
        for (String edge : uniqueEdges)
        {
            String[] parts = edge.split("\\|");
            Station from = stationMap.get(parts[0]);
            Station to   = stationMap.get(parts[1]);
            double x1 = stationX(from, swapXY, coordOverride);
            double y1 = stationY(from, swapXY, coordOverride);
            double x2 = stationX(to,   swapXY, coordOverride);
            double y2 = stationY(to,   swapXY, coordOverride);
            sb.append(String.format(Locale.US, "  \\draw[track] (%.4f,%.4f) -- (%.4f,%.4f);%n", x1, y1, x2, y2));
        }

        // Draw stations in order SPR -> Twijfel -> IC so higher types overlay lower ones.
        sb.append("\n% Stations\n");
        for (int type : new int[]{0, 1, 2})
        {
            for (Station s : stations)
            {
                if (s.getType() != type) continue;
                if (icOnly && s.getType() < 2) continue;
                double x = stationX(s, swapXY, coordOverride);
                double y = stationY(s, swapXY, coordOverride);
                String style = switch (type)
                {
                    case 2  -> "ic";
                    case 1  -> "twi";
                    default -> "spr";
                };
                sb.append(String.format(Locale.US, "  \\node[%s] at (%.4f,%.4f) {};%n", style, x, y));
            }
        }

        sb.append("\\end{tikzpicture}\n");
        sb.append("\\end{document}\n");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
        {
            writer.write(sb.toString());
        }
    }

    /** Returns the x (horizontal) coordinate for a station, using overrides if available.
     *  coordOverride stores [lat, lon] (file columns: yCor, xCor).
     *  Non-swapped (Dutch): x = lon = coord[1]. Swapped (Swiss): x = lon = coord[1]. */
    private static double stationX(Station s, boolean swapXY, Map<String, double[]> coordOverride)
    {
        double[] coord = coordOverride.get(s.getName());
        if (coord != null) return coord[1]; // x is always lon
        return swapXY ? s.getLongitude() : s.getLatitude();
    }

    /** Returns the y (vertical) coordinate for a station, using overrides if available.
     *  coordOverride stores [lat, lon] (file columns: yCor, xCor).
     *  y is always lat = coord[0]. */
    private static double stationY(Station s, boolean swapXY, Map<String, double[]> coordOverride)
    {
        double[] coord = coordOverride.get(s.getName());
        if (coord != null) return coord[0]; // y is always lat
        return swapXY ? s.getLatitude() : s.getLongitude();
    }

    /** Escapes station name characters that are special in LaTeX. */
    private static String escapeTex(String s)
    {
        return s.replace("_", "\\_").replace("&", "\\&").replace("#", "\\#");
    }
}
