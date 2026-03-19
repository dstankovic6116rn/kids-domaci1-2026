package org.example.controller;

import org.example.model.ProcessItem;
import org.example.services.DataService;
import org.example.view.MainView;
import org.example.view.ProcessDetailsView;
import org.example.view.ProcessListView;

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

    processListView.setOnLabelClicked(this::onLabelClicked);
    processListView.setOnCategoryChanged(this::onCategoryChanged);
    loadProcessItems();
  }

  private void onLabelClicked(ProcessItem item) {
    System.out.println("Label clicked: " + item.getProcessName());
    ProcessDetailsView processDetailsView = new ProcessDetailsView(item);
    processDetailsView.setOnBackRequested(mainView::showPieView);

    new ProcessDetailsController(processDetailsView, item);
    // TODO show Process Details view
    mainView.showProcessDetails(processDetailsView);
  }

  private void onCategoryChanged(ProcessItem item, String newValue) {
    System.out.println("Value changed: " + item.getCategory() + " → " + newValue);
    // TODO: persist the change, notify other controllers if needed
  }

  private void loadProcessItems() {
    processItems = dataService.loadItemPairs();
    processListView.setProcessItems(processItems);
  }

  public ObservableList<ProcessItem> getProcessItems() {
    return processItems;
  }

}
