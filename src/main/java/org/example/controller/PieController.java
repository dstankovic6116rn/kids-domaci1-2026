package org.example.controller;

import org.example.model.PieChartData;
import org.example.services.DataService;
import org.example.view.PieView;

public class PieController {

  private final PieView pieView;
  private final DataService dataService;

  private PieChartData pieChartData;

  public PieController(PieView pieView, DataService dataService) {
    this.pieView = pieView;
    this.dataService = dataService;
    loadPieChartData();
  }

  private void loadPieChartData() {
    pieChartData = dataService.loadChartData();
    pieView.bindPieData(pieChartData);
  }

  public PieChartData getPieChartData() {
    return pieChartData;
  }
}
