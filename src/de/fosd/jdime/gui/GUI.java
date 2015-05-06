package de.fosd.jdime.gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

public class GUI extends Application {

	private static final String TITLE = "JDime";

	public TextArea output;
	public TextField left;
	public TextField base;
	public TextField right;
	public TextField jdime;
	public TextField cmdArgs;

	private File leftArtifact;
	private File baseArtifact;
	private File rightArtifact;
	private File jDimeBinary;

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

	public void chooseLeft(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		leftArtifact = chooser.showOpenDialog(window);
		left.setText(leftArtifact.getAbsolutePath());
	}

	public void chooseBase(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		baseArtifact = chooser.showOpenDialog(window);
		base.setText(baseArtifact.getAbsolutePath());
	}

	public void chooseRight(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		rightArtifact = chooser.showOpenDialog(window);
		right.setText(rightArtifact.getAbsolutePath());
	}

	public void chooseJDime(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		jDimeBinary = chooser.showOpenDialog(window);
		jdime.setText(jDimeBinary.getAbsolutePath());
	}

	public void runClicked() {

	}
}
