package org.example.view;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.example.model.ProcessItem;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ProcessListView {

  private final ScrollPane root;
  private final VBox processListContainer;

  private Consumer<ProcessItem> onLabelClicked = item -> {
  };
  private BiConsumer<ProcessItem, String> onCategoryChanged = (item, newCategory) -> {
  };

  public ProcessListView() {
    processListContainer = new VBox(4);
    processListContainer.setPadding(new Insets(8));

    root = new ScrollPane(processListContainer);
    root.setFitToWidth(true);
  }

  public void setProcessItems(ObservableList<ProcessItem> items) {
    processListContainer.getChildren().clear();
    for (ProcessItem item : items) {
      processListContainer.getChildren().add(createProcessItem(item));
    }
  }

  private HBox createProcessItem(ProcessItem item) {

    Label processNameLabel = new Label(item.getDisplayName());
    processNameLabel.getStyleClass().addAll("item-label-left", "item-label-clickable");
    processNameLabel.setOnMouseEntered(e -> processNameLabel.getStyleClass().add("process-item-hover"));
    processNameLabel.setOnMouseExited(e -> processNameLabel.getStyleClass().remove("process-item-hover"));

    processNameLabel.setOnMouseClicked(e -> {
      e.consume(); // Prevent event from propagating to process name label
      onLabelClicked.accept(item);
    });

    Label processCategoryLabel = new Label(item.getCategory());
    processCategoryLabel.getStyleClass().addAll("item-label-right", "item-label-clickable");
    processCategoryLabel.setOnMouseEntered(e -> processCategoryLabel.getStyleClass().add("process-item-hover"));
    processCategoryLabel.setOnMouseExited(e -> processCategoryLabel.getStyleClass().remove("process-item-hover"));

    ContextMenu categoryMenu = buildCategoriesMenu(item, processCategoryLabel);
    processCategoryLabel.setOnMouseClicked(e -> {
      e.consume();
      categoryMenu.show(processCategoryLabel, e.getScreenX(), e.getScreenY());
    });

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox row = new HBox(processNameLabel, spacer, processCategoryLabel);
    row.getStyleClass().add("pair-row");
    row.setPadding(new Insets(8, 12, 8, 12));
    row.setAlignment(Pos.CENTER_LEFT);

    return row;
  }

  private ContextMenu buildCategoriesMenu(ProcessItem item, Label categoryLabel) {
    ContextMenu menu = new ContextMenu();

    for (String option : ProcessItem.CATEGORY_OPTIONS) {
      MenuItem menuItem = new MenuItem(option);
      menuItem.setOnAction(e -> {
        item.setCategory(option); // update model
        categoryLabel.setText(option); // refresh label immediately
        onCategoryChanged.accept(item, option);
      });
      menu.getItems().add(menuItem);
    }

    return menu;
  }

  public void setOnLabelClicked(Consumer<ProcessItem> handler) {
    this.onLabelClicked = handler;
  }

  public void setOnCategoryChanged(BiConsumer<ProcessItem, String> handler) {
    this.onCategoryChanged = handler;
  }

  public ScrollPane getRoot() {
    return root;
  }

}
