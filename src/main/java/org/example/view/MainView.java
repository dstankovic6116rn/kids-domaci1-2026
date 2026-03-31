package org.example.view;

import org.example.services.AnalyticsService;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainView {

  private final BorderPane root;
  private final StackPane mainPage;
  private final StackPane rightPane;
  private final SplitPane splitPane;

  private final ToolbarView toolbarView;
  private final ProcessListView processListView;
  private final PieView pieView;

  private AnalyticsService analyticsService;

  public MainView() {

    toolbarView = new ToolbarView();
    processListView = new ProcessListView();
    pieView = new PieView();

    rightPane = new StackPane(pieView.getRoot());

    splitPane = new SplitPane(processListView.getRoot(), rightPane);
    splitPane.setOrientation(Orientation.HORIZONTAL);
    splitPane.setDividerPositions(0.5);

    // mainPage holds the main split view and swaps in CategoryDetailsView
    mainPage = new StackPane(splitPane);

    root = new BorderPane();
    root.getStyleClass().add("root-pane");
    root.setTop(toolbarView.getRoot());
    root.setCenter(mainPage);
  }

  public void setAnalyticsService(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  public void showCategoryDetails(CategoryDetailsView categoryDetailsView) {
    mainPage.getChildren().removeIf(node -> node instanceof CategoryDetailsView);
    mainPage.getChildren().add(categoryDetailsView);

    splitPane.setVisible(false);
    splitPane.setManaged(false);

    if (analyticsService != null) {
      analyticsService.setActiveCategoryDetail(categoryDetailsView);
    }
  }

  public void showMain() {
    mainPage.getChildren().removeIf(node -> node instanceof CategoryDetailsView);
    splitPane.setVisible(true);
    splitPane.setManaged(true);

    if (analyticsService != null) {
      analyticsService.clearActiveCategoryDetail();
    }
  }

  public void showProcessDetails(ProcessDetailsView processDetailsView) {
    rightPane.getChildren().removeIf(node -> node instanceof ProcessDetailsView);
    rightPane.getChildren().add(processDetailsView);

    pieView.getRoot().setVisible(false);
    pieView.getRoot().setManaged(false);

    processDetailsView.setVisible(true);
    processDetailsView.setManaged(true);

    if (analyticsService != null) {
      analyticsService.setActiveProcessDetails(processDetailsView);
    }
  }

  public void showPieView() {
    rightPane.getChildren().removeIf(node -> node instanceof ProcessDetailsView);

    pieView.getRoot().setVisible(true);
    pieView.getRoot().setManaged(true);

    if (analyticsService != null) {
      analyticsService.clearActiveProcessDetails();
    }
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
