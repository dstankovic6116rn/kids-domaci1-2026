package org.example.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.model.ProcessData;
import org.example.model.ProcessItem;
import org.example.model.ProcessRanking;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

/**
 * Centralni koordinator podataka izmedju
 * ProcessScanService, ProcessStore, Executor Service i
 * kontrolera, kao i izvodjenje podataka za PieChart
 */

public class DataService {
	private final ProcessScanService processScanService = new ProcessScanService();
	private final ProcessData processData = new ProcessData();
	private final ExecutorService executorService = new ExecutorService(this);

	public void start(Runnable onScanComplete) {
		executorService.setOnScanComplete(onScanComplete);
		executorService.start();
	}

	/**
	 * Skenira procese sa fork/join pool
	 * Merge-uje rezultat u ProcessData
	 * Vraca osvezenu listu procesa
	 * 
	 * Ne sme se pozvati iz FX Thread-a
	 */
	public void scanAndUpdate() {
		List<ProcessItem> scannedProcesses = processScanService.scan();

		processData.merge(scannedProcesses);
	}

	/**
	 * Vraca trenutne in-memory procese
	 */
	public List<ProcessItem> getCurrentProcceses() {
		return processData.getAll();
	}

	public void setProcessCategory(String name, String category) {
		ProcessItem pi = processData.getByName(name);
		if (pi != null) {
			pi.setCategory(category);
		}
	}

	public ObservableList<PieChart.Data> buildProcessCategoryPieData() {
		Map<String, Long> counts = processData.getAll().stream()
				.filter(p -> !p.getCategory().equals(ProcessItem.DEFAULT_CATEGORY))
				.collect(Collectors.groupingBy(
						ProcessItem::getCategory,
						Collectors.counting()));

		ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
		counts.forEach((category, count) -> slices.add(new PieChart.Data(category, count)));

		return slices;
	}

	public List<ProcessItem> getProcessesByCategoryName(String catName) {
		return processData.getAll().stream().filter(p -> p.getCategory().equals(catName)).collect(Collectors.toList());
	}

	public long getCategoryUptimeSeconds(String category) {
		return processData.getAll().stream()
				.filter(p -> p.getCategory().equals(category))
				.mapToLong(ProcessItem::getUptimeSeconds)
				.sum();
	}

	public ProcessRanking getRankingForProcess(String originalName) {
		List<ProcessItem> allProcessItems = processData.getAll();

		List<ProcessItem> ramProcessItems = allProcessItems.stream()
				.sorted((a, b) -> Double.compare(a.getRamUsageMb(), b.getRamUsageMb())).collect(Collectors.toList());
		List<ProcessItem> cpuProcessItems = allProcessItems.stream()
				.sorted((a, b) -> Double.compare(a.getCpuUsage(), b.getCpuUsage())).collect(Collectors.toList());

		int ramRank = 0;
		int cpuRank = 0;

		// TODO: explain this
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

	public void shutdown() {
		executorService.shutdown();
		processScanService.shutdown();
	}

}
