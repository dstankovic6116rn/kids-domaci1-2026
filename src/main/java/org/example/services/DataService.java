package org.example.services;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.example.model.ProcessData;
import org.example.model.ProcessItem;
import org.example.model.ProcessRanking;
import org.example.workers.JsonReader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

/**
 * Centralni koordinator podataka izmedju
 * ProcessScanService, ProcessStore, Executor Service, AnalyticsService,
 * WatcherService i
 * kontrolera, kao i izvodjenje podataka za PieChart
 */

public class DataService {
	private final ProcessScanService processScanService = new ProcessScanService();
	private final ProcessData processData = new ProcessData();
	private final ExecutorService executorService = new ExecutorService(this);
	private final JsonReader jsonReader = new JsonReader();
	private volatile WatcherService watcherService;

	public void start(Runnable onScanComplete, Runnable onSnapshotComplete) {
		executorService.setOnScanComplete(onScanComplete);
		executorService.setOnConfigLoaded(this::startWatcherService);
		executorService.setOnSnapshotComplete(onSnapshotComplete);

		executorService.start();
	}

	// mapping file dolazi iz onConfigLoaded iz ExecutorService nakon ucitavanja
	// config-a accept metodom
	private void startWatcherService(String mappingFile) {
		watcherService = new WatcherService(processData, jsonReader, executorService::isWriteSuppressed,
				executorService::acquireJsonReadLock, executorService::releaseJsonReadLock, mappingFile,
				executorService::clearSuppression);
		watcherService.start();
	}

	/**
	 * Skenira procese sa fork/join pool
	 * Merge-uje rezultat u ProcessData
	 * Vraca osvezenu listu procesa
	 * 
	 * FileExecutor poziva
	 */
	public void scanAndUpdate() {
		List<ProcessItem> scannedProcesses = processScanService.scan();

		processData.merge(scannedProcesses);
	}

	/**
	 * FileExecutor poziva loadHistory pre prvog skeniranja procesa
	 * 
	 * @param records
	 */
	public void loadHistory(List<ProcessItem> records) {
		processData.loadFromHistory(records);
	}

	/**
	 * Vraca trenutne in-memory procese
	 */
	public List<ProcessItem> getCurrentProcceses() {
		return processData.getAll();
	}

	public ProcessItem getProcessByName(String name) {
		return processData.getByName(name);
	}

	public void setProcessCategory(String name, String category) {
		ProcessItem pi = processData.getByName(name);
		if (pi != null) {
			pi.setCategory(category);
		}
	}

	/**
	 * Odmah ukloni proces iz store-a nakon kill signala, bez cekanja na sledeci
	 * scan.
	 */
	public void removeProcess(String originalName) {
		processData.remove(originalName);
	}

	// Freeze kontrole
	public void freezeUptime(String originalName) {
		processData.freezeUptime(originalName);
	}

	public void unfreezeUptime(String originalName) {
		processData.unfreezeUptime(originalName);
	}

	public boolean isFrozen(String originalName) {
		return processData.isFrozen(originalName);
	}

	public long getLiveUptime(String originalName) {
		return processData.getLiveUptime(originalName);
	}

	public ObservableList<PieChart.Data> buildProcessCategoryPieData() {
		Map<String, Long> uptimeByCategory = processData.getAll().stream()
				.filter(p -> !p.getCategory().equals(ProcessItem.DEFAULT_CATEGORY))
				.collect(Collectors.groupingBy(
						ProcessItem::getCategory,
						Collectors.summingLong(ProcessItem::getUptimeSeconds)));

		ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
		uptimeByCategory.forEach((category, uptime) -> slices.add(new PieChart.Data(category, uptime)));

		return slices;
	}

	public List<ProcessItem> getProcessesByCategoryName(String catName) {
		return processData.getAll().stream().filter(p -> p.getCategory().equals(catName)).collect(Collectors.toList());
	}

	public ProcessRanking getRankingForProcess(String originalName) {
		List<ProcessItem> allProcessItems = processData.getAll();

		List<ProcessItem> ramProcessItems = allProcessItems.stream()
				.sorted((a, b) -> Double.compare(b.getRamUsageMb(), a.getRamUsageMb())).collect(Collectors.toList());
		List<ProcessItem> cpuProcessItems = allProcessItems.stream()
				.sorted((a, b) -> Double.compare(b.getCpuUsage(), a.getCpuUsage())).collect(Collectors.toList());

		int ramRank = 0;
		int cpuRank = 0;

		/**
		 * index 0 → IntelliJ 1500 MB → rank 1 (highest)
		 * index 1 → Chrome 800 MB → rank 2
		 * index 2 → Discord 600 MB → rank 3
		 * index 3 → Spotify 300 MB → rank 4 (lowest)
		 */
		for (int i = 0; i < ramProcessItems.size(); i++) {
			if (ramProcessItems.get(i).getOriginalName().equals(originalName)) {
				ramRank = i + 1;
				break;
			}
		}

		for (int i = 0; i < cpuProcessItems.size(); i++) {
			if (cpuProcessItems.get(i).getOriginalName().equals(originalName)) {
				cpuRank = i + 1;
				break;
			}
		}

		return new ProcessRanking(ramRank, cpuRank);

	}

	/**
	 * Submit-uje Save job FileExecutor-u
	 * 
	 * @param onComplete
	 */
	public void saveProcesses(Consumer<Boolean> onComplete) {
		List<ProcessItem> snapshot = processData.getAllWithHistory();
		executorService.submitSave(snapshot, onComplete);
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void shutdown() {
		if (watcherService != null) {
			watcherService.shutdown();
		}
		executorService.shutdown();
		processScanService.shutdown();
	}

}
