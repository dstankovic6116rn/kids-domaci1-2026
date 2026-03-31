package org.example.controller;

import org.example.model.ProcessItem;
import org.example.services.DataService;
import org.example.view.MainView;
import org.example.view.ProcessDetailsView;
import org.example.view.ProcessDialog;

public class ProcessDetailsController {

  private final ProcessDetailsView processDetailsView;
  private final ProcessItem processItem;
  private final DataService dataService;
  private final MainView mainView;
  private final Runnable onListRefreshNeeded;

  public ProcessDetailsController(ProcessDetailsView processDetailsView, ProcessItem processItem,
      DataService dataService, MainView mainView, Runnable onListRefreshNeeded) {
    this.processDetailsView = processDetailsView;
    this.processItem = processItem;
    this.dataService = dataService;
    this.mainView = mainView;
    this.onListRefreshNeeded = onListRefreshNeeded;

    processDetailsView.setOnKillProcess(this::onKillProcess);
    processDetailsView.setOnChangeProccessName(this::onChangeProccessName);
    processDetailsView.setOnFreezeTracking(this::onFreezeTracking);
    processDetailsView.setOnChangeProcessCategory(this::onChangeProcessCategory);
    processDetailsView.updateFreezeLabel(dataService.isFrozen(processItem.getOriginalName()));

  }

  private void onKillProcess() {
    System.out.println("Kill process: " + processItem.getDisplayName());

    ProcessDialog dialog = ProcessDialog.confirmDialog("Kill Process",
        "Kill " + processItem.getDisplayName() + " (PID " + processItem.getPid() + ")?\n"
            + "This will terminate the process immediately.",
        "Kill", this::executeKill);

    dialog.setOnDismiss(processDetailsView::hideDialog);
    processDetailsView.showDialog(dialog);
  }

  private void executeKill() {
    long pid = processItem.getPid();

    /*
     * Oshi ne podrzava kill process pa koristimo javin ProcessHandle
     */
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ProcessHandle.html#of(long)
    ProcessHandle.of(pid).ifPresentOrElse(
        handle -> {
          // destroy() sends SIGTERM (graceful); destroyForcibly() sends SIGKILL
          boolean sent = handle.destroy();
          if (sent) {
            System.out.println("Terminate signal sent to PID " + pid
                + " (" + processItem.getDisplayName() + ")");
          } else {
            System.err.println("Failed to send terminate signal to PID " + pid
                + " — process may have already exited");
          }
        }, () -> System.err.println("PID " + pid + " not found — already exited"));

    dataService.removeProcess(processItem.getOriginalName());
    mainView.showPieView();
    onListRefreshNeeded.run();
  }

  private void onChangeProccessName() {
    System.out.println("Change process name: " + processItem.getDisplayName());

    ProcessDialog dialog = ProcessDialog.textInputDialog(
        "Change process name",
        "Enter new name for " + processItem.getDisplayName(),
        "Save",
        this::executeChangeName);
    dialog.setOnDismiss(processDetailsView::hideDialog);
    processDetailsView.showDialog(dialog);
  }

  private void executeChangeName(String newName) {
    processItem.setAliasName(newName);
    processDetailsView.updateNameLabel(newName);
    onListRefreshNeeded.run(); // da osvezimo listu i prikazemo alias

    System.out.println("Name changed: " + processItem.getOriginalName()
        + " → " + newName);
  }

  private void onFreezeTracking() {
    boolean isFrozen = dataService.isFrozen(processItem.getOriginalName());

    if (isFrozen) {
      ProcessDialog dialog = ProcessDialog.confirmDialog(
          "Freeze time tracking?",
          "Stop tracking time for " + processItem.getDisplayName() + "?\n"
              + "Uptime will pause until tracking is resumed.",
          "Freeze",
          this::executeUnfreezeTracking);

      dialog.setOnDismiss(processDetailsView::hideDialog);
      processDetailsView.showDialog(dialog);
    } else {
      ProcessDialog dialog = ProcessDialog.confirmDialog(
          "Freeze time tracking?",
          "Stop tracking time for " + processItem.getDisplayName() + "?\n"
              + "Uptime will pause until tracking is resumed.",
          "Freeze",
          this::executeFreezeTracking);

      dialog.setOnDismiss(processDetailsView::hideDialog);
      processDetailsView.showDialog(dialog);
    }

  }

  private void executeFreezeTracking() {
    dataService.freezeUptime(processItem.getOriginalName());
    processDetailsView.updateFreezeLabel(true);

    System.out.println("Freeze tracking — " + processItem.getDisplayName());
  }

  private void executeUnfreezeTracking() {
    dataService.unfreezeUptime(processItem.getOriginalName());
    processDetailsView.updateFreezeLabel(false);

    System.out.println("Uptime resumed — " + processItem.getDisplayName());
  }

  private void onChangeProcessCategory() {
    ProcessDialog dialog = ProcessDialog.categoryPickerDialog(
        "Change category",
        ProcessItem.CATEGORY_OPTIONS,
        this::executeChangeCategory);
    dialog.setOnDismiss(processDetailsView::hideDialog);
    processDetailsView.showDialog(dialog);
  }

  private void executeChangeCategory(String newCategory) {
    dataService.setProcessCategory(processItem.getOriginalName(), newCategory);
    onListRefreshNeeded.run();

    System.out.println("Category changed: " + processItem.getDisplayName()
        + " → " + newCategory);
    // Analytics thread osvezava view na sledecem run-u
  }
}
