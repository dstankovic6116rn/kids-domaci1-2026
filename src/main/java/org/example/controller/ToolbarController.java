package org.example.controller;

import org.example.services.DataService;
import org.example.view.ToolbarView;

public class ToolbarController {

  private final ToolbarView toolbarView;
  private final DataService dataService;

  public ToolbarController(ToolbarView toolbarView, DataService dataService) {
    this.toolbarView = toolbarView;
    this.dataService = dataService;

    bindActions();
  }

  private void bindActions() {
    toolbarView.setOnSave(this::onSave);
    toolbarView.setOnLoad(this::onLoad);
    toolbarView.setOnShutdown(this::onShutdown);
  }

  private void onSave() {
    System.out.println("Save clicked");

    dataService.saveProcesses(success -> {
      toolbarView.showSaveStatus(true);
    });
  }

  private void onLoad() {
    System.out.println("Load clicked");
  }

  private void onShutdown() {
    System.out.println("Shutdown clicked");
  }

}
