package org.example.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class ToolbarView {

  private final HBox root;

  private final Button saveButton;
  private final Button loadButton;
  private final Button shutdownButton;

  // Prikazuje feedback save akcije
  private final Label statusLabel = new Label();

  public ToolbarView() {
    saveButton = new Button("Save");
    saveButton.getStyleClass().add("toolbar-button");
    loadButton = new Button("Load");
    loadButton.getStyleClass().add("toolbar-button");
    shutdownButton = new Button("Shutdown");
    shutdownButton.getStyleClass().add("toolbar-button");

    statusLabel.getStyleClass().add("toolbar-status-label");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    root = new HBox(saveButton, loadButton, shutdownButton, statusLabel);
    root.getStyleClass().add("toolbar");
    root.setAlignment(Pos.CENTER_LEFT);

  }

  public void setOnSave(Runnable action) {
    saveButton.setOnAction(e -> action.run());
  }

  public void setOnLoad(Runnable action) {
    loadButton.setOnAction(e -> action.run());
  }

  public void setOnShutdown(Runnable action) {
    shutdownButton.setOnAction(e -> action.run());
  }

  public void showSaveStatus(boolean success) {
    statusLabel.setText(success ? "Saved" : "Save failed");
    statusLabel.getStyleClass().removeAll("status-success", "status-error");
    statusLabel.getStyleClass().add(success ? "status-success" : "status-error");

    // PauseTransition runs entirely on the FX thread — no background thread needed.
    PauseTransition pause = new PauseTransition(Duration.seconds(3));
    pause.setOnFinished(e -> statusLabel.setText(""));
    pause.play();
  }

  public void showSnapshotStatus() {
    statusLabel.setText("Snapshot saved");
    statusLabel.getStyleClass().removeAll("status-success", "status-error");
    statusLabel.getStyleClass().add("status-success");

    // PauseTransition runs entirely on the FX thread — no background thread needed.
    PauseTransition pause = new PauseTransition(Duration.seconds(3));
    pause.setOnFinished(e -> statusLabel.setText(""));
    pause.play();
  }

  public void showSavingStatus() {
    statusLabel.setText("Saving...");
    statusLabel.getStyleClass().removeAll("status-success", "status-error");
  }

  /** Disable sve button-e dok traje shutdown sekvenca */
  public void setDisabled(boolean disabled) {
    saveButton.setDisable(disabled);
    loadButton.setDisable(disabled);
    shutdownButton.setDisable(disabled);
  }

  public HBox getRoot() {
    return root;
  }

}