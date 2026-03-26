package org.example.view;

import java.util.Map;
import java.util.function.Consumer;

import org.example.utils.TimeFormatter;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PieView {

  private final VBox root;
  private final PieChart pieChart;
  private final VBox detailsBox;

  private Consumer<String> onDetailsRequested = name -> {
  };

  public PieView() {
    pieChart = new PieChart();
    pieChart.setTitle("Process Categories");
    pieChart.setLegendVisible(true);
    pieChart.getStyleClass().add("pie-chart");
    VBox.setVgrow(pieChart, Priority.ALWAYS);

    detailsBox = new VBox(8);
    detailsBox.getStyleClass().add("details-box");
    detailsBox.setPadding(new Insets(8));

    root = new VBox(16, pieChart, detailsBox);
    root.getStyleClass().add("right-panel");
    root.setPadding(new Insets(12));

  }

  public void bindPieData(String title, ObservableList<PieChart.Data> slices, Map<String, Long> categoryUptimes) {
    pieChart.setTitle(title);
    pieChart.setData(slices);
    buildDetailsRows(slices, categoryUptimes);
  };

  private void buildDetailsRows(ObservableList<PieChart.Data> slices, Map<String, Long> categoryUptimes) {
    detailsBox.getChildren().clear();

    for (PieChart.Data slice : slices) {
      long uptime = categoryUptimes.getOrDefault(slice.getName(), 0L);
      detailsBox.getChildren().add(buildDetailsRow(slice, uptime));
    }
  }

  private HBox buildDetailsRow(PieChart.Data slice, Long uptimeSeconds) {
    Label nameLabel = new Label(slice.getName());
    nameLabel.getStyleClass().add("data-heading");

    Label valueLabel = new Label(TimeFormatter.formatTime(uptimeSeconds));
    valueLabel.getStyleClass().add("data-value");

    Button detailsBtn = new Button("Details");
    detailsBtn.getStyleClass().add("details-btn");
    detailsBtn.setOnAction(e -> onDetailsRequested.accept(slice.getName()));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox row = new HBox(8, nameLabel, spacer, valueLabel, detailsBtn);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("summary-row");
    row.setPadding(new Insets(4, 0, 4, 0));

    return row;
  }

  public void setOnDetailsRequested(Consumer<String> callback) {
    this.onDetailsRequested = callback;
  }

  public VBox getRoot() {
    return root;
  }
}
