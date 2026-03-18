package org.example.controller;

import org.example.services.DataService;
import org.example.view.MainView;

public class MainController {
  private final ProcessListController processListController;

  public MainController(MainView mainView) {
    DataService dataService = new DataService();
    this.processListController = new ProcessListController(mainView.getProcessListView(), dataService);

  }

  public ProcessListController getProcessListController() {
    return processListController;
  }
}
