package org.example.services;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.example.model.AppConfig;
import org.example.model.ProcessItem;
import org.example.workers.ConfigReader;
import org.example.workers.CsvWritter;
import org.example.workers.JsonReader;
import org.example.workers.JsonWritter;

import javafx.application.Platform;

/**
 * DataService startuje Executor Service Thread-ove
 * 1. Read config (scanExecutor)
 * 2. Load JSON history (fileExecutor)
 * 3. Zakazi periodicni CSV snapshot (fileExecutor)
 * 4. Zakazi periodicni scan (scanExecutor)
 * 
 * ScanExecutor:
 * 1. Zakazuje periodicna skeniranja sa fiksnim delay-om. Koristi se fixed delay
 * umesto fixed rate zbog slucaja da se zapoceti sken ne zavrsi u roku od tri
 * sekunde. U tom slucaju sledeci ceka a ne pokrece se preko prethodnog.
 * 
 * FileExecutor (File I/O):
 * 1. Koristi Config Reader da procita config.properties
 * 2. Pokrece cuvanje in-memory procesa u zadati fajl
 * 3. Na startu ucitava istorijske podatke iz fajla i puni
 * ProcessData.historicData koji se koristi za merge-ovanje sa novim skenovima
 */

public class ExecutorService {

  private final ScheduledExecutorService scanExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "Scan Executor Thread");
    t.setDaemon(true);
    return t;
  });

  private final ScheduledExecutorService fileExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "File Executor Thread");
    t.setDaemon(true);
    return t;
  });

  private final DataService dataService;

  private final ConfigReader configReader = new ConfigReader();
  private final JsonWritter jsonWritter = new JsonWritter();
  private final JsonReader jsonReader = new JsonReader();
  private final CsvWritter csvWritter = new CsvWritter();

  private ScheduledFuture<?> scanJob;

  // Callback koji okida FX Thread kada se posao zavrsi
  private volatile Runnable onScanComplete = () -> {
  };

  private volatile AppConfig config;

  public ExecutorService(DataService dataService) {
    this.dataService = dataService;
  }

  public void start() {
    scanExecutor.submit(() -> {
      // Read Config
      try {
        config = configReader.readConfig();

        // Prepusti file executor-u da procita, scanExecutor je slobodan nakon submit-a
        fileExecutor.submit(() -> {

          try {
            List<ProcessItem> historic = jsonReader.read(config.getMappingFile());
            dataService.loadHistory(historic);

            scanExecutor.submit(() -> {
              try {
                scheduleCSVSnapshot(config.getSnapshotIntervalSec());
                scheduleScan(config.getScanIntervalMS());
              } catch (Exception e) {
                System.err.println("[ScanExecutor] Scan scheduling failed: "
                    + e.getMessage());
                e.printStackTrace();
              }
            });

          } catch (Exception e) {
            System.err.println("[FileExecutor] History load failed: "
                + e.getMessage());
            e.printStackTrace();

            // History failed but we can still start scanning from scratch
            scanExecutor.submit(() -> {
              try {
                scheduleCSVSnapshot(config.getSnapshotIntervalSec());
                scheduleScan(config.getScanIntervalMS());
              } catch (Exception e2) {
                System.err.println("[ScanExecutor] Scan scheduling failed: "
                    + e2.getMessage());
                e2.printStackTrace();
              }
            });
          }
        });

      } catch (Exception e) {
        System.err.println("[ScanExecutor] Config read failed: " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  private void scheduleScan(long intervalMS) {
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
    System.out.println("[ScanExecutor] Scheduling scan every "
        + intervalMS + "ms");

    scanJob = scanExecutor.scheduleWithFixedDelay(this::runScan, 0, intervalMS, TimeUnit.MILLISECONDS);
  }

  private void runScan() {

    try {
      dataService.scanAndUpdate();
      Platform.runLater(onScanComplete);
    } catch (Exception e) {
      System.err.println("[ScanExecutor] Scan failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Submit-uje JSON save posao FileExecutor-u
   * 
   * @param processes
   * @param onComplete
   */
  public void submitSave(List<ProcessItem> processes, Consumer<Boolean> onComplete) {
    fileExecutor.submit(() -> {
      if (config == null) {
        System.err.println("[FileExecutor] Save skipped — config not yet loaded.");
        Platform.runLater(() -> onComplete.accept(false));
        return;
      }
      try {
        jsonWritter.write(processes, config.getMappingFile());
        Platform.runLater(() -> onComplete.accept(true));
      } catch (Exception e) {
        System.err.println("[FileExecutor] Save failed: " + e.getMessage());
        e.printStackTrace();
        Platform.runLater(() -> onComplete.accept(false));
      }
    });
  }

  private void scheduleCSVSnapshot(long intervalSEC) {
    System.out.println("[FileExecutor] Scheduling CSV snapshot every "
        + intervalSEC + "s");

    fileExecutor.scheduleWithFixedDelay(this::runCsvSnapshot, intervalSEC, intervalSEC,
        TimeUnit.SECONDS);
  }

  /**
   * Analytics Service submit-uje ovaj CSV Write job
   * 
   */
  public void submitFixedTimeSnapshot() {
    fileExecutor.submit(this::runCsvSnapshot);
  }

  /**
   * Pokupi sveze procese iz store-a i upisuje CSV snapshot
   */
  private void runCsvSnapshot() {
    try {
      List<ProcessItem> snapshot = dataService.getCurrentProcceses();
      csvWritter.write(snapshot);
    } catch (Exception e) {
      System.err.println("[FileExecutor] CSV snapshot failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setOnScanComplete(Runnable handler) {
    this.onScanComplete = handler;
  }

  public AppConfig getConfig() {
    return config;
  }

  public void shutdown() {
    scanExecutor.shutdown();
    fileExecutor.shutdown();
  }

}
