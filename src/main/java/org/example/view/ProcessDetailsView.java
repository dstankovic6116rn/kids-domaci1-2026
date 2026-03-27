package org.example.view;

import org.example.model.ProcessItem;
import org.example.model.ProcessRanking;
import org.example.utils.TimeFormatter;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProcessDetailsView extends VBox {

  private Runnable onKillProcess = () -> {
  };
  private Runnable onChangeProccessName = () -> {
  };
  private Runnable onFreezeTracking = () -> {
  };
  private Runnable onChangeProcessCategory = () -> {
  };

  private Runnable onBackRequested = () -> {
  };

  // Read by AnalyticsService to look up fresh data
  private final String processName;
  private final Label uptimeLabel = new Label();
  private final Label ramMetricLabel = new Label();
  private final Label ramRankLabel = new Label();
  private final Label cpuMetricLabel = new Label();
  private final Label cpuRankLabel = new Label();

  public ProcessDetailsView(ProcessItem processItem, ProcessRanking ranking) {
    getStyleClass().add("item-detail-root");
    setMaxWidth(Double.MAX_VALUE);
    setMaxHeight(Double.MAX_VALUE);

    Button backBtn = new Button("Back");
    backBtn.getStyleClass().add("back-btn");
    backBtn.setOnAction(e -> onBackRequested.run());

    HBox topBar = new HBox(backBtn);
    topBar.getStyleClass().add("process-detail-top-bar");
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 16, 10, 16));

    this.processName = processItem.getOriginalName();

    // Content
    Label nameLabel = new Label(processItem.getDisplayName());
    nameLabel.getStyleClass().add("process-detail-name");

    Label uptimeLabel = new Label(
        "Total time — " + TimeFormatter.formatTime(processItem.getUptimeSeconds()));

    Label ramMetric = new Label(
        String.format("RAM usage   %.1f MB", processItem.getRamUsageMb()));

    Label ramRank = new Label(
        ProcessRanking.toOrdinalRank(ranking.getRamRank()) + " on RAM usage");

    Label cpuMetric = new Label(
        String.format("CPU usage   %.1f%%", processItem.getCpuUsage()));

    Label cpuRank = new Label(
        ProcessRanking.toOrdinalRank(ranking.getCpuRank()) + " on CPU usage");

    GridPane metricsGrid = buildMetricsGrid();

    Button killBtn = new Button("Kill Process");
    Button changeNameBtn = new Button("Change Process Name");
    Button freezeTrackingBtn = new Button("Freeze Tracking");
    Button changeCategoryBtn = new Button("Change Process Category");

    // Action buttons
    for (Button btn : new Button[] { killBtn, changeNameBtn, freezeTrackingBtn, changeCategoryBtn }) {
      btn.getStyleClass().add("item-detail-btn");
      btn.setMaxWidth(Double.MAX_VALUE);
    }

    killBtn.setOnAction(e -> onKillProcess.run());
    changeNameBtn.setOnAction(e -> onChangeProccessName.run());
    freezeTrackingBtn.setOnAction(e -> onFreezeTracking.run());
    changeCategoryBtn.setOnAction(e -> onChangeProcessCategory.run());

    VBox buttonGroup = new VBox(10, killBtn, changeNameBtn, freezeTrackingBtn, changeCategoryBtn);
    buttonGroup.setAlignment(Pos.CENTER);
    buttonGroup.getStyleClass().add("process-detail-btn-group");

    VBox content = new VBox(12, nameLabel, uptimeLabel, metricsGrid, buttonGroup);
    content.setPadding(new Insets(24));

    VBox page = new VBox(topBar, content);
    page.setMaxWidth(Double.MAX_VALUE);
    page.setMaxHeight(Double.MAX_VALUE);

    getChildren().add(page);

  }

  private GridPane buildMetricsGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(0);
    grid.setVgap(0);

    ColumnConstraints metricCol = new ColumnConstraints();
    metricCol.setHgrow(Priority.ALWAYS);

    ColumnConstraints rankCol = new ColumnConstraints();
    rankCol.setHgrow(Priority.ALWAYS);

    grid.getColumnConstraints().addAll(metricCol, rankCol);

    grid.add(ramMetricLabel, 0, 0);
    grid.add(ramRankLabel, 1, 0);
    grid.add(cpuMetricLabel, 0, 1);
    grid.add(cpuRankLabel, 1, 1);

    return grid;
  }

  public void updateMetrics(ProcessItem processItem, ProcessRanking processRanking) {
    uptimeLabel.setText("Total time — " + TimeFormatter.formatTime(processItem.getUptimeSeconds()));
    ramMetricLabel.setText(
        String.format("RAM usage   %.1f MB", processItem.getRamUsageMb()));
    ramRankLabel.setText(
        ProcessRanking.toOrdinalRank(processRanking.getRamRank()) + " on RAM usage");
    cpuMetricLabel.setText(
        String.format("CPU usage   %.1f%%", processItem.getCpuUsage()));
    cpuRankLabel.setText(
        ProcessRanking.toOrdinalRank(processRanking.getCpuRank()) + " on CPU usage");
  }

  public String getProcessName() {
    return processName;
  }

  public void setOnKillProcess(Runnable handler) {
    this.onKillProcess = handler;
  }

  public void setOnChangeProccessName(Runnable handler) {
    this.onChangeProccessName = handler;
  }

  public void setOnFreezeTracking(Runnable handler) {
    this.onFreezeTracking = handler;
  }

  public void setOnChangeProcessCategory(Runnable handler) {
    this.onChangeProcessCategory = handler;
  }

  public void setOnBackRequested(Runnable handler) {
    this.onBackRequested = handler;
  }

}
