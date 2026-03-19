package org.example.services;

import java.util.List;

import org.example.model.PieChartData;
import org.example.model.ProcessItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

public class DataService {
  private final List<String> options = List.of("Work", "Fun", "Other");

  public ObservableList<ProcessItem> loadItemPairs() {
    // Each ItemPair takes: label, initial value, list of 3 possible values.
    return FXCollections.observableArrayList(
        new ProcessItem("VS Code", "Work",
            options),
        new ProcessItem("Chrome", "Fun",
            options),
        new ProcessItem("Pages", "Other",
            options),
        new ProcessItem("YouTube", "Fun",
            options),
        new ProcessItem("Mail", "Work",
            options));
  }

  public PieChartData loadChartData() {
    PieChartData model = new PieChartData("Overview");
    model.setSlices(FXCollections.observableArrayList(
        new PieChart.Data("Work", 35),
        new PieChart.Data("Fun", 25),
        new PieChart.Data("Other", 20)));
    return model;
  }

}
