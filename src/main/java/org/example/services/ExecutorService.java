package org.example.services;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.example.model.AppConfig;
import org.example.model.ProcessItem;

import javafx.application.Platform;

/**
 * Za sada je namerno Scheduled Single-Thread da bi upisivanje u fajlove bilo
 * sekvencijalno i da bi se izbeglo konkurentno upisivanje u isti fajl
 * 
 * 1. DataService startuje Executor Service
 * 2. Koristi Config Reader da procita config.properties
 * 3. Zakazuje periodicna skeniranja sa fiksnim delay-om. Koristi se fixed delay
 * umesto fixed rate zbog slucaja da se zapoceti sken ne zavrsi u roku od tri
 * sekunde. U tom slucaju sledeci ceka a ne pokrece se preko prethodnog.
 * 4. Pokrece cuvanje in-memory procesa u zadati fajl
 * 
 */

public class ExecutorService {

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "File Executor Thread");
    t.setDaemon(true);
    return t;
  });

  private final ConfigReader configReader = new ConfigReader();
  private final DataService dataService;
  private final JsonWritter jsonWritter = new JsonWritter();

  private ScheduledFuture<?> scanJob;

  // Callback koji okida FX Thread kada se posao zavrsi
  private Runnable onScanComplete = () -> {
  };

  private volatile AppConfig config;

  public ExecutorService(DataService dataService) {
    this.dataService = dataService;
  }

  public void start() {
    executor.submit(() -> {
      config = configReader.readConfig();
      scheduleScan(config.getScanIntervalMS());
    });
  }

  private void scheduleScan(long interval) {
    /**
     * Ukoliko postoji schedule-ovan posao koji traje kada stigne novi, necemo ga
     * nasilno prekidati
     * i da ne bismo gomilali poslove saljemo cancel(false) koji nema trenutni
     * interrupt
     * vec dozvoljava da se posao zavrsi pre nego dodamo novi
     */
    if (scanJob != null && !scanJob.isDone()) {
      scanJob.cancel(false);
    }
    System.out.println("[FileExecutorService] Scheduling scan every "
        + interval + "ms");

    scanJob = executor.scheduleWithFixedDelay(this::runScan, 0, interval, TimeUnit.MILLISECONDS);
  }

  private void runScan() {

    try {
      dataService.scanAndUpdate();
      Platform.runLater(onScanComplete);
    } catch (Exception e) {
      System.err.println("[FileExecutorService] Scan failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void submitSave(List<ProcessItem> processes, Consumer<Boolean> onComplete) {
    executor.submit(() -> {
      if (config == null) {
        System.err.println("[FileExecutorService] Save skipped — config not yet loaded.");
        Platform.runLater(() -> onComplete.accept(false));
        return;
      }
      try {
        jsonWritter.write(processes, config.getMappingFile());
        Platform.runLater(() -> onComplete.accept(true));
      } catch (Exception e) {
        System.err.println("[FileExecutorService] Save failed: " + e.getMessage());
        e.printStackTrace();
        Platform.runLater(() -> onComplete.accept(false));
      }
    });

  }

  public void setOnScanComplete(Runnable handler) {
    this.onScanComplete = handler;
  }

  /**
   * Genericni job za submit
   */
  public <T> Future<T> submitJob(Callable<T> job) {
    return executor.submit(job);
  }

  public void shutdown() {
    executor.shutdown();
  }

}
