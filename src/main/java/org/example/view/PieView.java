package org.example.view;

import org.example.model.PieChartData;

import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PieView {

  private final VBox root;
  private final PieChart pieChart;

  public PieView() {
    pieChart = new PieChart();
    pieChart.setTitle("Process Categories");
    pieChart.setLegendVisible(true);
    pieChart.getStyleClass().add("pie-chart");
    VBox.setVgrow(pieChart, Priority.ALWAYS);

    root = new VBox(16, pieChart);
    root.getStyleClass().add("right-panel");
    root.setPadding(new Insets(12));

  }

  public void bindPieData(PieChartData data) {
    pieChart.setTitle(data.getTitle());
    pieChart.setData(data.getSlices());
  };

  public VBox getRoot() {
    return root;
  }
}
