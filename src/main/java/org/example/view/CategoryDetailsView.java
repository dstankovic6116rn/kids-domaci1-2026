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
import javafx.scene.layout.VBox;

public class CategoryDetailsView extends VBox {

  private final String categoryName;

  private Runnable onBackRequested = () -> {
  };

  private final ObservableList<ProcessItem> tableItems = FXCollections.observableArrayList();
  private final ObservableList<PieChart.Data> pieSlices = FXCollections.observableArrayList();
  private final Label totalLabel;

  public CategoryDetailsView(String categoryName, List<ProcessItem> processItems) {
    this.categoryName = categoryName;

    setMaxWidth(Double.MAX_VALUE);
    setMaxHeight(Double.MAX_VALUE);

    Button backBtn = new Button("Back");
    backBtn.getStyleClass().add("back-btn");
    backBtn.setOnAction(e -> onBackRequested.run());

    Label title = new Label(categoryName + " Category");

    HBox topBar = new HBox(24, backBtn, title);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 16, 10, 16));

    // Table view
    TableView<ProcessItem> table = buildTable(processItems);
    HBox.setHgrow(table, Priority.ALWAYS);

    Label chartTitleLabel = new Label("Top 10 processes by time spent");
    chartTitleLabel.getStyleClass().add("category-chart-section-title");

    // Pie chart
    PieChart uptimeChart = new PieChart(pieSlices);
    uptimeChart.setLegendVisible(true);
    uptimeChart.getStyleClass().add("main-pie-chart");
    VBox.setVgrow(uptimeChart, Priority.ALWAYS);

    // Total uptime label
    totalLabel = new Label();

    VBox rightPanel = new VBox(8, chartTitleLabel, uptimeChart, totalLabel);
    rightPanel.setAlignment(Pos.TOP_LEFT);
    HBox.setHgrow(rightPanel, Priority.ALWAYS);

    HBox body = new HBox(16, table, rightPanel);
    body.setPadding(new Insets(16));
    VBox.setVgrow(body, Priority.ALWAYS);

    getChildren().addAll(topBar, body);

    updateData(processItems);
  }

  private TableView<ProcessItem> buildTable(List<ProcessItem> processes) {
    TableView<ProcessItem> table = new TableView<>();

    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPlaceholder(new Label("No processes in this category."));

    // Process name column
    TableColumn<ProcessItem, String> nameCol = new TableColumn<>("Process");
    nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));

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
    // table.setItems(FXCollections.observableArrayList(processes));

    return table;
  }

  public void updateData(List<ProcessItem> processes) {
    tableItems.setAll(processes);

    // Recompute top 10 by uptime
    List<ProcessItem> top10 = processes.stream()
        .sorted(Comparator.comparingLong(
            ProcessItem::getUptimeSeconds).reversed())
        .limit(10)
        .collect(Collectors.toList());

    long top10Total = top10.stream().mapToLong(ProcessItem::getUptimeSeconds).sum();

    pieSlices.setAll(
        top10.stream()
            .map(p -> new PieChart.Data(
                p.getDisplayName(),
                top10Total > 0 ? p.getUptimeSeconds() : 1.0))
            .collect(Collectors.toList()));

    // Update total label
    long totalUptime = processes.stream()
        .mapToLong(
            ProcessItem::getUptimeSeconds)
        .sum();

    totalLabel.setText(categoryName + " Total time — " + TimeFormatter.formatTime(totalUptime));
  }

  public void setOnBackRequested(Runnable handler) {
    this.onBackRequested = handler;
  }

  public String getCategoryName() {
    return categoryName;
  }

}
