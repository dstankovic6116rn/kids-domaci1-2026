package org.example.services;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.example.controller.PieController;
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
  private static final long DEFAULT_ANALYTICS_INTERVAL_MS = 2000L;
  private final DataService dataService;
  private final PieController pieController;

  // Null kada view nije prikazan
  private final AtomicReference<ProcessDetailsView> activeProcessDetails = new AtomicReference<>(null);
  private final AtomicReference<CategoryDetailsView> activeCategoryDetails = new AtomicReference<>(null);

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "Analytics Thread");
    t.setDaemon(true);
    return t;
  });

  private ScheduledFuture<?> pendingRuns;

  private final long analyticsIntervalMs;

  public AnalyticsService(DataService dataService, PieController pieController) {
    this.dataService = dataService;
    this.pieController = pieController;
    this.analyticsIntervalMs = DEFAULT_ANALYTICS_INTERVAL_MS;
  }

  public void start() {
    scheduleRun(analyticsIntervalMs);
  }

  private void scheduleRun(long delay) {
    pendingRuns = scheduler.scheduleWithFixedDelay(this::run, delay, analyticsIntervalMs, TimeUnit.MILLISECONDS);
  }

  /**
   * nudgeRun() poziva ExecutorService nakon sto se svaki scan procesa zavrsi
   * Otkazuje pending run i pokrece jedan odmah, zatim restartuje regularan
   * interval
   */
  public void nudgeRun() {
    if (pendingRuns != null) {
      pendingRuns.cancel(false);
    }
    scheduler.submit(this::run);
    scheduleRun(analyticsIntervalMs);
  }

  public void shutdown() {
    scheduler.shutdown();
  }

  private void run() {
    try {
      final ProcessDetailsView processDetailsView = activeProcessDetails.get();
      final CategoryDetailsView categoryDetailsView = activeCategoryDetails.get();

      Platform.runLater(pieController::loadPieChartData);

      // Update Process Details View
      if (processDetailsView != null) {
        String processName = getProcessName(processDetailsView);
        if (processName != null) {
          ProcessItem processItem = dataService.getCurrentProcceses().stream()
              .filter(p -> p.getOriginalName().equals(processName)).findFirst().orElse(null);
          if (processItem != null) {
            ProcessRanking ranking = dataService.getRankingForProcess(processName);

            Platform.runLater(() -> processDetailsView.updateMetrics(processItem, ranking));
          }
        }
      }

      // Update Category Details View
      if (categoryDetailsView != null) {
        String category = categoryDetailsView.getCategoryName();
        List<ProcessItem> processItems = dataService.getProcessesByCategoryName(category);

        Platform.runLater(() -> categoryDetailsView.updateData(processItems));
      }
    } catch (Exception e) {
      System.err.println("[AnalyticsService] failed: " + e.getMessage());
    }
  }

  private String getProcessName(ProcessDetailsView view) {
    return view.getProcessName();
  }

  public void setActiveItemDetail(ProcessDetailsView view) {
    activeProcessDetails.set(view);
  }

  public void clearActiveItemDetail() {
    activeProcessDetails.set(null);
  }

  public void setActiveCategoryDetail(CategoryDetailsView view) {
    activeCategoryDetails.set(view);
  }

  public void clearActiveCategoryDetail() {
    activeCategoryDetails.set(null);
  }
}
