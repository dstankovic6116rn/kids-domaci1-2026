package org.example.controller;

import org.example.services.DataService;
import org.example.view.MainView;

public class MainController {
  private final ProcessListController processListController;
  private final ToolbarController toolbarController;
  private final PieController pieController;

  public MainController(MainView mainView) {
    DataService dataService = new DataService();
    this.processListController = new ProcessListController(mainView.getProcessListView(), dataService);
    this.toolbarController = new ToolbarController(mainView.getToolbarView());
    this.pieController = new PieController(mainView.getPieView(), dataService);

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
}
