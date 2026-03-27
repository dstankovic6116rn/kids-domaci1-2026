package org.example;

import org.example.controller.MainController;
import org.example.services.DataService;
import org.example.view.MainView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private DataService dataService;
    private MainController mainController;

    @Override
    public void start(@SuppressWarnings("exports") Stage stage) {
        dataService = new DataService();

        MainView mainView = new MainView();
        new MainController(mainView, dataService);

        Scene scene = new Scene(mainView.getRoot(), 1000, 800);
        scene.getStylesheets()
                .add(getClass().getResource("/org/example/styles.css").toExternalForm());

        stage.setTitle("Productivity Buddy");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Gasi tredove Fork/join pool-a u JVM nakon gasenja prozora aplikacije
     * dataService - processScanService - forkJoinPool
     */
    @Override
    public void stop() {
        if (mainController != null) {
            mainController.getAnalyticsService().shutdown();
        }
        if (dataService != null) {
            dataService.shutdown();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}