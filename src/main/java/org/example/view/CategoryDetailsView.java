package org.example.view;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.example.model.ProcessItem;
import org.example.utils.TimeFormatter;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CategoryDetailsView extends VBox {

  private Runnable onBackRequested = () -> {
  };

  public CategoryDetailsView(String categoryName, List<ProcessItem> processItems) {
    getStyleClass().add("category-detail-root");
    setMaxWidth(Double.MAX_VALUE);
    setMaxHeight(Double.MAX_VALUE);

    Button backBtn = new Button("Back");
    backBtn.getStyleClass().add("back-btn");
    backBtn.setOnAction(e -> onBackRequested.run());

    Label title = new Label(categoryName + " Category");
    title.getStyleClass().add("category-detail-title");

    // Region headerSpacer = new Region();
    // HBox.setHgrow(headerSpacer, Priority.ALWAYS);

    HBox topBar = new HBox(24, backBtn, title);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 16, 10, 16));

    // Table view
    TableView<ProcessItem> table = buildTable(processItems);
    HBox.setHgrow(table, Priority.ALWAYS);

    // Pie Chart
    VBox uptimeChart = buildRightPanel(categoryName, processItems);
    HBox.setHgrow(uptimeChart, Priority.ALWAYS);

    HBox body = new HBox(16, table, uptimeChart);
    body.setPadding(new Insets(16));
    VBox.setVgrow(body, Priority.ALWAYS);

    getChildren().addAll(topBar, body);
  }

  private TableView<ProcessItem> buildTable(List<ProcessItem> processes) {
    TableView<ProcessItem> table = new TableView<>();
    table.getStyleClass().add("category-details-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setPlaceholder(new Label("No processes in this category."));

    // Process name column
    TableColumn<ProcessItem, String> nameCol = new TableColumn<>("Process");
    nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));
    nameCol.getStyleClass().add("table-col-name");

    // CPU usage column
    TableColumn<ProcessItem, Double> cpuCol = new TableColumn<>("CPU %");
    cpuCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getCpuUsage()).asObject());
    cpuCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : String.format("%.1f%%", value));
      }
    });

    // RAM usage column
    TableColumn<ProcessItem, Double> ramCol = new TableColumn<>("RAM (MB)");
    ramCol.setCellValueFactory(
        data -> new SimpleDoubleProperty(data.getValue().getRamUsageMb()).asObject());
    ramCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : String.format("%.1f", value));
      }
    });

    // Uptime column
    TableColumn<ProcessItem, Long> uptimeCol = new TableColumn<>("Uptime");
    uptimeCol.setCellValueFactory(
        data -> new SimpleLongProperty(data.getValue().getUptimeSeconds()).asObject());
    uptimeCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Long value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : TimeFormatter.formatTime(value));
      }
    });

    table.getColumns().addAll(nameCol, cpuCol, ramCol, uptimeCol);
    table.setItems(FXCollections.observableArrayList(processes));

    return table;
  }

  private VBox buildRightPanel(String categoryName, List<ProcessItem> processes) {

    List<ProcessItem> top10 = processes.stream()
        .sorted(Comparator.comparingLong(
            ProcessItem::getUptimeSeconds).reversed())
        .limit(10)
        .collect(Collectors.toList());

    PieChart uptimeChart = buildUptimeChart(top10);
    VBox.setVgrow(uptimeChart, Priority.ALWAYS);

    long totalUptime = processes.stream()
        .mapToLong(
            ProcessItem::getUptimeSeconds)
        .sum();

    Label totalLabel = new Label(
        categoryName + " Total time — " + TimeFormatter.formatTime(totalUptime));
    totalLabel.getStyleClass().add("category-total-uptime");

    VBox panel = new VBox(8, uptimeChart, totalLabel);
    panel.setAlignment(Pos.TOP_CENTER);

    return panel;
  }

  private PieChart buildUptimeChart(List<ProcessItem> processes) {
    long totalUptime = processes.stream()
        .mapToLong(
            ProcessItem::getUptimeSeconds)
        .sum();

    ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();

    for (ProcessItem p : processes) {
      // If all processes have zero uptime, give each an equal slice
      double value = totalUptime > 0 ? p.getUptimeSeconds() : 1.0;
      slices.add(new PieChart.Data(p.getDisplayName(), value));
    }

    PieChart chart = new PieChart(slices);
    chart.setTitle("Top 10 processes by time spent");
    chart.setLegendVisible(true);
    chart.getStyleClass().add("main-pie-chart");

    return chart;
  }

  public void setOnBackRequested(Runnable handler) {
    this.onBackRequested = handler;
  }

}
