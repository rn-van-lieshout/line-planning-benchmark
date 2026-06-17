package synthetic.visualiser;

import java.util.Map;
import java.util.Set;

import graph.structures.digraph.DirectedGraph;
import graph.structures.digraph.DirectedGraphArc;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import linePlanning.LinePlanningSolution;
import linePlanning.instance.Line;
import linePlanning.instance.ODPair;
import linePlanning.instance.Path;
import linePlanning.instance.StationNode;
import linePlanning.instance.Track;
import modelling.Connection;
import modelling.Task;
import synthetic.GeoStation;
import synthetic.RandomInstanceGenerator;
import timeTabling.ScheduledLine;
import timeTabling.ScheduledTrack;
import util.Pair;

public class Controller
{
	@FXML
	private Slider detourSlider;

	@FXML
	private Slider fractionArcsSlider;

	@FXML
	private Slider maximumDistanceSlider;

	@FXML
	private Slider maximumPathsSlider;

	@FXML
	private LineChart<Number, Number> networkLineChart;

	@FXML
	private LineChart<Number, Number> linePlanChart;

	@FXML
	private LineChart<Number, Number> passengerChart;

	@FXML
	private Slider largeStationsSlider;

	@FXML
	private Slider smallStationsSlider;

	@FXML
	private TableView<GeoStation> stationTable;

	@FXML
	private TableView<DirectedGraphArc<StationNode, Track>> trackTable;

	@FXML
	private TableView<Line> linePlanTable;

	@FXML
	private TableView<ODPair> ODPairTable;

	@FXML
	private TableView<Pair<Path, Double>> ODPathTable;

	@FXML
	private TableView<ScheduledTrack> timeTable;

	@FXML
	private TableView<Task> taskTable;

	@FXML
	private TableView<Connection> connectionTable;

	@FXML
	void clickedGenerateNetwork(ActionEvent event)
	{
		// Retrieve slider values.
		RandomInstanceGenerator.FRACTION_EXTRA_ARCS = fractionArcsSlider.getValue();
		int numberLargeStations = Math.max(1, (int) largeStationsSlider.getValue());
		int numberSmallStations = (int) smallStationsSlider.getValue();

		// Generate and draw random city network
		long seed = System.currentTimeMillis();
		RandomInstanceGenerator.generateRandomNetwork(numberLargeStations, numberSmallStations, seed);
		drawNetwork(RandomInstanceGenerator.infraNetwork, networkLineChart);

		// Update network tables.
		updateNetworkTables();
	}

	private void updateNetworkTables()
	{
		// Set station table entries.
		stationTable.getColumns().clear();
		stationTable.getItems().clear();
		stationTable.refresh();
		TableColumn<GeoStation, Integer> indexCol = new TableColumn<>("Index");
		indexCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getIndex()));

		TableColumn<GeoStation, Double> populationCol = new TableColumn<>("Population");
		populationCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPopulation()));

		TableColumn<GeoStation, String> isLargeCol = new TableColumn<>("Large");
		isLargeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("" + data.getValue().isLarge()));

		stationTable.getColumns().addAll(indexCol, populationCol, isLargeCol);
		stationTable.getItems().addAll(RandomInstanceGenerator.stations);

		// Set track table entries.
		trackTable.getColumns().clear();
		trackTable.getItems().clear();
		trackTable.refresh();
		TableColumn<DirectedGraphArc<StationNode, Track>, Integer> startCol = new TableColumn<>("Start");
		startCol.setCellValueFactory(
				data -> new ReadOnlyObjectWrapper<>(data.getValue().getFrom().getStation().getIndex()));

		TableColumn<DirectedGraphArc<StationNode, Track>, Integer> endCol = new TableColumn<>("End");
		endCol.setCellValueFactory(
				data -> new ReadOnlyObjectWrapper<>(data.getValue().getTo().getStation().getIndex()));

		TableColumn<DirectedGraphArc<StationNode, Track>, Integer> distanceCol = new TableColumn<>("Distance");
		distanceCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getData().getDuration()));

		trackTable.getColumns().addAll(startCol, endCol, distanceCol);
		trackTable.getItems().addAll(RandomInstanceGenerator.infraNetwork.getArcs());
	}

	@FXML
	void clickedGenerateLinePlan(ActionEvent event)
	{
		RandomInstanceGenerator.constructChangeAndGoNetwork();
		RandomInstanceGenerator.generateODPairs();
		RandomInstanceGenerator.generatePassengerPaths();
		RandomInstanceGenerator.generateLinePlan();

		// Set frequencies of line plan.
		LinePlanningSolution linePlan = RandomInstanceGenerator.linePlan;

		// Update tables.
		updateLinePlanTable(linePlan);
		updateODPathTable(linePlan);
	}

	private void updateTimeTable()
	{
		timeTable.getColumns().clear();
		timeTable.getItems().clear();
		timeTable.refresh();

		TableColumn<ScheduledTrack, Integer> indexCol = new TableColumn<>("Line");
		indexCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getLine().getIndex()));

		TableColumn<ScheduledTrack, Integer> startCol = new TableColumn<>("Start");
		startCol.setCellValueFactory(
				data -> new ReadOnlyObjectWrapper<>(data.getValue().getTrack().getFrom().getIndex()));

		TableColumn<ScheduledTrack, Integer> endCol = new TableColumn<>("End");
		endCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getTrack().getTo().getIndex()));

		TableColumn<ScheduledTrack, Integer> departureCol = new TableColumn<>("Departure");
		departureCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDepartureTime()));

		TableColumn<ScheduledTrack, Integer> arrivalCol = new TableColumn<>("Arrival");
		arrivalCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getArrivalTime()));

		timeTable.getColumns().addAll(indexCol, startCol, endCol, departureCol, arrivalCol);
		for (ScheduledLine line : RandomInstanceGenerator.scheduledLines)
		{
			timeTable.getItems().addAll(line.getScheduledTracks());
		}
	}

	private void updateODPathTable(LinePlanningSolution linePlan)
	{
		// ODPair information.
		ODPairTable.getColumns().clear();
		ODPairTable.getItems().clear();
		ODPairTable.refresh();

		TableColumn<ODPair, Integer> originCol = new TableColumn<>("Origin");
		originCol.setCellValueFactory(
				data -> new ReadOnlyObjectWrapper<>(data.getValue().getOrigin().getStation().getIndex()));

		TableColumn<ODPair, Integer> destinationCol = new TableColumn<>("Destination");
		destinationCol.setCellValueFactory(
				data -> new ReadOnlyObjectWrapper<>(data.getValue().getDestination().getStation().getIndex()));

		TableColumn<ODPair, Double> demandCol = new TableColumn<>("Demand");
		demandCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getVolume()));

		ODPairTable.getColumns().addAll(originCol, destinationCol, demandCol);
		ODPairTable.getItems().addAll(RandomInstanceGenerator.ODPairs);

		// Passenger flow information.
		ODPairTable.setRowFactory(tv -> {
			TableRow<ODPair> row = new TableRow<>();
			row.setOnMouseClicked(click -> {
				if (!row.isEmpty() && click.getButton() == MouseButton.PRIMARY && click.getClickCount() == 1)
				{
					// Fill table with flows.
					ODPair pair = row.getItem();
					Set<Pair<Path, Double>> pathPairs = linePlan.getPathsPerOD().get(pair);

					ODPathTable.getColumns().clear();
					ODPathTable.getItems().clear();
					ODPathTable.refresh();

					TableColumn<Pair<Path, Double>, Double> distanceCol = new TableColumn<>("Distance");
					distanceCol.setCellValueFactory(
							data -> new ReadOnlyObjectWrapper<>(data.getValue().getKey().getLength()));

					TableColumn<Pair<Path, Double>, Integer> numberTransfersCol = new TableColumn<>("Nr. Transfers");
					numberTransfersCol.setCellValueFactory(
							data -> new ReadOnlyObjectWrapper<>(data.getValue().getKey().getNumberOfTransfers()));

					TableColumn<Pair<Path, Double>, String> pathStopsCol = new TableColumn<>("Stops");
					pathStopsCol.setCellValueFactory(
							data -> new ReadOnlyStringWrapper("" + data.getValue().getKey().getStations()));

					TableColumn<Pair<Path, Double>, Double> volumeCol = new TableColumn<>("Volume");
					volumeCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));

					ODPathTable.getColumns().addAll(distanceCol, numberTransfersCol, pathStopsCol, volumeCol);
					ODPathTable.getItems().addAll(pathPairs);

					ODPathTable.setRowFactory(tableView -> {
						TableRow<Pair<Path, Double>> odPathRow = new TableRow<>();
						odPathRow.setOnMouseClicked(mouseEvent -> {
							if (!odPathRow.isEmpty() && mouseEvent.getButton() == MouseButton.PRIMARY
									&& mouseEvent.getClickCount() == 1)
							{
								Pair<Path, Double> selectedPair = odPathRow.getItem();
								Path path = selectedPair.getKey();

								// Draw the regular network.
								drawNetwork(RandomInstanceGenerator.infraNetwork, passengerChart);

								// Draw each track segment of the path
								for (int i = 0; i < path.getStations().size() - 1; i++)
								{
									GeoStation from = (GeoStation) path.getStations().get(i);
									GeoStation to = (GeoStation) path.getStations().get(i + 1);

									XYChart.Series<Number, Number> series = new XYChart.Series<>();
									series.getData().add(new XYChart.Data<>(from.getX(), from.getY()));
									series.getData().add(new XYChart.Data<>(to.getX(), to.getY()));

									passengerChart.getData().add(series);
									series.getNode().setStyle("-fx-stroke: black;"); // Customize path color
									for (XYChart.Data<Number, Number> dataPoint : series.getData())
									{
										dataPoint.getNode().setStyle("-fx-background-color: transparent, transparent;");
									}
								}
							}
						});
						return odPathRow;
					});

				}
			});
			return row;
		});
	}

	private void updateLinePlanTable(LinePlanningSolution linePlan)
	{
		Map<Line, Integer> lineFrequencyMap = linePlan.getLineFrequencyMap();

		// Fill line plan table.
		linePlanTable.getColumns().clear();
		linePlanTable.getItems().clear();
		linePlanTable.refresh();

		TableColumn<Line, Integer> costCol = new TableColumn<>("Cost");
		costCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getCost()));

		TableColumn<Line, Integer> startCol = new TableColumn<>("Start");
		startCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getStartStation().getIndex()));

		TableColumn<Line, Integer> endCol = new TableColumn<>("End");
		endCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getEndStation().getIndex()));

		TableColumn<Line, Integer> numberStopsCol = new TableColumn<>("Nr. Stops");
		numberStopsCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getStations().size()));

		TableColumn<Line, String> stopsCol = new TableColumn<>("Stops");
		stopsCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("" + data.getValue().getStations()));

		TableColumn<Line, Integer> frequencyCol = new TableColumn<>("Frequency");
		frequencyCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(lineFrequencyMap.get(data.getValue())));

		linePlanTable.getColumns().addAll(costCol, startCol, endCol, numberStopsCol, stopsCol, frequencyCol);
		linePlanTable.getItems().addAll(lineFrequencyMap.keySet());

		// Specific line information.
		linePlanTable.setRowFactory(tv -> {
			TableRow<Line> row = new TableRow<>();
			row.setOnMouseClicked(click -> {
				if (!row.isEmpty() && click.getButton() == MouseButton.PRIMARY && click.getClickCount() == 1)
				{
					// Draw the regular network.
					drawNetwork(RandomInstanceGenerator.infraNetwork, linePlanChart);

					// Draw the line in the chart.
					Line line = row.getItem();
					for (Track track : line.getLineSegments())
					{
						GeoStation from = (GeoStation) track.getFrom();
						GeoStation to = (GeoStation) track.getTo();
						XYChart.Series<Number, Number> series = new XYChart.Series<>();
						series.getData().add(new XYChart.Data<Number, Number>(from.getX(), from.getY()));
						series.getData().add(new XYChart.Data<Number, Number>(to.getX(), to.getY()));
						linePlanChart.getData().add(series);
						series.getNode().setStyle("-fx-stroke: black;");
						for (XYChart.Data<Number, Number> dataPoint : series.getData())
						{
							dataPoint.getNode().setStyle("-fx-background-color: transparent, transparent");
						}
					}
				}
			});
			return row;
		});
	}

	@FXML
	void clickedGenerateLinePool(ActionEvent event)
	{
		// Set slider values.
		RandomInstanceGenerator.MAX_DETOUR_FRACTION_LINES = 1.0 + detourSlider.getValue();
		RandomInstanceGenerator.MAX_DETOUR_FRACTION_PATHS = 1.0 + detourSlider.getValue();
		RandomInstanceGenerator.MAX_LINE_DISTANCE = (int) Math.rint(maximumDistanceSlider.getValue());

		// Perform steps.
		RandomInstanceGenerator.constructLinePool();
	}

	@FXML
	void clickedGenerateTimeTable(ActionEvent event)
	{
		RandomInstanceGenerator.computeTimeTable();
		updateTimeTable();
	}

	@FXML
	void clickedGenerateCrewData(ActionEvent event)
	{
		RandomInstanceGenerator.assingRollingStockToLines();
		RandomInstanceGenerator.retrieveTasks();
		RandomInstanceGenerator.initialiseTaskNetwork();

		updateTaskTable();
		updateConnectionTable();
	}

	private void updateTaskTable()
	{
		taskTable.getColumns().clear();
		taskTable.getItems().clear();
		taskTable.refresh();

		TableColumn<Task, Integer> indexCol = new TableColumn<>("Task");
		indexCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getIndex()));

		TableColumn<Task, Integer> fromCol = new TableColumn<>("From");
		fromCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getStartStation().getIndex()));

		TableColumn<Task, Integer> toCol = new TableColumn<>("To");
		toCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getEndStation().getIndex()));

		TableColumn<Task, Integer> startCol = new TableColumn<>("Start");
		startCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getStartTime()));

		TableColumn<Task, Integer> endCol = new TableColumn<>("End");
		endCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getEndTime()));

		TableColumn<Task, Integer> durationCol = new TableColumn<>("Duration");
		durationCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDuration()));

		taskTable.getColumns().addAll(indexCol, fromCol, toCol, startCol, endCol, durationCol);
		taskTable.getItems().addAll(RandomInstanceGenerator.tasks);
	}

	private void updateConnectionTable()
	{
		connectionTable.getColumns().clear();
		connectionTable.getItems().clear();
		connectionTable.refresh();

		TableColumn<Connection, Integer> indexCol = new TableColumn<>("Connection");
		indexCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getIndex()));

		TableColumn<Connection, Integer> fromCol = new TableColumn<>("Start task");
		fromCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getStartBlock()));

		TableColumn<Connection, Integer> toCol = new TableColumn<>("End task");
		toCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getEndBlock()));

		TableColumn<Connection, Integer> durationCol = new TableColumn<>("Duration");
		durationCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDuration()));

		TableColumn<Connection, Boolean> breakCol = new TableColumn<>("Break");
		breakCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().hasBreak()));

		connectionTable.getColumns().addAll(indexCol, fromCol, toCol, durationCol, breakCol);
		connectionTable.getItems().addAll(RandomInstanceGenerator.connections);
	}

	public void drawNetwork(DirectedGraph<StationNode, Track> graph, LineChart<Number, Number> chart)
	{
		chart.getData().clear();
		drawArcs(graph, chart);
		drawNodes(graph, chart);
	}

	public void drawNodes(DirectedGraph<StationNode, Track> graph, LineChart<Number, Number> chart)
	{
		XYChart.Series<Number, Number> series = new XYChart.Series<>();
		for (StationNode stationNode : graph.getNodes())
		{
			GeoStation station = (GeoStation) stationNode.getStation();
			if (station.isLarge())
			{
				series.getData().add(new XYChart.Data<Number, Number>(station.getX(), station.getY()));
			}
		}
		chart.getData().add(series);
		series.getNode().setStyle("-fx-stroke: transparent;");
		for (XYChart.Data<Number, Number> dataPoint : series.getData())
		{
			dataPoint.getNode().setStyle("-fx-background-color: black, white;");
		}
	}

	public void drawArcs(DirectedGraph<StationNode, Track> graph, LineChart<Number, Number> chart)
	{
		for (DirectedGraphArc<StationNode, Track> arc : graph.getArcs())
		{
			GeoStation from = (GeoStation) arc.getFrom().getStation();
			GeoStation to = (GeoStation) arc.getTo().getStation();
			XYChart.Series<Number, Number> series = new XYChart.Series<>();
			series.getData().add(new XYChart.Data<Number, Number>(from.getX(), from.getY()));
			series.getData().add(new XYChart.Data<Number, Number>(to.getX(), to.getY()));
			chart.getData().add(series);
			series.getNode().setStyle("-fx-stroke: red;");
			for (XYChart.Data<Number, Number> dataPoint : series.getData())
			{
				dataPoint.getNode().setStyle("-fx-background-color: red, white;");
			}
		}
	}
}
