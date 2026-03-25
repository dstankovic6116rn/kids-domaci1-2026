package org.example.controller;

import org.example.services.DataService;
import org.example.view.CategoryDetailsView;
import org.example.view.MainView;
import org.example.view.PieView;

import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

public class PieController {

  private final PieView pieView;
  private final DataService dataService;
  private final MainView mainView;

  public PieController(PieView pieView, DataService dataService, MainView mainView) {
    this.pieView = pieView;
    this.dataService = dataService;
    this.mainView = mainView;

    pieView.setOnDetailsRequested(this::onDetailsRequested);
  }

  /**
   * ProcessListController poziva loadPiewChartData() preko onChartRefreshNeeded
   * kada se skeniranje procesa zavrsi i nakon svake promene kategorije
   */
  public void loadPieChartData() {
    ObservableList<PieChart.Data> slices = dataService.buildProcessCategoryPieData();
    pieView.bindPieData("Category Overview", slices);
  }

  private void onDetailsRequested(String category) {
    new CategoryDetailsControler(category);

    CategoryDetailsView detailsView = new CategoryDetailsView(category);
    detailsView.setOnBackRequested(mainView::showMain);

    mainView.showCategoryDetails(detailsView);

  }
}
