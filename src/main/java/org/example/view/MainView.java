package org.example.view;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class MainView {

  private final BorderPane root;
  private final ToolbarView toolbarView;
  private final ProcessListView processListView;
  private final PieView pieView;

  public MainView() {
    root = new BorderPane();
    root.getStyleClass().add("root-pane");

    toolbarView = new ToolbarView();
    root.setTop(toolbarView.getRoot());

    processListView = new ProcessListView();
    pieView = new PieView();

    SplitPane splitPane = new SplitPane(processListView.getRoot(), pieView.getRoot());
    splitPane.setOrientation(Orientation.HORIZONTAL);
    splitPane.setDividerPositions(0.5);
    root.setCenter(splitPane);

  }

  public ToolbarView getToolbarView() {
    return toolbarView;
  }

  public ProcessListView getProcessListView() {
    return processListView;
  }

  public PieView getPieView() {
    return pieView;
  }

  public BorderPane getRoot() {
    return root;
  }

}
