package modelling.infra;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Configuration;
import util.DateExtender;

/**
 * Storage of covered segments (what a surprise)
 *
 * <p>
 * Usage
 * <ul>
 * <li>Make sure that {@code ../in/block-segment-data} exists and that it
 * contains all necessary information. See {@link InfraProcessingLauncher} for
 * details about how to generate the data.</li>
 * <li>{@code CoveredSegmentStorage coveredSegmentStorage = CoveredSegmentStorage.create();}</li>
 * at any point in the code base.</li>
 * <li>{@code CoveredSegmentStorage} is immutable; it is safe to read the data
 * once, and to pass a reference to any class that want to look at the covered
 * segments.</li>
 * <li>{@link #getProductiveSegmentsOf(LocalDate, int)},
 * {@link #getPassengerSegmentsOf(LocalDate, int)} and
 * {@link #getAllSegmentsOf(LocalDate, int)} return lists that may contain a
 * segment several times.</li>
 * <li>{@link CoveredSegmentStorage} identifies the tasks by their integer ID;
 * it never considers a {@code } object when reading the data file or when
 * retrieving the covered segments.</li>
 * <li>{@link #getCoveredSegmentsOf(LocalDate, int)} is a deprecated alias for
 * {@link #getProductiveSegmentsOf(LocalDate, int)}.</li>
 * </ul>
 * 
 * <p>
 * The CREWS data cover a 7-week period (2021-10-04 through 2021-11-21), let's
 * call it a block. This class assumes that the timetable is repeated every 7
 * weeks, ad infinitum.
 * 
 * <p>
 * Caveats
 * <ul>
 * <li>Missing/unclear data was corrected manually, errors are possible or even
 * likely.</li>
 * <li>Exotic-looking pieces of works were dropped, e.g., with indication "REG"
 * and "G3".</li>
 * <li>The train paths on the Western edge of Asd are far from trivial, multiple
 * routes are possible. The manually chosen segments may not be 100% accurate.
 * The same applies to the area of Wgm.</li>
 * </ul>
 * 
 * @author marotig
 *
 */
public class CoveredSegmentStorage
{
	public static final String PASSENGER_SEGMENT_SUFFIX = "_p";

	private static final Path DEFAULT_SEGMENT_DATA_DIRECTORY = Paths.get(
			Configuration.getConfiguration().getStringProperty("INFRA_FOLDER"));

	private static CoveredSegmentStorage coveredSegmentStorage;

	public static CoveredSegmentStorage getCoveredSegmentStorage()
	{
		try
		{
			return (coveredSegmentStorage == null)
					? coveredSegmentStorage = create()
					: coveredSegmentStorage;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static CoveredSegmentStorage create() throws IOException
	{
		return create(DEFAULT_SEGMENT_DATA_DIRECTORY);
	}

	public static CoveredSegmentStorage create(Path segmentDataDirectory) throws IOException
	{
		InfraGraph infraGraph = InfraGraph.fromFile(segmentDataDirectory.resolve("segments.txt"));
		Pattern blockSegmentFileNameRegex = Pattern.compile("^block-segments-(\\d+[-]\\d+[-]\\d+)[.]txt$");
		Pattern segmentDescriptionPegex = Pattern.compile(
				"^([a-zA-Z]+)[-]([a-zA-Z]+)((?:" + PASSENGER_SEGMENT_SUFFIX + ")?)$");

		Map<LocalDate, Map<Integer, List<InfraSegment>>> productiveSegmentsOf = new TreeMap<>();
		Map<LocalDate, Map<Integer, List<InfraSegment>>> passengerSegmentsOf = new TreeMap<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(DEFAULT_SEGMENT_DATA_DIRECTORY))
		{
			for (Path path : directoryStream)
			{
				Matcher matcher = blockSegmentFileNameRegex.matcher(path.getFileName().toString());
				if (matcher.matches())
				{
					LocalDate date = LocalDate.parse(matcher.group(1));
					productiveSegmentsOf.put(date, new LinkedHashMap<>());
					passengerSegmentsOf.put(date, new LinkedHashMap<>());

					try (BufferedReader reader = Files.newBufferedReader(path))
					{
						while (true)
						{
							String line = reader.readLine();
							if (line == null)
							{
								break;
							}
							if (line.isBlank() || line.strip().startsWith("#"))
							{
								continue;
							}

							String[] parts = line.strip().split("\\s+", -1);
							int taskID = Integer.parseInt(parts[0]);

							if (productiveSegmentsOf.get(date).containsKey(taskID))
							{
								throw new IllegalStateException("line = '" + line + "'");
							}
							productiveSegmentsOf.get(date).putIfAbsent(taskID, new ArrayList<>());
							passengerSegmentsOf.get(date).putIfAbsent(taskID, new ArrayList<>());

							for (int i = 1; i < parts.length; ++i)
							{
								Matcher segmentMatcher = segmentDescriptionPegex.matcher(parts[i]);
								if (!segmentMatcher.matches())
								{
									throw new IllegalStateException("line = '" + line + "'");
								}
								String from = segmentMatcher.group(1);
								String to = segmentMatcher.group(2);
								boolean isPassenger = PASSENGER_SEGMENT_SUFFIX.equals(
										segmentMatcher.group(3).toLowerCase());

								Optional<InfraSegment> optionalInfraSegment = infraGraph.getInfraSegment(from, to);
								if (optionalInfraSegment.isEmpty())
								{
									throw new IllegalStateException("line = '" + line + "'");
								}
								if (isPassenger)
								{
									passengerSegmentsOf.get(date).get(taskID).add(optionalInfraSegment.get());
								}
								else
								{
									productiveSegmentsOf.get(date).get(taskID).add(optionalInfraSegment.get());
								}
							}
						}
					}
				}
			}
		}

		return new CoveredSegmentStorage(infraGraph, productiveSegmentsOf, passengerSegmentsOf);
	}

	private final InfraGraph infraGraph;
	private final Map<LocalDate, Map<Integer, List<InfraSegment>>> productiveSegmentsOf;
	private final Map<LocalDate, Map<Integer, List<InfraSegment>>> passengerSegmentsOf;
	private final Map<LocalDate, Map<Integer, List<InfraSegment>>> allSegmentsOf;
	private final DateExtender dateExtender;

	private CoveredSegmentStorage(
			InfraGraph infraGraph,
			Map<LocalDate, Map<Integer, List<InfraSegment>>> productiveSegmentsOf,
			Map<LocalDate, Map<Integer, List<InfraSegment>>> passengerSegmentsOf)
	{
		this.infraGraph = infraGraph;
		this.productiveSegmentsOf = _make_immutate_copy(List.of(productiveSegmentsOf));
		this.passengerSegmentsOf = _make_immutate_copy(List.of(passengerSegmentsOf));
		this.allSegmentsOf = _make_immutate_copy(List.of(productiveSegmentsOf, passengerSegmentsOf));

		LocalDate block1StartDate = Collections.min(this.allSegmentsOf.keySet());
		LocalDate block1EndDate = Collections.max(this.allSegmentsOf.keySet());
		this.dateExtender = DateExtender.create(block1StartDate, block1EndDate);
	}

	public final InfraGraph getInfraGraph()
	{
		return this.infraGraph;
	}

	public final List<InfraSegment> getProductiveSegmentsOf(LocalDate date, int taskID)
	{
		return this.retrieve_segments_of(date, taskID, this.productiveSegmentsOf);
	}

	public final List<InfraSegment> getPassengerSegmentsOf(LocalDate date, int taskID)
	{
		return this.retrieve_segments_of(date, taskID, this.passengerSegmentsOf);
	}

	public final List<InfraSegment> getAllSegmentsOf(LocalDate date, int taskID)
	{
		return this.retrieve_segments_of(date, taskID, this.allSegmentsOf);
	}

	private static Map<LocalDate, Map<Integer, List<InfraSegment>>> _make_immutate_copy(
			List<Map<LocalDate, Map<Integer, List<InfraSegment>>>> segmentMaps)
	{
		Map<LocalDate, Map<Integer, List<InfraSegment>>> result = new LinkedHashMap<>();

		// Populate
		for (Map<LocalDate, Map<Integer, List<InfraSegment>>> segmentMap : segmentMaps)
		{
			for (LocalDate date : segmentMap.keySet())
			{
				result.putIfAbsent(date, new LinkedHashMap<>());
				for (int taskID : segmentMap.get(date).keySet())
				{
					result.get(date).putIfAbsent(taskID, new ArrayList<>());
					result.get(date).get(taskID).addAll(segmentMap.get(date).get(taskID));
				}
			}
		}

		// Immutate
		for (LocalDate date : result.keySet())
		{
			for (int taskID : result.get(date).keySet())
			{
				result.get(date).put(taskID, Collections.unmodifiableList(result.get(date).get(taskID)));
			}
			result.put(date, Collections.unmodifiableMap(result.get(date)));
		}

		return Collections.unmodifiableMap(result);
	}

	private List<InfraSegment> retrieve_segments_of(
			LocalDate date, int taskID, Map<LocalDate, Map<Integer, List<InfraSegment>>> segmentsOf)
	{
		LocalDate effectiveDate = this.dateExtender.getReferenceDate(date);
		if (!segmentsOf.containsKey(effectiveDate) || !segmentsOf.get(effectiveDate).containsKey(taskID))
		{
			throw new NoSuchElementException(effectiveDate + " " + taskID);
		}
		return segmentsOf.get(effectiveDate).get(taskID);
	}
}