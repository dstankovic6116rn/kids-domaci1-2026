package org.example.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.example.model.ProcessItem;
import org.example.workers.ScanWorker;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OperatingSystem;

/**
 * Skenira sistemske procese koristeci Oshi
 * Rezultati se vracaju nazad pozivaocu kao flat lista
 * 
 * Fork/join pool se kreira samo jednom i koristi prilikom vise skeniranja
 * 
 * OSHI OSProcess API
 * ForkJoinPool / RecursiveTask pattern
 */

public class ProcessScanService {
  private static final int SPLIT_THRESHOLD = 50;
  private final ForkJoinPool forkJoinPool;

  // https://javadoc.io/doc/com.github.oshi/oshi-core/latest/oshi/software/os/OperatingSystem.html
  private final OperatingSystem os;

  public ProcessScanService() {
    this.forkJoinPool = new ForkJoinPool();
    this.os = new SystemInfo().getOperatingSystem();
  }

  public List<ProcessItem> scan() {

    List<OSProcess> rawProcesses = os.getProcesses();
    if (rawProcesses == null) {
      System.err.println("[ProcessScanService] os.getProcesses() returned null — skipping scan");
      return new ArrayList<>();
    }

    List<OSProcess> active;

    try {
      active = rawProcesses.stream().filter(p -> {
        try {
          return isActiveProcess(p);
        } catch (Exception e) {
          return false;
        }
      }).collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("[ProcessScanService] Failed to filter process list: "
          + e.getMessage());
      return new ArrayList<>();
    }

    /**
     * invoke() submit-uje task i blokira pozivajuci thread dok se ne zavrsi i vraca
     * rezultat direktno
     * ProcessListController hendla ne blokirajuce ponasanje sa runLater() pa se
     * FxThread ne blokira
     * Zato nam nije potreban Future kao povratni tip
     */
    try {
      return forkJoinPool.invoke(new ScanWorker(active, 0, active.size(), SPLIT_THRESHOLD));
    } catch (Exception e) {
      // ForkJoinPool re-throws exceptions from tasks via join()
      System.err.println("[ProcessScanService] ForkJoin scan failed: " + e.getMessage());
      e.printStackTrace();
      return new ArrayList<>();
    }

  }

  public void shutdown() {
    forkJoinPool.shutdown();
  }

  private Boolean isActiveProcess(OSProcess process) {
    State processState = process.getState();

    // https://javadoc.io/doc/com.github.oshi/oshi-core/latest/oshi/software/os/OSProcess.State.html#enum-constant-summary
    if (processState == OSProcess.State.RUNNING || processState == OSProcess.State.WAITING
        || processState == OSProcess.State.SLEEPING) {
      return true;
    } else
      return false;
  }
}
