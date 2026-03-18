package org.example;

import org.example.controller.MainController;
import org.example.view.MainView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView();
        new MainController(mainView);

        Scene scene = new Scene(mainView.getRoot(), 1000, 800);
        scene.getStylesheets()
                .add(getClass().getResource("/org/example/styles.css").toExternalForm());

        stage.setTitle("Productivity Buddy");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}