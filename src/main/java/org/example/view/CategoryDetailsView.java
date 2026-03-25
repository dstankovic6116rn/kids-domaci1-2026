package org.example.view;

import java.util.List;

import org.example.model.ProcessItem;

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
import javafx.scene.control.cell.PropertyValueFactory;
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

    Label title = new Label(categoryName);
    title.getStyleClass().add("category-detail-title");

    Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);

    HBox topBar = new HBox(backBtn, title, headerSpacer);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 16, 10, 16));

    // Table view
    TableView<ProcessItem> table = buildTable(processItems);
    HBox.setHgrow(table, Priority.ALWAYS);

    // Pie Chart
    PieChart uptimeChart = buildUptimeChart(processItems);
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
    nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
    nameCol.getStyleClass().add("table-col-name");

    // CPU usage column
    TableColumn<ProcessItem, Double> cpuCol = new TableColumn<>("CPU %");
    cpuCol.setCellValueFactory(new PropertyValueFactory<>("cpuUsage"));
    cpuCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : String.format("%.1f%%", value));
      }
    });

    // RAM usage column
    TableColumn<ProcessItem, Double> ramCol = new TableColumn<>("RAM (MB)");
    ramCol.setCellValueFactory(new PropertyValueFactory<>("ramUsageMb"));
    ramCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : String.format("%.1f", value));
      }
    });

    // Uptime column
    TableColumn<ProcessItem, Long> uptimeCol = new TableColumn<>("Uptime");
    uptimeCol.setCellValueFactory(new PropertyValueFactory<>("uptimeSeconds"));
    uptimeCol.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(Long value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : formatUptime(value));
      }
    });

    table.getColumns().addAll(nameCol, cpuCol, ramCol, uptimeCol);
    table.setItems(FXCollections.observableArrayList(processes));

    return table;
  }

  private PieChart buildUptimeChart(List<ProcessItem> processes) {
    ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();

    long totalUptime = processes.stream()
        .mapToLong(
            ProcessItem::getUptimeSeconds)
        .sum();

    for (ProcessItem p : processes) {
      // If all processes have zero uptime, give each an equal slice
      double value = totalUptime > 0 ? p.getUptimeSeconds() : 1.0;
      slices.add(new PieChart.Data(p.getDisplayName(), value));
    }

    PieChart chart = new PieChart(slices);
    chart.setTitle("Uptime Distribution");
    chart.setLegendVisible(true);
    chart.getStyleClass().add("main-pie-chart");

    return chart;
  }

  private String formatUptime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    if (hours > 0)
      return String.format("%dh %dm %ds", hours, minutes, seconds);
    else if (minutes > 0)
      return String.format("%dm %ds", minutes, seconds);
    else
      return String.format("%ds", seconds);
  }

  public void setOnBackRequested(Runnable handler) {
    this.onBackRequested = handler;
  }

}
