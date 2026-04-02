package org.example.controller;

import org.example.services.DataService;
import org.example.view.ProcessDialog;
import org.example.view.ToolbarView;

import javafx.scene.layout.StackPane;

public class ToolbarController {

  private final ToolbarView toolbarView;
  private final DataService dataService;
  private final StackPane rootPane;
  private final Runnable onShutdown;

  public ToolbarController(ToolbarView toolbarView, DataService dataService, StackPane rootPane, Runnable onShutdown) {
    this.toolbarView = toolbarView;
    this.dataService = dataService;
    this.rootPane = rootPane;
    this.onShutdown = onShutdown;

    bindActions();
  }

  private void bindActions() {
    toolbarView.setOnSave(this::onSave);
    toolbarView.setOnLoad(this::onLoad);
    toolbarView.setOnShutdown(this::onShutdownClicked);
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

  private void onShutdownClicked() {
    System.out.println("Shutdown clicked");

    ProcessDialog dialog = ProcessDialog.confirmDialog(
        "Shutdown?",
        "Current process data will be saved before the application exits.",
        "Shutdown",
        this::confirmShutdown);

    dialog.setOnDismiss(() -> rootPane.getChildren().removeIf(n -> n instanceof ProcessDialog));

    rootPane.getChildren().add(dialog);
  }

  private void confirmShutdown() {
    rootPane.getChildren().removeIf(n -> n instanceof ProcessDialog);

    toolbarView.setDisabled(true);
    toolbarView.showSavingStatus();

    onShutdown.run();
  }

}
