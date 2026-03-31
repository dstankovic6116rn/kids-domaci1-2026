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

  public ProcessListController(ProcessListView processListView, DataService dataService, MainView mainView) {
    this.processListView = processListView;
    this.dataService = dataService;
    this.mainView = mainView;
    this.processItems = FXCollections.observableArrayList();

    processListView.setOnLabelClicked(this::onLabelClicked);
    processListView.setOnCategoryChanged(this::onCategoryChanged);

  }

  /**
   * Refreshuje listu procesa sa najnovijim podacima iz DataService-a nakon svakog
   * skeniranja.
   * Upate chart-a i metrika se hendluje iz AnalyticsService-a koji osluskuje
   * promene u DataService-u i obavestava FX Thread da osvezi prikaz pozivom
   * Platform.runLater(() -> processListController.onScanComplete())
   */
  public void onScanComplete() {
    List<ProcessItem> result = dataService.getCurrentProcceses();
    processItems.setAll(result);
    processListView.setItems(processItems);
  }

  private void onLabelClicked(ProcessItem item) {
    ProcessRanking ranking = dataService.getRankingForProcess(item.getOriginalName());
    long liveUptime = dataService.getLiveUptime(item.getOriginalName());
    ProcessDetailsView processDetailsView = new ProcessDetailsView(item, ranking, liveUptime);

    processDetailsView.setOnBackRequested(mainView::showPieView);

    new ProcessDetailsController(processDetailsView, item, dataService, mainView, this::refreshList);
    mainView.showProcessDetails(processDetailsView);
  }

  private void onCategoryChanged(ProcessItem item, String category) {
    dataService.setProcessCategory(item.getOriginalName(), category);
    System.out.println("Value changed for:" + item.getOriginalName() + " " + item.getCategory() + " → " + category);
  }

  public void refreshList() {
    processListView.forceNextRebuild();
    processItems.setAll(dataService.getCurrentProcceses());
    processListView.setItems(processItems);
  }

  public ObservableList<ProcessItem> getProcessItems() {
    return processItems;
  }

}
