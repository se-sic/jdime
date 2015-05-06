package de.fosd.jdime.gui;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUI extends Application {

	private static final String TITLE = "JDime";

	public TextArea output;
	public TextField left;
	public TextField base;
	public TextField right;
	public TextField jDime;
	public TextField cmdArgs;
	public Button leftBtn;
	public Button baseBtn;
	public Button rightBtn;
	public Button runBtn;
	public Button jDimeBtn;

	private List<TextField> textFields;
	private List<Button> buttons;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(getClass().getSimpleName() + ".fxml"));
		loader.setController(this);

		Parent root = loader.load();
		Scene scene = new Scene(root);

		textFields = Arrays.asList(left, base, right, jDime, cmdArgs);
		buttons = Arrays.asList(leftBtn, baseBtn, rightBtn, runBtn, jDimeBtn);

		primaryStage.setTitle(TITLE);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public void chooseLeft(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		File leftArtifact = chooser.showOpenDialog(window);
		if (leftArtifact != null) {
			left.setText(leftArtifact.getAbsolutePath());
		}
	}

	public void chooseBase(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		File baseArtifact = chooser.showOpenDialog(window);
		if (baseArtifact != null) {
			base.setText(baseArtifact.getAbsolutePath());
		}
	}

	public void chooseRight(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		File rightArtifact = chooser.showOpenDialog(window);
		if (rightArtifact != null) {
			right.setText(rightArtifact.getAbsolutePath());
		}
	}

	public void chooseJDime(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		File jDimeBinary = chooser.showOpenDialog(window);
		if (jDimeBinary != null) {
			jDime.setText(jDimeBinary.getAbsolutePath());
		}
	}

	public void runClicked() {
		textFields.forEach(textField -> textField.setDisable(true));
		buttons.forEach(button -> button.setDisable(true));

		Task<String> jDimeExec = new Task<String>() {

			@Override
			protected String call() throws Exception {
				ProcessBuilder builder = new ProcessBuilder();
				List<String> command = new ArrayList<>();

				command.add(jDime.getText());
				command.addAll(Arrays.asList(cmdArgs.getText().split("\\s+")));
				command.add(left.getText());
				command.add(base.getText());
				command.add(right.getText());
				builder.command(command);

				Process process = builder.start();
				process.waitFor();

				StringWriter writer = new StringWriter();
				IOUtils.copy(process.getInputStream(), writer, Charset.defaultCharset());

				return writer.toString();
			}
		};

		jDimeExec.setOnSucceeded(event -> {
			output.setText(jDimeExec.getValue());
			textFields.forEach(textField -> textField.setDisable(false));
			buttons.forEach(button -> button.setDisable(false));
		});

		new Thread(jDimeExec).start();
	}
}
