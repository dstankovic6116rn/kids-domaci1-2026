package org.example.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.example.controller.PieController;
import org.example.model.AppConfig;
import org.example.model.ProcessItem;
import org.example.model.ProcessRanking;
import org.example.view.CategoryDetailsView;
import org.example.view.ProcessDetailsView;

import javafx.application.Platform;

/**
 * Periodicno vrsi agregaciju podataka iz processDataStore i osvezava prikaze
 * 
 * PieController se uvek osvezava zato sto je stalno aktivan
 * ProcessDetailsView se osvezava kada se taj panel otvori sa strane
 * CategoryDetailsView se osvezava kada se taj prozor otvori
 * 
 * AnalyticsService se okida u zadatom fiksnom intervalu ali kada se
 * ProcessScanService zavrsi posao on prekida PENDING posao AnalyticsService i
 * zakazuje da se izvrsi odmah zatim resetuje interval za standardnu analitiku
 * 
 * Active view reference su AtomicReference da bi bezbedno mogle da se rukuju iz
 * FX Thread-a bez zakljucavanja
 */

public class AnalyticsService {
  private static final long DEFAULT_INITIAL_DELAY_MS = 2000L;
  private static final long DEFAULT_FIXED_RUN_INTERVAL_MS = 1000L;

  private final DataService dataService;
  private final PieController pieController;
  private final Supplier<AppConfig> configSupplier; // reads config lazily after startup
  private final SnapshotSubmitter snapshotSubmitter;

  // Null kada view nije prikazan
  private final AtomicReference<ProcessDetailsView> activeProcessDetails = new AtomicReference<>(null);
  private final AtomicReference<CategoryDetailsView> activeCategoryDetails = new AtomicReference<>(null);

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "Analytics Thread");
    t.setDaemon(true);
    return t;
  });

  // Fixed-time snapshot tracking — reset each day
  // Stores "HH:mm:ss" strings of times already fired today
  private final Set<String> firedToday = new HashSet<>();
  private int lastDay = -1;

  public AnalyticsService(DataService dataService, PieController pieController, Supplier<AppConfig> configSupplier,
      SnapshotSubmitter snapshotSubmitter) {
    this.dataService = dataService;
    this.pieController = pieController;
    this.configSupplier = configSupplier;
    this.snapshotSubmitter = snapshotSubmitter;
  }

  /**
   * Pokrece periodicku analitiku u intervalu od 1s kako bi se poklopilo vreme sa
   * intervalom za snapshot.
   */
  public void start() {
    scheduler.scheduleWithFixedDelay(this::run,
        DEFAULT_INITIAL_DELAY_MS,
        DEFAULT_FIXED_RUN_INTERVAL_MS,
        TimeUnit.MILLISECONDS);
  }

  public void shutdown() {
    scheduler.shutdown();
  }

  private void run() {
    try {
      checkAndSubmitFixedSnapshot();
      updateViews();
    } catch (Exception e) {
      System.err.println("[AnalyticsService] failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void checkAndSubmitFixedSnapshot() {
    AppConfig config = configSupplier.get();

    if (config == null || config.getFixedSnapshotTimes().isEmpty())
      return;

    LocalTime now = LocalTime.now(ZoneId.systemDefault())
        .withNano(0); // truncate to second

    // Reset fired set at midnight
    int dayOfYear = LocalDate.now().getDayOfYear();
    if (dayOfYear != lastDay) { //
      firedToday.clear();
      lastDay = dayOfYear;
    }

    List<LocalTime> fixedTimes = config.getFixedSnapshotTimes();

    for (int i = 0; i < fixedTimes.size(); i++) {

      LocalTime truncated = fixedTimes.get(i).withNano(0);
      // Key by index — prevents same-second deduplication across distinct
      // configured times. Two entries at 21:08:55 each fire exactly once.
      String fireKey = i + "@" + now;

      if (truncated.equals(now) && !firedToday.contains(fireKey)) {
        firedToday.add(fireKey);

        System.out.println("[AnalyticsService] Fixed-time snapshot triggered at "
            + now + " (entry " + (i + 1) + ")");

        // Submit to file executor
        snapshotSubmitter.submitFixedTimeSnapshot();
      }
    }
  }

  private void updateViews() {
    final ProcessDetailsView processDetailsView = activeProcessDetails.get();
    final CategoryDetailsView categoryDetailsView = activeCategoryDetails.get();

    Platform.runLater(pieController::loadPieChartData);

    // Update Process Details View
    if (processDetailsView != null) {
      String processName = processDetailsView.getProcessName();
      if (processName != null) {
        ProcessItem processItem = dataService.getProcessByName(processName);

        if (processItem != null) {
          ProcessRanking ranking = dataService.getRankingForProcess(processName);

          Platform.runLater(() -> {
            long liveUptime = dataService.getLiveUptime(processName);
            processDetailsView.updateMetrics(processItem, ranking, liveUptime);
          });
        }
      }
    }

    // Update Category Details View
    if (categoryDetailsView != null) {
      String category = categoryDetailsView.getCategoryName();
      List<ProcessItem> processItems = dataService.getProcessesByCategoryName(category);

      Platform.runLater(() -> categoryDetailsView.updateData(processItems));
    }
  }

  public void setActiveProcessDetails(ProcessDetailsView view) {
    activeProcessDetails.set(view);
  }

  public void clearActiveProcessDetails() {
    activeProcessDetails.set(null);
  }

  public void setActiveCategoryDetail(CategoryDetailsView view) {
    activeCategoryDetails.set(view);
  }

  public void clearActiveCategoryDetail() {
    activeCategoryDetails.set(null);
  }
}
