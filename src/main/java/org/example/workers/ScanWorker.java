package org.example.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import org.example.model.ProcessItem;

import oshi.software.os.OSProcess;

/**
 * Rekurzivno deli podniz procesa na pola dok ne bude dovoljno mali za obradu
 */

public class ScanWorker extends RecursiveTask<List<ProcessItem>> {

  private final List<OSProcess> processes;
  private final int from;
  private final int to;
  private final int treshold;

  public ScanWorker(List<OSProcess> processes, int from, int to, int treshold) {
    this.processes = processes;
    this.from = from;
    this.to = to;
    this.treshold = treshold;
  }

  @Override
  protected List<ProcessItem> compute() {
    int size = to - from;

    if (size <= treshold) {
      return processRange();
    }

    int mid = from + size / 2;
    ScanWorker left = new ScanWorker(processes, from, mid, treshold);
    ScanWorker right = new ScanWorker(processes, mid, to, treshold);

    left.fork();

    List<ProcessItem> rightResult = right.compute();
    List<ProcessItem> leftResult = left.join();

    List<ProcessItem> mergedResult = new ArrayList<>(leftResult.size() + rightResult.size());
    mergedResult.addAll(leftResult);
    mergedResult.addAll(rightResult);
    return mergedResult;
  }

  /**
   * Konvertuje pod-niz OSProcess-a u ProcessItem objekte
   */
  private List<ProcessItem> processRange() {
    List<ProcessItem> result = new ArrayList<>(to - from);

    for (int i = from; i < to; i++) {
      try {
        OSProcess rawProcess = processes.get(i);

        // getName() can return null if the process terminated before
        // the name was readable — skip rather than propagating a null
        String name = rawProcess.getName();
        if (name == null || name.isBlank())
          continue;

        // getProcessID() returns -1 if the PID is unavailable —
        // skip rather than storing a meaningless entry
        int pid = rawProcess.getProcessID();
        if (pid < 0)
          continue;

        ProcessItem processItem = new ProcessItem(
            pid,
            rawProcess.getStartTime(), // 0 if unavailable - ok
            name);

        // Can return Double.NaN if CPU data is unavailable
        double cpu = rawProcess.getProcessCpuLoadCumulative();
        processItem.setCpuUsage(Double.isNaN(cpu) ? Double.NaN : cpu * 100.0);

        processItem.setRamUsageMb(rawProcess.getResidentSetSize() / (1024.0 * 1024.0));

        result.add(processItem);

      } catch (Exception e) {
        // Process terminated or OS denied access mid-scan — skip silently.
        System.out.println("[ProcessScanService] Skipped process at index " + i
            + ": " + e.getClass().getSimpleName() + " — " + e.getMessage());
      }
    }

    return result;
  }

}
