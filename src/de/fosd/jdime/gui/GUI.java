package de.fosd.jdime.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GUI extends Application {

	private static final String TITLE = "JDime";

	public TextArea output;
	public TextField left;
	public TextField base;
	public TextField right;
	public TextField jdime;
	public TextField cmdArgs;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource(getClass().getSimpleName() + ".fxml"));
		Scene scene = new Scene(root);

		primaryStage.setTitle(TITLE);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public void chooseLeft() {

	}

	public void chooseBase() {

	}

	public void chooseRight() {

	}

	public void chooseJDime() {

	}

	public void runClicked() {

	}
}
