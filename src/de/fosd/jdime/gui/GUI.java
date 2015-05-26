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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

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

	public static final Pattern DUMP_GRAPH = Pattern.compile(".*-mode\\s+dumpgraph.*");

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
	CheckBox debugMode;
	@FXML
	TabPane tabPane;
	@FXML
	Tab outputTab;
	@FXML
	private GridPane controlsPane;
	@FXML
	private Button historyPrevious;
	@FXML
	private Button historyNext;

	private Properties config;

	private File lastChooseDir;
	private List<TextField> textFields;

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
		historyIndex = new SimpleIntegerProperty(0);
		history = FXCollections.observableArrayList();
		historyListProp = new SimpleListProperty<>(history);
		config = new Properties();

		BooleanBinding noPrev = historyListProp.emptyProperty().or(historyIndex.isEqualTo(0));
		BooleanBinding noNext = historyListProp.emptyProperty().or(historyIndex.greaterThanOrEqualTo(historyListProp.sizeProperty()));
		historyNext.disableProperty().bind(noNext);
		historyPrevious.disableProperty().bind(noPrev);

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
		getConfig(JDIME_EXEC_KEY).ifPresent(s -> jDime.setText(s.trim()));
		getConfig(JDIME_DEFAULT_ARGS_KEY).ifPresent(s -> cmdArgs.setText(s.trim()));
		getConfig(JDIME_DEFAULT_LEFT_KEY).ifPresent(left::setText);
		getConfig(JDIME_DEFAULT_BASE_KEY).ifPresent(base::setText);
		getConfig(JDIME_DEFAULT_RIGHT_KEY).ifPresent(right::setText);
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
	 * If no environment variable named <code>key</code> exists an empty <code>Optional</code> will be returned.
	 *
	 * @param key the configuration key
	 * @return optionally the mapped value
	 */
	private Optional<String> getConfig(String key) {
		String value = config.getProperty(key, System.getProperty(key));

		if (value != null) {
			return Optional.of(value);
		} else {
			return Optional.empty();
		}
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
	 * Called when the '{@literal >}' button for the history is clicked.
	 */
	public void historyNext() {
		historyIndex.setValue(historyIndex.get() + 1);

		if (historyIndex.get() == history.size()) {
			inProgress.applyTo(this);
		} else {
			history.get(historyIndex.get()).applyTo(this);
		}
	}

	/**
	 * Called when the '{@literal <}' button for the history is clicked.
	 */
	public void historyPrevious() {

		if (historyIndex.get() == history.size()) {
			inProgress = State.of(this);
		}

		historyIndex.setValue(historyIndex.get() - 1);
		history.get(historyIndex.get()).applyTo(this);
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

		controlsPane.setDisable(true);

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
				StringBuilder text = new StringBuilder();

				Charset cs = StandardCharsets.UTF_8;
				try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), cs))) {
					r.lines().forEach(line -> {
						text.append(line).append(System.lineSeparator());
						updateMessage(text.toString());
					});
				}

				process.waitFor();
				return text.toString();
			}
		};

		jDimeExec.messageProperty().addListener((observable, oldValue, newValue) -> {
			output.setText(newValue);
		});

		jDimeExec.setOnSucceeded(event -> {
			boolean dumpGraph = DUMP_GRAPH.matcher(cmdArgs.getText()).matches();
			tabPane.getTabs().retainAll(outputTab);

			if (dumpGraph) {
				GraphvizParser parser = new GraphvizParser(jDimeExec.getValue());
				parser.setOnSucceeded(roots -> {
					addTabs(parser.getValue());
					reactivate();
				});
				parser.setOnFailed(event1 ->  {
					System.err.println(event1.getSource().getException().getMessage());
					reactivate();
				});
				new Thread(parser).start();
			} else {
				reactivate();
			}
		});

		jDimeExec.setOnFailed(event -> {
			System.err.println(event.getSource().getException().getMessage());
			reactivate();
		});

		new Thread(jDimeExec).start();
	}

	/**
	 * Saves the current state of the GUI to the history and then reactivates the user controls.
	 */
	private void reactivate() {
		State currentState = State.of(GUI.this);

		if (history.isEmpty() || !history.get(history.size() - 1).equals(currentState)) {
			history.add(currentState);
			historyIndex.setValue(history.size());
		}

		controlsPane.setDisable(false);
	}

	/**
	 * Adds <code>Tab</code>s containing <code>TreeTableView</code>s for every <code>TreeDumpNode</code> root in the
	 * given <code>List</code>.
	 *
	 * @param roots
	 * 		the roots of the trees to display
	 */
	private void addTabs(List<TreeItem<TreeDumpNode>> roots) {
		roots.forEach(root -> tabPane.getTabs().add(getTreeTableViewTab(root)));
	}

	/**
	 * Returns a <code>Tab</code> containing a <code>TreeTableView</code> displaying the with the given
	 * <code>root</code>.
	 *
	 * @param root
	 * 		the root of the tree to display
	 * @return a <code>Tab</code> containing the tree
	 */
	private Tab getTreeTableViewTab(TreeItem<TreeDumpNode> root) {
		TreeTableView<TreeDumpNode> tableView = new TreeTableView<>(root);
		TreeTableColumn<TreeDumpNode, String> id = new TreeTableColumn<>("ID");
		TreeTableColumn<TreeDumpNode, String> label = new TreeTableColumn<>("AST Type");

		tableView.setRowFactory(param -> {
			TreeTableRow<TreeDumpNode> row = new TreeTableRow<>();
			TreeDumpNode node = row.getItem();

			if (node == null) {
				return row;
			}

			String color = node.getFillColor();

			if (color != null) {

				try {
					BackgroundFill fill = new BackgroundFill(Color.valueOf(color), CornerRadii.EMPTY, Insets.EMPTY);
					row.setBackground(new Background(fill));
				} catch (IllegalArgumentException e) {
					System.err.println("Could not convert \'" + color + "\' to a JavaFX Color.");
				}
			}

			return row;
		});

		id.setCellValueFactory(param -> param.getValue().getValue().idProperty());
		label.setCellValueFactory(param -> param.getValue().getValue().labelProperty());

		tableView.getColumns().setAll(Arrays.asList(label, id));
		return new Tab("Tree View", tableView);
	}
}
