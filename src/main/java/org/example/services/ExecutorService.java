package org.example.services;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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
 * Upravlja dva executor-a sa jasno odvojenim odgovornostima
 * 
 * 1. Read config (scanExecutor)
 * 2. Load JSON history (fileExecutor)
 * 3. Zakazi periodicni CSV snapshot (fileExecutor)
 * 4. Zakazi periodicni scan (scanExecutor)
 * 
 * ScanExecutor - single thread:
 * 1. Zakazuje periodicna skeniranja sa fiksnim delay-om i merge-uje u store.
 * Koristi se fixed delay umesto fixed rate zbog slucaja da se zapoceti sken ne
 * zavrsi u roku od tri sekunde. U tom slucaju sledeci ceka a ne pokrece se
 * preko prethodnog.
 * 2. Kada stigne novi scan interval, cancel-uje prethodni posao i zakazuje novi
 * sa novim intervalom. Cancel-ovanje je sa false parametrom sto znaci da ne
 * prekida trenutno izvrsavanje skena, vec dozvoljava da se zavrsi pre nego sto
 * se zakaze novi.
 * 
 * FileExecutor (File I/O) - 2 thread pool:
 * 1. Na startu ucitava istorijske podatke iz fajla i puni
 * ProcessData.historicData koji se koristi za merge-ovanje sa novim skenovima
 * 2. Zakazuje periodicno cuvanje CSV snapshot-a svakih N sekundi
 * 3. Prima hitne zahteve za cuvanje CSV snapshot-a od strane AnalyticsService
 * 4. Prima zahteve za cuvanje JSON istorije od onSave akcije na FX Thread-u
 * 
 * Upravljanje kolizijama:
 * 1. ScanExecutor i FileExecutor su odvojeni da bi se izbeglo da I/O operacije
 * usporavaju skeniranje
 * 2. FileExecutor ima lock oko JSON upisa da bi se izbeglo da dva thread-a
 * istovremeno upisuju u isti fajl
 * 3. CSV snapshot fajlovi imaju executionTime timestamp u nazivu da bi se
 * izbeglo da dva thread-a upisuju u isti fajl, sto eliminiše potrebu za lock-om
 * 4. CSV Writter koristi CREATE_NEW + 1ms timestamp da bi se eliminisala
 * kolizija imena fajla u slucaju da se dva snapshot-a pokrenu u istoj
 * milisekundi, sto je malo verovatno ali nije nemoguce, a eliminiše potrebu za
 * lock-om
 *
 */

public class ExecutorService {

  /**
   * Scan Executor je single-threaded jer ima jednu funkciju — skeniranje procesa
   * i merge-ovanje u store, sto je posao koji nema potrebe da se mesa sa I/O
   * operacijama. Samim tim nikada nece kasniti skeniranje zbog I/O operacija, sto
   * je bitno za odrzavanje stabilnog intervala izmedju skenova.
   */
  private final ScheduledExecutorService scanExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "Scan Executor Thread");
    t.setDaemon(true);
    return t;
  });

  private final ScheduledExecutorService fileExecutor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "File Executor Thread-" + count.getAndIncrement());
      t.setDaemon(true);
      return t;
    }
  });

  /**
   * Stiti json fajl od istovremenog upisa iz dva thread-a
   */
  private final ReentrantLock reentrantLock = new ReentrantLock();

  /**
   * Zaustavlja WatcherService da reaguje na promene fajla koje je prouzrokovao
   * save action ili shutdown action, sto bi dovelo do nepotrebnih reload-ova i
   * merge-ovanja u store. Ovaj flag se postavlja na true pre nego sto
   * FileExecutor pokrene save job, a resetuje na false nakon sto se job zavrsi.
   * WatcherService proverava ovaj flag pre nego sto reaguje na promene fajla i
   * ignorise ih ako je flag true.
   */
  private final AtomicBoolean writeSuppressed = new AtomicBoolean(false);

  private final DataService dataService;
  private final ConfigReader configReader = new ConfigReader();
  private final JsonWritter jsonWritter = new JsonWritter();
  private final JsonReader jsonReader = new JsonReader();
  private final CsvWritter csvWritter = new CsvWritter();

  private ScheduledFuture<?> scanJob;

  // volatile — written on FX thread before start(), read on scan executor thread
  private volatile Runnable onScanComplete = () -> {
  };

  // Poziva se jednom nakon ucitavanja configa-a, DataService koristi ovaj handler
  // da startuje WatcherService
  private volatile Consumer<String> onConfigLoaded = path -> {
  };

  // Called on FX thread nakon sto se JSON save zavrsi
  private volatile Runnable onSnapshotComplete = () -> {
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

        onConfigLoaded.accept(config.getMappingFile());

        // Prepusti file executor-u da procita istoriju i zakaze CSV snapshot,
        // scanExecutor je slobodan nakon submit-a
        fileExecutor.submit(() -> {

          try {
            List<ProcessItem> historic = jsonReader.read(config.getMappingFile());
            dataService.loadHistory(historic);

            scheduleCSVSnapshot(config.getSnapshotIntervalSec());

            // vraca scanExecutor-u da zakaze periodicni scan nakon sto se istorija ucita i
            // merge-uje u store
            scanExecutor.submit(() -> {
              try {
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
   * Ceka da se zavrsi csv posao ako postoji
   * 
   * @param processes
   * @param onComplete
   */
  public void submitSave(List<ProcessItem> processes, Consumer<Boolean> onComplete) {
    fileExecutor.submit(() -> {
      AppConfig cfg = config;
      if (cfg == null) {
        System.err.println("[FileExecutor] Save skipped — config not loaded.");

        Platform.runLater(() -> onComplete.accept(false));
        return;
      }

      // Lock oko JSON upisa da bi se izbeglo da dva thread-a istovremeno upisuju u
      // isti fajl
      reentrantLock.lock();
      try {
        writeSuppressed.set(true);
        jsonWritter.write(processes, cfg.getMappingFile());
        Platform.runLater(() -> onComplete.accept(true));

      } catch (Exception e) {
        System.err.println("[FileExecutor] Save failed: " + e.getMessage());
        e.printStackTrace();
        Platform.runLater(() -> onComplete.accept(false));
      } finally {
        reentrantLock.unlock();
      }

    });
  }

  public void clearSuppression() {
    writeSuppressed.set(false);
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

      Platform.runLater(onSnapshotComplete);
    } catch (Exception e) {
      System.err.println("[FileExecutor] CSV snapshot failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setOnScanComplete(Runnable handler) {
    this.onScanComplete = handler;
  }

  public void setOnSnapshotComplete(Runnable handler) {
    this.onSnapshotComplete = handler;
  }

  /**
   * True while the app is writing process_info.json — WatcherService ignores
   * events.
   */
  public boolean isWriteSuppressed() {
    return writeSuppressed.get();
  }

  /**
   * Acquires the JSON read lock — WatcherService calls before reading the file.
   */
  public void acquireJsonReadLock() {
    reentrantLock.lock();
  }

  /** Releases the JSON read lock. */
  public void releaseJsonReadLock() {
    reentrantLock.unlock();
  }

  public void setOnConfigLoaded(Consumer<String> handler) {
    this.onConfigLoaded = handler;
  }

  public AppConfig getConfig() {
    return config;
  }

  public void shutdown() {
    scanExecutor.shutdown();
    fileExecutor.shutdown();
  }

}
