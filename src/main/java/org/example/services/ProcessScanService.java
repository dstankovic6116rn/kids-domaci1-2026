package org.example.services;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

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
    List<OSProcess> active = rawProcesses.stream().filter(p -> isActiveProcess(p)).toList();

    /**
     * invoke() submit-uje task i blokira pozivajuci thread dok se ne zavrsi i vraca
     * rezultat direktno
     * ProcessListController hendla ne blokirajuce ponasanje sa runLater() pa se
     * FxThread ne blokira
     * Zato nam nije potreban Future kao povratni tip
     */
    return forkJoinPool.invoke(new ScanWorker(active, 0, active.size(), SPLIT_THRESHOLD));

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
