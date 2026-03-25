package org.example.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.model.ProcessData;
import org.example.model.ProcessItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

/**
 * Centralni koordinator podataka izmedju ProcessScanService, ProcessStore i
 * kontrolera, kao i izvodjenje podataka za PieChart
 */

public class DataService {
	private final ProcessScanService processScanService = new ProcessScanService();
	private final ProcessData processData = new ProcessData();

	/**
	 * Skenira procese sa fork/join pool
	 * Merge-uje rezultat u ProcessData
	 * Vraca osvezenu listu procesa
	 * 
	 * Ne sme se pozvati iz FX Thread-a
	 */
	public List<ProcessItem> scanAndUpdate() {
		List<ProcessItem> scannedProcesses = processScanService.scan();
		processData.merge(scannedProcesses);
		return processData.getAll();
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
		System.out.println("buildProcessCategoryPieData" + slices);
		return slices;
	}

	public void shutdown() {
		processScanService.shutdown();
	}

	public List<ProcessItem> getProcessesByCategoryName(String catName) {
		return processData.getAll().stream().filter(p -> p.getCategory().equals(catName)).collect(Collectors.toList());
	}

}
