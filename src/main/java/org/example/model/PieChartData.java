package org.example.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

// TODO: Ne koristi se izbrisi
public class PieChartData {

  private String title;

  private final ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();

  public PieChartData(String title) {
    this.title = title;
  }

  public ObservableList<PieChart.Data> getSlices() {
    return slices;
  }

  // Zamenjuje sve slice-ove
  public void setSlices(ObservableList<PieChart.Data> newSlices) {
    slices.setAll(newSlices);
  }

  // Dodaje jedan slice
  public void addSlice(String name, double value) {
    slices.add(new PieChart.Data(name, value));
  }

  public void clearSlices() {
    slices.clear();
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String t) {
    this.title = t;
  }

}
