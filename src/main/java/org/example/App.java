package org.example;

import org.example.controller.MainController;
import org.example.services.DataService;
import org.example.view.MainView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

	private DataService dataService;
	private MainController mainController;
	private boolean shutdownInitiated = false;

	@Override
	public void start(@SuppressWarnings("exports") Stage stage) {
		dataService = new DataService();

		MainView mainView = new MainView();
		mainController = new MainController(mainView, dataService, this::initiateGreacefulShutdown);

		Scene scene = new Scene(mainView.getRoot(), 1000, 800);
		scene.getStylesheets()
				.add(getClass().getResource("/org/example/styles.css").toExternalForm());

		stage.setOnCloseRequest((event) -> {
			event.consume(); // Sprečava automatsko zatvaranje prozora
			initiateGreacefulShutdown(); // Inicira gasenje aplikacije
		});
		stage.setTitle("Productivity Buddy");
		stage.setScene(scene);
		stage.show();
	}

	public void initiateGreacefulShutdown() {
		if (!shutdownInitiated) {
			shutdownInitiated = true;

			dataService.saveProcesses(success -> {
				if (!success)
					System.err.println("[App] Save failed — shutting down anyway.");

				mainController.shutdown();
				Platform.exit();

			});
		}
	}

	/**
	 * Zbog provere !shutdownInitiated && mainController != null i
	 * initiateGreacefulShutdown() na onCloseRequest iz stage-a, ovaj stop() ce se
	 * okinuti samo prilikom ne ocegkivanog prekida rada JVM
	 */
	@Override
	public void stop() {
		if (!shutdownInitiated && mainController != null) {
			mainController.shutdown();
		}
	}

	public static void main(String[] args) {
		launch();
	}

}