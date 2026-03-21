package org.example.controller;

import java.util.List;

import org.example.model.ProcessItem;
import org.example.services.DataService;
import org.example.view.MainView;
import org.example.view.ProcessDetailsView;
import org.example.view.ProcessListView;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ProcessListController {

  private final ProcessListView processListView;
  private final DataService dataService;
  private final MainView mainView;

  ObservableList<ProcessItem> processItems;

  public ProcessListController(ProcessListView processListView, DataService dataService, MainView mainView) {
    this.processListView = processListView;
    this.dataService = dataService;
    this.mainView = mainView;
    this.processItems = FXCollections.observableArrayList();

    processListView.setOnLabelClicked(this::onLabelClicked);
    processListView.setOnCategoryChanged(this::onCategoryChanged);
    triggerScan();
  }

  private void triggerScan() {
    Thread scanner = new Thread(() -> {
      List<ProcessItem> result = dataService.scanAndUpdate();
      Platform.runLater(() -> {
        processItems.setAll(result);
        processListView.setProcessItems(processItems);
      });
    });

    scanner.setDaemon(true);
    scanner.setName("Process Scanner Thread");
    scanner.start();
  }

  private void onLabelClicked(ProcessItem item) {
    System.out.println("Label clicked: " + item.getOriginalName());
    ProcessDetailsView processDetailsView = new ProcessDetailsView(item);
    processDetailsView.setOnBackRequested(mainView::showPieView);

    new ProcessDetailsController(processDetailsView, item);
    mainView.showProcessDetails(processDetailsView);
  }

  private void onCategoryChanged(ProcessItem item, String category) {
    System.out.println("Value changed for:" + item.getOriginalName() + " " + item.getCategory() + " → " + category);
    dataService.setProcessCategory(item.getOriginalName(), category);

  }

  public ObservableList<ProcessItem> getProcessItems() {
    return processItems;
  }

}
