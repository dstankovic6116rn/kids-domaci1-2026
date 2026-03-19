package org.example.view;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainView {

  private final StackPane root;
  private final BorderPane mainPage;
  private final StackPane rightPane;

  private final ToolbarView toolbarView;
  private final ProcessListView processListView;
  private final PieView pieView;

  public MainView() {

    toolbarView = new ToolbarView();
    processListView = new ProcessListView();
    pieView = new PieView();
    rightPane = new StackPane(pieView.getRoot());

    SplitPane splitPane = new SplitPane(processListView.getRoot(), rightPane);
    splitPane.setOrientation(Orientation.HORIZONTAL);
    splitPane.setDividerPositions(0.5);

    mainPage = new BorderPane();
    mainPage.getStyleClass().add("root-pane");
    mainPage.setTop(toolbarView.getRoot());
    mainPage.setCenter(splitPane);

    root = new StackPane(mainPage);
    root.getStyleClass().add("root-pane");
  }

  public void showCategoryDetails(CategoryDetailsView categoryDetailsView) {
    root.getChildren().removeIf(node -> node instanceof CategoryDetailsView);
    root.getChildren().add(categoryDetailsView);

    mainPage.setVisible(false);
    mainPage.setManaged(false);

    categoryDetailsView.setVisible(true);
    categoryDetailsView.setManaged(true);
  }

  public void showMain() {
    root.getChildren().removeIf(node -> node instanceof CategoryDetailsView);
    mainPage.setVisible(true);
    mainPage.setManaged(true);
  }

  public void showProcessDetails(ProcessDetailsView processDetailsView) {
    rightPane.getChildren().removeIf(node -> node instanceof ProcessDetailsView);
    rightPane.getChildren().add(processDetailsView);

    pieView.getRoot().setVisible(false);
    pieView.getRoot().setManaged(false);

    processDetailsView.setVisible(true);
    processDetailsView.setManaged(true);
  }

  public void showPieView() {
    rightPane.getChildren().removeIf(node -> node instanceof ProcessDetailsView);

    pieView.getRoot().setVisible(true);
    pieView.getRoot().setManaged(true);
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

  public StackPane getRoot() {
    return root;
  }

}
