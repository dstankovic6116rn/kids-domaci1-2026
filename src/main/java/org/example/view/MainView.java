package org.example.view;

import javafx.scene.layout.BorderPane;

public class MainView {

  private final BorderPane root;
  private final ToolbarView toolbarView;
  private final ProcessListView processListView;

  public MainView() {
    root = new BorderPane();
    root.getStyleClass().add("root-pane");

    toolbarView = new ToolbarView();
    root.setTop(toolbarView.getRoot());

    processListView = new ProcessListView();
    root.setCenter(processListView.getRoot());

  }

  public ToolbarView getToolbarView() {
    return toolbarView;
  }

  public ProcessListView getProcessListView() {
    return processListView;
  }

  public BorderPane getRoot() {
    return root;
  }

}
