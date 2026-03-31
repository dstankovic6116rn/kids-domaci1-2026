package org.example.view;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Reusable dialog
 * Confirm/cancel za kill process i freeze time
 * Text input za promenu alias imena
 * Category picker za promenu kategorije procesa
 */
public class ProcessDialog extends StackPane {

  private Runnable onDismiss = () -> {
  };

  public ProcessDialog() {
    setAlignment(Pos.CENTER);
    setMaxWidth(Double.MAX_VALUE);
    setMaxHeight(Double.MAX_VALUE);
    // Ensure the backdrop doesn't stretch the dialog's preferred height
    setPickOnBounds(true);
    setOnMouseClicked(e -> onDismiss.run());
  }

  /**
   * Jednostavan confirmation dialog za KillProcess i FreezeTracking
   * 
   * @param title
   * @param message
   * @param confirmLabel
   * @param onConfirm
   */
  public static ProcessDialog confirmDialog(String title, String message, String confirmLabel, Runnable onConfirm) {
    ProcessDialog dialog = new ProcessDialog();

    Label messageLabel = new Label(message);
    messageLabel.getStyleClass().add("dialog-body-text");
    messageLabel.setWrapText(true);

    VBox body = new VBox(messageLabel);
    body.getStyleClass().add("dialog-body");
    body.setPadding(new Insets(16, 16, 8, 16));

    Button confirmBtn = new Button(confirmLabel);
    confirmBtn.getStyleClass().add("dialog-confirm-btn");
    confirmBtn.setOnAction(e -> {
      dialog.onDismiss.run();
      onConfirm.run();
    });

    dialog.buildCard(title, body, confirmBtn);
    return dialog;
  }

  /**
   * Text input dialog za promenu alias imena procesa
   * 
   * @param title
   * @param placeholder
   * @param confirmLabel
   * @param onConfirm
   */
  public static ProcessDialog textInputDialog(String title, String placeholder, String confirmLabel,
      Consumer<String> onConfirm) {

    ProcessDialog dialog = new ProcessDialog();

    TextField inputField = new TextField();
    inputField.getStyleClass().add("dialog-text-input");
    inputField.setPromptText(placeholder);

    Label errorLabel = new Label("Please enter a name.");
    errorLabel.getStyleClass().add("dialog-error-label");
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);

    VBox body = new VBox(8, inputField, errorLabel);
    body.getStyleClass().add("dialog-body");
    body.setPadding(new Insets(16, 16, 8, 16));

    Button confirmBtn = new Button(confirmLabel);
    confirmBtn.getStyleClass().add("dialog-confirm-btn");
    confirmBtn.setOnAction(e -> {
      String text = inputField.getText().trim();
      if (text.isEmpty()) {
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        return;
      }
      dialog.onDismiss.run();
      onConfirm.accept(text);
    });

    // Allow confirming with Enter key
    inputField.setOnAction(e -> confirmBtn.fire());

    dialog.buildCard(title, body, confirmBtn);

    return dialog;
  }

  /**
   * Category Picker Dialog za promenu kategorije procesa
   * 
   * @param title
   * @param categories
   * @param onConfirm
   */
  public static ProcessDialog categoryPickerDialog(String title, List<String> categories, Consumer<String> onConfirm) {
    ProcessDialog dialog = new ProcessDialog();

    ListView<String> listView = new ListView<>();
    listView.getStyleClass().add("dialog-category-list");
    listView.getItems().addAll(categories);
    listView.setPrefHeight(categories.size() * 32.0 + 2);

    Label errorLabel = new Label("Please select a category.");
    errorLabel.getStyleClass().add("dialog-error-label");
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);

    VBox body = new VBox(8, listView, errorLabel);
    body.getStyleClass().add("dialog-body");
    body.setPadding(new Insets(16, 16, 8, 16));

    Button confirmBtn = new Button("Apply");
    confirmBtn.getStyleClass().add("dialog-confirm-btn");
    confirmBtn.setOnAction(e -> {
      String selected = listView.getSelectionModel().getSelectedItem();
      if (selected == null) {
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        return;
      }
      dialog.onDismiss.run();
      onConfirm.accept(selected);
    });

    dialog.buildCard(title, body, confirmBtn);

    return dialog;
  }

  private void buildCard(String title, VBox body, Button confirmBtn) {

    // Header
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("dialog-title");

    Button closeBtn = new Button("✕");
    closeBtn.getStyleClass().add("dialog-close-btn");
    closeBtn.setOnAction(e -> onDismiss.run());

    Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);

    HBox header = new HBox(titleLabel, headerSpacer, closeBtn);
    header.getStyleClass().add("dialog-header");
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(14, 14, 14, 16));

    // Footer
    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("dialog-cancel-btn");
    cancelBtn.setOnAction(e -> onDismiss.run());

    Region footerSpacer = new Region();
    HBox.setHgrow(footerSpacer, Priority.ALWAYS);

    HBox footer = new HBox(footerSpacer, cancelBtn, confirmBtn);
    footer.getStyleClass().add("dialog-footer");
    footer.setAlignment(Pos.CENTER_RIGHT);
    footer.setSpacing(8);
    footer.setPadding(new Insets(12, 16, 14, 16));

    // Body
    VBox card = new VBox(header, body, footer);
    card.getStyleClass().add("dialog-card");
    card.setMaxWidth(380);
    card.setMaxHeight(BASELINE_OFFSET_SAME_AS_HEIGHT);
    StackPane.setAlignment(card, Pos.CENTER);
    card.setOnMouseClicked(e -> e.consume()); // prevent backdrop dismiss

    getChildren().add(card);
  }

  public void setOnDismiss(Runnable handler) {
    this.onDismiss = handler;
  }

}
