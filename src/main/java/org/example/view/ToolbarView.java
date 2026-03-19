package org.example.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class ToolbarView {

  private final HBox root;

  private final Button saveButton;
  private final Button loadButton;
  private final Button shutdownButton;

  public ToolbarView() {
    saveButton = new Button("Save");
    saveButton.getStyleClass().add("toolbar-button");
    loadButton = new Button("Load");
    loadButton.getStyleClass().add("toolbar-button");
    shutdownButton = new Button("Shutdown");
    shutdownButton.getStyleClass().add("toolbar-button");

    root = new HBox(saveButton, loadButton, shutdownButton);
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

  public HBox getRoot() {
    return root;
  }

}