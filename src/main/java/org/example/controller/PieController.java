package org.example.controller;

import org.example.model.PieChartData;
import org.example.services.DataService;
import org.example.view.CategoryDetailsView;
import org.example.view.MainView;
import org.example.view.PieView;

public class PieController {

  private final PieView pieView;
  private final DataService dataService;
  private final MainView mainView;

  private PieChartData pieChartData;

  public PieController(PieView pieView, DataService dataService, MainView mainView) {
    this.pieView = pieView;
    this.dataService = dataService;
    this.mainView = mainView;

    pieView.setOnDetailsRequested(this::onDetailsRequested);
    loadPieChartData();
  }

  private void loadPieChartData() {
    pieChartData = dataService.loadChartData();
    pieView.bindPieData(pieChartData.getTitle(), pieChartData.getSlices());
  }

  private void onDetailsRequested(String category) {
    new CategoryDetailsControler(category);
    CategoryDetailsView detailsView = new CategoryDetailsView(category);
    detailsView.setOnBackRequested(mainView::showMain);
    mainView.showCategoryDetails(detailsView);

  }

  public PieChartData getPieChartData() {
    return pieChartData;
  }
}
