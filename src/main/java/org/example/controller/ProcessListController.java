package org.example.controller;

import java.util.List;
import org.example.model.ProcessItem;
import org.example.model.ProcessRanking;
import org.example.services.DataService;
import org.example.view.MainView;
import org.example.view.ProcessDetailsView;
import org.example.view.ProcessListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Upravlja listom procesa
 * Expose-uje onScanComplete koji se poziva na FX Thread-u iz Executor Service-a
 * preko Platform.runLater nakon svakog skeniranja
 */

public class ProcessListController {

  private final ProcessListView processListView;
  private final DataService dataService;
  private final MainView mainView;

  ObservableList<ProcessItem> processItems;
  private Runnable onChartRefreshNeeded = () -> {
  };

  public ProcessListController(ProcessListView processListView, DataService dataService, MainView mainView) {
    this.processListView = processListView;
    this.dataService = dataService;
    this.mainView = mainView;
    this.processItems = FXCollections.observableArrayList();

    processListView.setOnLabelClicked(this::onLabelClicked);
    processListView.setOnCategoryChanged(this::onCategoryChanged);

  }

  public void onScanComplete() {
    List<ProcessItem> result = dataService.getCurrentProcceses();
    processItems.setAll(result);
    processListView.setProcessItems(processItems);
    onChartRefreshNeeded.run();
  }

  private void onLabelClicked(ProcessItem item) {
    ProcessRanking ranking = dataService.getRankingForProcess(item.getOriginalName());
    ProcessDetailsView processDetailsView = new ProcessDetailsView(item, ranking);

    processDetailsView.setOnBackRequested(mainView::showPieView);

    new ProcessDetailsController(processDetailsView, item);
    mainView.showProcessDetails(processDetailsView);
  }

  private void onCategoryChanged(ProcessItem item, String category) {
    dataService.setProcessCategory(item.getOriginalName(), category);
    System.out.println("Value changed for:" + item.getOriginalName() + " " + item.getCategory() + " → " + category);

    onChartRefreshNeeded.run();
  }

  public void setOnChartRefreshNeeded(Runnable handler) {
    this.onChartRefreshNeeded = handler;
  }

  public ObservableList<ProcessItem> getProcessItems() {
    return processItems;
  }

}
