package org.example.controller;

import org.example.model.ProcessItem;
import org.example.view.ProcessDetailsView;

public class ProcessDetailsController {

  private final ProcessDetailsView processDetailsView;
  private final ProcessItem processItem;

  public ProcessDetailsController(ProcessDetailsView processDetailsView, ProcessItem processItem) {
    this.processDetailsView = processDetailsView;
    this.processItem = processItem;

    processDetailsView.setOnKillProcess(this::onKillProcess);
    processDetailsView.setOnChangeProccessName(this::onChangeProccessName);
    processDetailsView.setOnFreezeTracking(this::onFreezeTracking);
    processDetailsView.setOnChangeProcessCategory(this::onChangeProcessCategory);
  }

  private void onKillProcess() {
    System.out.println("Kill process: " + processItem.getProcessName());
  }

  private void onChangeProccessName() {
    System.out.println("Change process name: " + processItem.getProcessName());
  }

  private void onFreezeTracking() {
    System.out.println("Freeze tracking: " + processItem.getProcessName());
  }

  private void onChangeProcessCategory() {
    System.out.println("Change process category: " + processItem.getProcessName());
  }
}
