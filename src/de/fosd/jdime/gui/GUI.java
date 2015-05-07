package de.fosd.jdime.gui;

import javafx.application.Application;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * A simple JavaFX GUI for JDime.
 */
public final class GUI extends Application {

	private static final String TITLE = "JDime";

	private static final String JDIME_CONF_FILE = "JDime.properties";
	private static final String JDIME_DEFAULT_ARGS_KEY = "DEFAULT_ARGS";
	private static final String JDIME_DEFAULT_LEFT_KEY = "DEFAULT_LEFT";
	private static final String JDIME_DEFAULT_BASE_KEY = "DEFAULT_BASE";
	private static final String JDIME_DEFAULT_RIGHT_KEY = "DEFAULT_RIGHT";
	private static final String JDIME_EXEC_KEY = "JDIME_EXEC";

	private static final String JVM_DEBUG_PARAMS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005";
	private static final String STARTSCRIPT_JVM_ENV_VAR = "JAVA_OPTS";

	@FXML
	TextArea output;
	@FXML
	TextField left;
	@FXML
	TextField base;
	@FXML
	TextField right;
	@FXML
	TextField jDime;
	@FXML
	TextField cmdArgs;
	@FXML
	private Button leftBtn;
	@FXML
	private Button baseBtn;
	@FXML
	private Button rightBtn;
	@FXML
	private Button runBtn;
	@FXML
	private Button jDimeBtn;
	@FXML
	private Button historyPrevious;
	@FXML
	private Button historyNext;
	@FXML
	private CheckBox debugMode;

	private Properties config;

	private File lastChooseDir;
	private List<TextField> textFields;
	private List<Button> buttons;

	private IntegerProperty historyIndex;
	private ObservableList<State> history;
	private SimpleListProperty<State> historyListProp;
	private State inProgress;

	/**
	 * Launches the GUI with the given <code>args</code>.
	 *
	 * @param args
	 * 		the command line arguments
	 */
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
		historyIndex = new SimpleIntegerProperty(1);
		history = FXCollections.observableArrayList();
		historyListProp = new SimpleListProperty<>(history);
		config = new Properties();

		historyListProp.sizeProperty().addListener((observable, oldValue, newValue) -> historyIndex.setValue(newValue.intValue()));

		BooleanBinding noPrev = historyListProp.sizeProperty().greaterThan(0).and(historyIndex.greaterThan(0)).not();
		BooleanBinding noNext = historyListProp.sizeProperty().greaterThan(0).and(historyIndex.greaterThanOrEqualTo(historyListProp.sizeProperty())).not();
		historyNext.disableProperty().bind(noNext);
		historyPrevious.disableProperty().bind(noPrev);

		historyIndex.addListener((observable, oldValue, newValue) -> System.out.println(newValue));

		loadConfigFile();
		loadDefaults();

		primaryStage.setTitle(TITLE);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Loads default values for the <code>TextField</code>s from the config file.
	 */
	private void loadDefaults() {
		String jDimeExec = getConfig(JDIME_EXEC_KEY);
		if (jDimeExec != null) {
			jDime.setText(jDimeExec.trim());
		}

		String defaultArgs = getConfig(JDIME_DEFAULT_ARGS_KEY);
		if (defaultArgs != null) {
			cmdArgs.setText(defaultArgs.trim());
		}

		String left = getConfig(JDIME_DEFAULT_LEFT_KEY);
		if (left != null) {
			this.left.setText(left);
		}

		String base = getConfig(JDIME_DEFAULT_BASE_KEY);
		if (base != null) {
			this.base.setText(left);
		}

		String right = getConfig(JDIME_DEFAULT_RIGHT_KEY);
		if (right != null) {
			this.right.setText(left);
		}
	}

	/**
	 * Checks whether the current working directory contains a file called {@value #JDIME_CONF_FILE} and if so loads
	 * the mappings contained in it into the <code>Properties</code> instance <code>config</code> which is used
	 * by {@link #getConfig(String)}.
	 */
	private void loadConfigFile() {
		File configFile = new File(JDIME_CONF_FILE);
		if (configFile.exists()) {
			Charset cs = StandardCharsets.UTF_8;

			try {
				config.load(new InputStreamReader(new BufferedInputStream(new FileInputStream(configFile)), cs));
			} catch (IOException e) {
				System.err.println("Could not load " + configFile);
				System.err.println(e.getMessage());
			}
		}
	}

	/**
	 * Checks whether the file {@value #JDIME_CONF_FILE} in the current directory contains a mapping for the given key
	 * and if so returns the mapped value. If the file contains no mapping the system environment variables are checked.
	 * If no environment variable named <code>key</code> exists <code>null</code> will be returned.
	 *
	 * @param key the configuration key
	 * @return the mapped value or <code>null</code>
	 */
	private String getConfig(String key) {
		return config.getProperty(key, System.getProperty(key));
	}

	/**
	 * Shows a <code>FileChooser</code> and returns the chosen <code>File</code>. Sets <code>lastChooseDir</code>
	 * to the parent file of the returned <code>File</code>.
	 *
	 * @param event
	 * 		the <code>ActionEvent</code> that occurred in the action listener
	 *
	 * @return the chosen <code>File</code> or <code>null</code> if the dialog was closed
	 */
	private File getChosenFile(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		Window window = ((Node) event.getTarget()).getScene().getWindow();

		if (lastChooseDir != null && lastChooseDir.isDirectory()) {
			chooser.setInitialDirectory(lastChooseDir);
		}

		return chooser.showOpenDialog(window);
	}

	/**
	 * Called when the 'Choose' button for the left file is clicked.
	 *
	 * @param event
	 * 		the <code>ActionEvent</code> that occurred
	 */
	public void chooseLeft(ActionEvent event) {
		File leftArtifact = getChosenFile(event);

		if (leftArtifact != null) {
			lastChooseDir = leftArtifact.getParentFile();
			left.setText(leftArtifact.getAbsolutePath());
		}
	}

	/**
	 * Called when the 'Choose' button for the base file is clicked.
	 *
	 * @param event
	 * 		the <code>ActionEvent</code> that occurred
	 */
	public void chooseBase(ActionEvent event) {
		File baseArtifact = getChosenFile(event);

		if (baseArtifact != null) {
			lastChooseDir = baseArtifact.getParentFile();
			base.setText(baseArtifact.getAbsolutePath());
		}
	}

	/**
	 * Called when the 'Choose' button for the right file is clicked.
	 *
	 * @param event
	 * 		the <code>ActionEvent</code> that occurred
	 */
	public void chooseRight(ActionEvent event) {
		File rightArtifact = getChosenFile(event);

		if (rightArtifact != null) {
			lastChooseDir = rightArtifact.getParentFile();
			right.setText(rightArtifact.getAbsolutePath());
		}
	}

	/**
	 * Called when the 'Choose' button for the JDime executable is clicked.
	 *
	 * @param event
	 * 		the <code>ActionEvent</code> that occurred
	 */
	public void chooseJDime(ActionEvent event) {
		File jDimeBinary = getChosenFile(event);

		if (jDimeBinary != null) {
			lastChooseDir = jDimeBinary.getParentFile();
			jDime.setText(jDimeBinary.getAbsolutePath());
		}
	}

	/**
	 * Called when the '>' button for the history is clicked.
	 */
	public void historyNext() {
		historyIndex.setValue(historyIndex.get() + 1);
	}

	/**
	 * Called when the '<' button for the history is clicked.
	 */
	public void historyPrevious() {
		historyIndex.setValue(historyIndex.get() - 1);
	}

	/**
	 * Called when the 'Run' button is clicked.
	 */
	public void runClicked() {
		boolean valid = textFields.stream().allMatch(tf -> {

			if (tf == cmdArgs) {
				return true;
			}

			if (tf == base) {
				return tf.getText().trim().isEmpty() || new File(tf.getText()).exists();
			}

			return new File(tf.getText()).exists();
		});

		if (!valid) {
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
				command.addAll(Arrays.asList(cmdArgs.getText().trim().split("\\s+")));
				command.add(left.getText());
				command.add(base.getText());
				command.add(right.getText());
				builder.command(command);

				File workingDir = new File(jDime.getText()).getParentFile();
				if (workingDir != null && workingDir.exists()) {
					builder.directory(workingDir);
				}

				if (debugMode.isSelected()) {
					builder.environment().put(STARTSCRIPT_JVM_ENV_VAR, JVM_DEBUG_PARAMS);
				}

				Process process = builder.start();
				process.waitFor();

				StringWriter writer = new StringWriter();
				IOUtils.copy(process.getInputStream(), writer, Charset.defaultCharset());

				return writer.toString();
			}
		};

		jDimeExec.setOnSucceeded(event -> {
			output.setText(jDimeExec.getValue());

			State currentState = State.of(GUI.this);
			if (history.isEmpty() || !history.get(history.size() - 1).equals(currentState)) {
				history.add(currentState);
			}

			textFields.forEach(textField -> textField.setDisable(false));
			buttons.forEach(button -> button.setDisable(false));
		});

		new Thread(jDimeExec).start();
	}
}
