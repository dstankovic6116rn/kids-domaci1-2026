package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Create the components
        Label label = new Label("\u0054\u0065\u0073\u0074"); // "Test" in Unicode
        Button button = new Button("Switch to Secondary View");

        // 2. Set up the Layout (Equivalent to your VBox in FXML)
        VBox root = new VBox(20); // 20.0 spacing
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(label, button);

        // 3. Handle the event (Equivalent to onAction="#switchToSecondary")
        button.setOnAction(event -> {
            System.out.println("Switching...");
            // Logic to change scenes goes here
        });

        // 4. Show the stage
        Scene scene = new Scene(root, 400, 300);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("JavaFX without FXML");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }

}