package org.example.controller;

import org.example.services.AnalyticsService;
import org.example.services.DataService;
import org.example.view.MainView;

/**
 * Application level controller
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
    this.analyticsService = new AnalyticsService(dataService, pieController);

    this.toolbarController = new ToolbarController(mainView.getToolbarView(), dataService, mainView.getMainPage(),
        onShutdown);

    mainView.setAnalyticsService(analyticsService);

    dataService.start(() -> {
      processListController.onScanComplete();
      analyticsService.nudgeRun();
    });

    analyticsService.start();
  }

  public void shutdown() {
    analyticsService.shutdown();
    dataService.shutdown();
  }

  public ProcessListController getProcessListController() {
    return processListController;
  }

  public ToolbarController getToolbarController() {
    return toolbarController;
  }

  public PieController getPieController() {
    return pieController;
  }

  public AnalyticsService getAnalyticsService() {
    return analyticsService;
  }
}
