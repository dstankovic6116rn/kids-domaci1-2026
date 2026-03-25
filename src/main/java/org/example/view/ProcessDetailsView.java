package org.example.view;

import org.example.model.ProcessItem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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

  public ProcessDetailsView(ProcessItem processItem) {
    getStyleClass().add("item-detail-root");
    setMaxWidth(Double.MAX_VALUE);
    setMaxHeight(Double.MAX_VALUE);

    Button backBtn = new Button("← Back");
    backBtn.getStyleClass().add("back-btn");
    backBtn.setOnAction(e -> onBackRequested.run());

    HBox topBar = new HBox(backBtn);
    topBar.getStyleClass().add("process-detail-top-bar");
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 16, 10, 16));

    // ── Content ──────────────────────────────────────────────────
    Label nameLabel = new Label(processItem.getDisplayName());
    nameLabel.getStyleClass().add("process-detail-name");

    Label placeholderText = new Label(
        "Additional information about \"" + processItem.getDisplayName() + "\" will appear here.");
    placeholderText.getStyleClass().add("process-detail-placeholder");
    placeholderText.setWrapText(true);

    Button killBtn = new Button("Kill Process");
    Button changeNameBtn = new Button("Change Process Name");
    Button freezeTrackingBtn = new Button("Freeze Tracking");
    Button changeCategoryBtn = new Button("Change Process Category");

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

    VBox content = new VBox(16, nameLabel, placeholderText, buttonGroup);
    content.getStyleClass().add("process-detail-content");
    content.setPadding(new Insets(24));

    getChildren().addAll(topBar, content);

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
