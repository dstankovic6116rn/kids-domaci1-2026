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
      OSProcess rawProcess = processes.get(i);

      ProcessItem processItem = new ProcessItem(rawProcess.getProcessID(), rawProcess.getStartTime(),
          rawProcess.getName());

      // https://www.oshi.ooo/oshi-core-java11/apidocs/com.github.oshi/oshi/software/os/OSProcess.html#getProcessCpuLoadCumulative()
      processItem.setCpuUsage(rawProcess.getProcessCpuLoadCumulative() * 100.0);
      // https://www.oshi.ooo/oshi-core-java11/apidocs/com.github.oshi/oshi/software/os/OSProcess.html#getResidentSetSize()
      processItem.setRamUsageMb(rawProcess.getResidentSetSize() / (1024.0 * 1024.0));
      // https://www.oshi.ooo/oshi-core-java11/apidocs/com.github.oshi/oshi/software/os/OSProcess.html#getUpTime()
      processItem.setUptimeSeconds(rawProcess.getUpTime() / 1000L);

      result.add(processItem);
    }

    return result;
  }

}
