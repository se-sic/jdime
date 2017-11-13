/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.fosd.jdime.config.JDimeConfig;

import static de.fosd.jdime.config.JDimeConfig.ALLOW_INVALID;
import static de.fosd.jdime.config.JDimeConfig.BUFFERED_LINES;
import static de.fosd.jdime.config.JDimeConfig.DEFAULT_ARGS;
import static de.fosd.jdime.config.JDimeConfig.DEFAULT_BASE;
import static de.fosd.jdime.config.JDimeConfig.DEFAULT_JDIME_EXEC;
import static de.fosd.jdime.config.JDimeConfig.DEFAULT_LEFT;
import static de.fosd.jdime.config.JDimeConfig.DEFAULT_RIGHT;

/**
 * A simple JavaFX GUI for JDime.
 */
@SuppressWarnings("unused")
public final class GUI extends Application {

    private static final Logger LOG = Logger.getLogger(GUI.class.getCanonicalName());
    private static final String TITLE = "JDime";

    private static final String JVM_DEBUG_PARAMS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005";
    private static final String STARTSCRIPT_JVM_ENV_VAR = "JAVA_OPTS";

    private static final Pattern DUMP_GRAPH = Pattern.compile(".*-mode\\s+dumpgraph.*");

    @FXML
    ListView<String> output;
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
    private StackPane cancelPane;
    @FXML
    private GridPane controlsPane;
    @FXML
    private Button historyPrevious;
    @FXML
    private Button historyNext;

    private int bufferedLines;
    private boolean allowInvalid;

    private File lastChooseDir;
    private List<TextField> textFields;

    private long histHashLastSave;
    private History history;

    private Task<Void> jDimeExec;
    private Process jDimeProcess;

    /**
     * Launches the GUI with the given <code>args</code>.
     *
     * @param args
     *         the command line arguments
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

        loadConfig();

        history = new History();
        histHashLastSave = history.storeHash();
        historyNext.disableProperty().bind(history.hasNextProperty().not());
        historyPrevious.disableProperty().bind(history.hasPreviousProperty().not());

        primaryStage.setTitle(TITLE);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> {
            if (histHashLastSave != history.storeHash()) {
                Optional<ButtonType> res = showYesNoDialog("Unsaved changes to the history. Save now?", primaryStage.getOwner());

                if (res.isPresent()) {
                    if (res.get().getButtonData() == ButtonBar.ButtonData.YES) {
                        saveClicked(new ActionEvent(primaryStage, event.getTarget()));
                    } else if (res.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                        event.consume();
                    }
                } else {
                    event.consume();
                }
            }
        });

        primaryStage.show();
    }

    /**
     * Loads the config values from the <code>JDimeConfig</code>.
     */
    private void loadConfig() {
        JDimeConfig config = new JDimeConfig();

        config.get(DEFAULT_JDIME_EXEC).ifPresent(s -> jDime.setText(s.trim()));
        config.get(DEFAULT_ARGS).ifPresent(s -> cmdArgs.setText(s.trim()));
        config.get(DEFAULT_LEFT).ifPresent(left::setText);
        config.get(DEFAULT_BASE).ifPresent(base::setText);
        config.get(DEFAULT_RIGHT).ifPresent(right::setText);
        bufferedLines = config.getInteger(BUFFERED_LINES).orElse(100);
        allowInvalid = config.getBoolean(ALLOW_INVALID).orElse(false);
    }

    /**
     * Shows a <code>FileChooser</code> and returns the chosen <code>File</code>. Sets <code>lastChooseDir</code>
     * to the parent file of the returned <code>File</code>.
     *
     * @param event
     *         the <code>ActionEvent</code> that occurred in the action listener
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
     *         the <code>ActionEvent</code> that occurred
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
     *         the <code>ActionEvent</code> that occurred
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
     *         the <code>ActionEvent</code> that occurred
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
     *         the <code>ActionEvent</code> that occurred
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
        history.applyNext(this);
    }

    /**
     * Called when the '{@literal <}' button for the history is clicked.
     */
    public void historyPrevious() {
        history.applyPrevious(this);
    }

    /**
     * Called when the history 'Save' button is clicked.
     *
     * @param event the <code>ActionEvent</code> that occurred
     */
    public void saveClicked(ActionEvent event) {
        Window owner = ((Node) event.getTarget()).getScene().getWindow();
        FileChooser chooser = new FileChooser();

        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("History File", "*.xml"));
        chooser.setInitialFileName("History");

        File file = chooser.showSaveDialog(owner);

        if (file != null) {
            try {
                history.store(file);
                histHashLastSave = history.storeHash();
                showAlert("Save successful!", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not store the history in " + file.getAbsolutePath());
                showAlert("Save failed. See the log for more info.", Alert.AlertType.WARNING);
            }
        }
    }

    /**
     * Called when the history 'Load' button is clicked.
     *
     * @param event the <code>ActionEvent</code> that occurred
     */
    public void loadClicked(ActionEvent event) {
        Window owner = ((Node) event.getTarget()).getScene().getWindow();

        if (histHashLastSave != history.storeHash()) {
            Optional<ButtonType> res = showYesNoDialog("Save the current history before overwriting it?", owner);

            if (res.isPresent()) {
                if (res.get().getButtonData() == ButtonBar.ButtonData.YES) {
                    saveClicked(event);
                } else if (res.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
            } else {
                return;
            }
        }

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("History File", "*.xml"));

        File file = chooser.showOpenDialog(owner);

        if (file != null) {
            try {
                Optional<History> loadedHistory = History.load(file);

                if (loadedHistory.isPresent()) {
                    history = loadedHistory.get();
                    histHashLastSave = history.storeHash();
                    historyNext.disableProperty().bind(history.hasNextProperty().not());
                    historyPrevious.disableProperty().bind(history.hasPreviousProperty().not());
                    history.apply(this, history.getSize());
                    showAlert("Load successful!", Alert.AlertType.INFORMATION);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, e, () -> "Could not load the history from " + file.getAbsolutePath());
                showAlert("Load failed. See the log for more info.", Alert.AlertType.WARNING);
            }
        }
    }

    /**
     * Shows a simple <code>Alert</code> of the given <code>type</code>.
     *
     * @param content
     *         the content <code>String</code> for the <code>Alert</code>
     * @param type
     *         the type of the <code>Alert</code>
     */
    private void showAlert(String content, Alert.AlertType type) {
        Alert alert = new Alert(type);

        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    /**
     * Shows a blocking Yes/No/Cancel dialog and returns a <code>ButtonType</code> whose <code>ButtonData</code> is one
     * of the corresponding enum values ({@link ButtonBar.ButtonData#YES}, {@link ButtonBar.ButtonData#NO},
     * {@link ButtonBar.ButtonData#CANCEL_CLOSE}).
     *
     * @param content
     *         the content <code>String</code> for the <code>Alert</code>
     * @param owner
     *         the owner <code>Window</code> for the dialog, may be null in which case the dialog will be non-modal
     * @return the optional <code>ButtonType</code> returned from {@link Alert#showAndWait()}
     */
    private Optional<ButtonType> showYesNoDialog(String content, Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        ButtonType yesType = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noType = new ButtonType("No", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.getButtonTypes().setAll(yesType, noType, cancel);

        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }

        return alert.showAndWait();
    }

    /**
     * Called when the 'Cancel' button is clicked.
     *
     * @throws InterruptedException
     *         if the method is interrupted while destroying the JDime process
     */
    public void cancelClicked() throws InterruptedException {
        jDimeProcess.destroyForcibly().waitFor();
        jDimeExec.cancel(true);
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

        if (!valid && !allowInvalid) {
            return;
        }

        jDimeExec = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                ProcessBuilder builder = new ProcessBuilder();
                List<String> command = new ArrayList<>();
                String input;

                input = jDime.getText().trim();
                if (!input.isEmpty()) {
                    command.add(input);
                }

                List<String> args = Arrays.asList(cmdArgs.getText().trim().split("\\s+"));
                if (!args.isEmpty()) {
                    command.addAll(args);
                }

                input = left.getText().trim();
                if (!input.isEmpty()) {
                    command.add(input);
                }

                input = base.getText().trim();
                if (!input.isEmpty()) {
                    command.add(input);
                }

                input = right.getText().trim();
                if (!input.isEmpty()) {
                    command.add(input);
                }

                builder.command(command);
                builder.redirectErrorStream(true);

                File workingDir = new File(jDime.getText()).getParentFile();
                if (workingDir != null && workingDir.exists()) {
                    builder.directory(workingDir);
                }

                if (debugMode.isSelected()) {
                    builder.environment().put(STARTSCRIPT_JVM_ENV_VAR, JVM_DEBUG_PARAMS);
                }

                jDimeProcess = builder.start();

                Charset cs = StandardCharsets.UTF_8;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(jDimeProcess.getInputStream(), cs))) {
                    List<String> lines = new ArrayList<>(bufferedLines + 1);
                    boolean stop = false;
                    String line;

                    do {
                        do {
                            if ((line = r.readLine()) != null) {
                                lines.add(line);

                                if (lines.size() >= bufferedLines) {
                                    List<String> toAdd = new ArrayList<>(lines);
                                    Platform.runLater(() -> output.getItems().addAll(toAdd));
                                    lines.clear();
                                }
                            } else {
                                stop = true;
                            }
                        } while (r.ready());

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            stop = true;
                        }
                    } while (!Thread.interrupted() && !stop && jDimeProcess.isAlive());

                    Platform.runLater(() -> output.getItems().addAll(lines));
                }

                try {
                    jDimeProcess.waitFor();
                } catch (InterruptedException ignored) {
                    jDimeProcess.destroyForcibly();
                }

                return null;
            }
        };

        jDimeExec.setOnRunning(event -> {
            controlsPane.setDisable(true);
            cancelPane.setVisible(true);
        });

        jDimeExec.setOnSucceeded(event -> {
            tabPane.getTabs().retainAll(outputTab);

            if (isDumpGraph(cmdArgs.getText())) {
                GraphvizParser parser = new GraphvizParser(output.getItems());
                parser.setOnSucceeded(roots -> {
                    addTabs(parser.getValue());
                    reactivate();
                });
                parser.setOnFailed(event1 -> {
                    LOG.log(Level.WARNING, event1.getSource().getException(), () -> "Graphviz parsing failed.");
                    reactivate();
                });
                new Thread(parser).start();
            } else {
                reactivate();
            }
        });

        jDimeExec.setOnCancelled(event -> reactivate());

        jDimeExec.setOnFailed(event -> {
            LOG.log(Level.WARNING, event.getSource().getException(), () -> "JDime execution failed.");
            reactivate();
        });

        output.setItems(FXCollections.observableArrayList());

        Thread jDimeT = new Thread(jDimeExec);
        jDimeT.setName("JDime Task Thread");
        jDimeT.start();
    }

    /**
     * Saves the current state of the GUI to the history and then reactivates the user controls.
     */
    private void reactivate() {
        history.storeCurrent(this);
        cancelPane.setVisible(false);
        controlsPane.setDisable(false);
    }

    /**
     * Adds <code>Tab</code>s containing <code>TreeTableView</code>s for every <code>TreeDumpNode</code> root in the
     * given <code>List</code>.
     *
     * @param roots
     *         the roots of the trees to display
     */
    private void addTabs(List<TreeItem<TreeDumpNode>> roots) {
        roots.forEach(root -> tabPane.getTabs().add(getTreeTableViewTab(root)));
    }

    /**
     * Returns whether the given JDime command line arguments contain the parameters necessary to activate graph
     * dump mode.
     *
     * @param cmdArgs
     *         the JDime command line arguments
     * @return true iff JDime starts in dump graph mode with the given command line arguments
     */
    static boolean isDumpGraph(String cmdArgs) {
        return DUMP_GRAPH.matcher(cmdArgs).matches();
    }

    /**
     * Returns a <code>Tab</code> containing a <code>TreeTableView</code> displaying the with the given
     * <code>root</code>.
     *
     * @param root
     *         the root of the tree to display
     * @return a <code>Tab</code> containing the tree
     */
    static Tab getTreeTableViewTab(TreeItem<TreeDumpNode> root) {
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
                    LOG.fine(() -> String.format("Could not convert '%s' to a JavaFX Color.", color));
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
