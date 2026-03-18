package org.example.controller;

import org.example.model.ProcessItem;
import org.example.services.DataService;
import org.example.view.ProcessListView;

import javafx.collections.ObservableList;

public class ProcessListController {

  private final ProcessListView processListView;
  private final DataService dataService;

  ObservableList<ProcessItem> processItems;

  public ProcessListController(ProcessListView processListView, DataService dataService) {
    this.processListView = processListView;
    this.dataService = dataService;

    processListView.setOnLabelClicked(this::onLabelClicked);
    processListView.setOnCategoryChanged(this::onCategoryChanged);
    loadProcessItems();
  }

  private void onLabelClicked(ProcessItem item) {
    System.out.println("Label clicked: " + item.getProcessName());
    // TODO: open detail window for this item
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
