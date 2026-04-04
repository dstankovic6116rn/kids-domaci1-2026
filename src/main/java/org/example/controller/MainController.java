package org.example.controller;

import org.example.services.AnalyticsService;
import org.example.services.DataService;
import org.example.services.ExecutorService;
import org.example.view.MainView;

/**
 * Application level controller
 * Konstruise sve kontrolere i AnalyticsService, povezuje ih i startuje proces
 * skeniranja i analitike
 */
public class MainController {
  private final ProcessListController processListController;
  private final ToolbarController toolbarController;
  private final PieController pieController;
  private final AnalyticsService analyticsService;
  private final DataService dataService;

  public MainController(MainView mainView, DataService dataService, Runnable onShutdown) {
    this.dataService = dataService;
    this.processListController = new ProcessListController(mainView.getProcessListView(), dataService, mainView);
    this.pieController = new PieController(mainView.getPieView(), dataService, mainView);

    ExecutorService es = dataService.getExecutorService();
    this.analyticsService = new AnalyticsService(dataService, pieController, es::getConfig,
        es::submitFixedTimeSnapshot);

    this.toolbarController = new ToolbarController(mainView.getToolbarView(), dataService, mainView.getMainPage(),
        onShutdown);

    mainView.setAnalyticsService(analyticsService);

    dataService.start(processListController::onScanComplete, mainView.getToolbarView()::showSnapshotStatus);

    analyticsService.start();
  }

  public void shutdown() {
    analyticsService.shutdown();
    dataService.shutdown();
  }

}
