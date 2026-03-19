package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CategoryDetailsView extends VBox {

  private Runnable onBackRequested = () -> {
  };

  public CategoryDetailsView(String categoryName) {
    Button backBtn = new Button("← Back");
    backBtn.getStyleClass().add("back-btn");
    backBtn.setOnAction(e -> onBackRequested.run());

    HBox topBar = new HBox(backBtn);
    topBar.getStyleClass().add("category-detail-top-bar");
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.setPadding(new Insets(10, 16, 10, 16));

    Label title = new Label(categoryName);
    title.getStyleClass().add("category-detail-title");

    Label placeholder = new Label("Details for \"" + categoryName + "\" will appear here.");
    placeholder.getStyleClass().add("category-detail-placeholder");

    VBox content = new VBox(16, title, placeholder);
    content.getStyleClass().add("category-detail-content");
    content.setPadding(new Insets(24));
    content.setAlignment(Pos.TOP_LEFT);

    getChildren().addAll(topBar, content);
    getStyleClass().add("category-detail-root");

    setMaxWidth(Double.MAX_VALUE);
    setMaxHeight(Double.MAX_VALUE);
  }

  public void setOnBackRequested(Runnable handler) {
    this.onBackRequested = handler;
  }

}
