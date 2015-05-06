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

	private File lastChooseDir;
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

	private File getChosenFile(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		if (lastChooseDir != null && lastChooseDir.isDirectory()) {
			chooser.setInitialDirectory(lastChooseDir);
		}

		return chooser.showOpenDialog(window);
	}

	public void chooseLeft(ActionEvent event) {
		File leftArtifact = getChosenFile(event);

		if (leftArtifact != null) {
			lastChooseDir = leftArtifact.getParentFile();
			left.setText(leftArtifact.getAbsolutePath());
		}
	}

	public void chooseBase(ActionEvent event) {
		File baseArtifact = getChosenFile(event);

		if (baseArtifact != null) {
			lastChooseDir = baseArtifact.getParentFile();
			base.setText(baseArtifact.getAbsolutePath());
		}
	}

	public void chooseRight(ActionEvent event) {
		File rightArtifact = getChosenFile(event);

		if (rightArtifact != null) {
			lastChooseDir = rightArtifact.getParentFile();
			right.setText(rightArtifact.getAbsolutePath());
		}
	}

	public void chooseJDime(ActionEvent event) {
		File jDimeBinary = getChosenFile(event);

		if (jDimeBinary != null) {
			lastChooseDir = jDimeBinary.getParentFile();
			jDime.setText(jDimeBinary.getAbsolutePath());
		}
	}

	public void runClicked() {

		if (textFields.stream().anyMatch(tf -> !new File(tf.getText()).exists())) {
			return;
		}

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
