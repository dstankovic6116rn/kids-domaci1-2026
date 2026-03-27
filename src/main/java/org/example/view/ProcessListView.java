package org.example.view;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

/**
 * setItems() rebuild-uje listu samo ako je lista procesa promenjena od
 * poslednjeg build-a
 * Ako nista nije promenjeno DOM se ne azurira
 */

public class ProcessListView {

  private final ScrollPane root;
  private final VBox processListContainer;

  private Consumer<ProcessItem> onLabelClicked = item -> {
  };
  private BiConsumer<ProcessItem, String> onCategoryChanged = (item, newCategory) -> {
  };

  private ContextMenu activeMenu = null;

  // Deferred update — held while a menu is open, applied on menu close
  private ObservableList<ProcessItem> pendingProcesses = null;

  // Names from the last successful rebuild — used for change detection
  private List<String> lastBuiltNames = List.of();

  public ProcessListView() {
    processListContainer = new VBox(4);
    processListContainer.setPadding(new Insets(8));

    root = new ScrollPane(processListContainer);
    root.setFitToWidth(true);
  }

  /**
   * Ako je meni otvoren, update liste se odlaze dok se ne zatvori da ne bi doslo
   * do prekida
   * 
   * pendingProcesses cuva ObservableList referencu iz ProcessListController-a
   * ProcessListController drzi istu referencu i poziva setAll() nad njom nakon
   * svakog skeniranja tako da je pendingProcesses lista uvek azurna ukljucujuci
   * promene kategorija dok je meni otvoren jer se te promene upisuju direktno u
   * ProcessItem objekat u processDataStore
   * 
   */

  public void setItems(ObservableList<ProcessItem> processes) {
    if (activeMenu != null && activeMenu.isShowing()) {
      pendingProcesses = processes;
      return;
    }
    rebuildIfChanged(processes);
  }

  private void rebuildIfChanged(ObservableList<ProcessItem> processes) {
    pendingProcesses = null;

    List<String> incomingNames = processes.stream()
        .map(
            ProcessItem::getOriginalName)
        .collect(Collectors.toList());

    if (incomingNames.equals(lastBuiltNames)) {
      return; // nothing changed — leave the list untouched
    }

    lastBuiltNames = incomingNames;
    processListContainer.getChildren().clear();
    for (ProcessItem p : processes) {
      processListContainer.getChildren().add(createProcessItem(p));
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
      // Close the previously open menu before opening this one
      if (activeMenu != null && activeMenu.isShowing()) {
        activeMenu.hide();
      }
      activeMenu = categoryMenu;
      categoryMenu.show(processCategoryLabel, e.getScreenX(), e.getScreenY());
    });

    categoryMenu.setOnHidden(e -> {
      if (activeMenu == categoryMenu) {
        activeMenu = null;
      }
      if (pendingProcesses != null) {
        rebuildIfChanged(pendingProcesses);
      }
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
