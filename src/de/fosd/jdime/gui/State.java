package de.fosd.jdime.gui;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;

import de.fosd.jdime.JDimeConfig;
import de.uni_passau.fim.seibt.kvconfig.Config;

import static de.fosd.jdime.JDimeConfig.DEFAULT_ARGS;
import static de.fosd.jdime.JDimeConfig.DEFAULT_BASE;
import static de.fosd.jdime.JDimeConfig.DEFAULT_JDIME_EXEC;
import static de.fosd.jdime.JDimeConfig.DEFAULT_LEFT;
import static de.fosd.jdime.JDimeConfig.DEFAULT_RIGHT;

/**
 * A Bean encapsulating the state of the gui at one point.
 */
final class State {

    private List<Tab> treeViewTabs;
    private ObservableList<String> output;
    private String left;
    private String base;
    private String right;
    private String jDime;
    private String cmdArgs;
    private boolean debugMode;

    private State() {}

    /**
     * Returns a <code>State</code> with (where possible) the values initialized to the defaults specified using the
     * {@link JDimeConfig}.
     *
     * @return the default <code>State</code>
     */
    public static State defaultState() {
        State state = new State();
        Config config = JDimeConfig.getConfig();

        state.treeViewTabs = new ArrayList<>();
        state.output = FXCollections.observableArrayList();

        state.left = config.get(DEFAULT_LEFT).orElse("");
        state.base = config.get(DEFAULT_BASE).orElse("");
        state.right = config.get(DEFAULT_RIGHT).orElse("");
        state.jDime = config.get(DEFAULT_JDIME_EXEC).orElse("");
        state.cmdArgs = config.get(DEFAULT_ARGS).orElse("");

        state.debugMode = false;

        return state;
    }

    /**
     * Returns a <code>State</code> instance containing the current state of the given <code>GUI</code>.
     *
     * @param gui
     *         the <code>GUI</code> whose state is to be copied
     *
     * @return the resulting <code>State</code>
     */
    public static State of(GUI gui) {
        State state = new State();

        state.treeViewTabs = gui.tabPane.getTabs().stream().filter(tab -> tab != gui.outputTab).collect(Collectors.toList());
        state.output = gui.output.getItems();
        state.left = gui.left.getText();
        state.base = gui.base.getText();
        state.right = gui.right.getText();
        state.jDime = gui.jDime.getText();
        state.cmdArgs = gui.cmdArgs.getText();
        state.debugMode = gui.debugMode.isSelected();

        return state;
    }

    /**
     * Applies the state stored in this <code>State</code> to the given <code>GUI</code>. Must be called in the
     * JavaFX Application thread.
     *
     * @param gui
     *         the <code>GUI</code> to apply the stored state to
     */
    public void applyTo(GUI gui) {
        gui.tabPane.getTabs().retainAll(gui.outputTab);
        gui.tabPane.getTabs().addAll(treeViewTabs);
        gui.output.setItems(output);
        gui.left.setText(left);
        gui.base.setText(base);
        gui.right.setText(right);
        gui.jDime.setText(jDime);
        gui.cmdArgs.setText(cmdArgs);
        gui.debugMode.setSelected(debugMode);
    }

    public List<Tab> getTreeViewTabs() {
        return treeViewTabs;
    }

    public void setTreeViewTabs(List<Tab> treeViewTabs) {
        this.treeViewTabs = treeViewTabs;
    }

    public ObservableList<String> getOutput() {
        return output;
    }

    public String getLeft() {
        return left;
    }

    public String getBase() {
        return base;
    }

    public String getRight() {
        return right;
    }

    public String getjDime() {
        return jDime;
    }

    public String getCmdArgs() {
        return cmdArgs;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        State state = (State) o;

        return Objects.equals(debugMode, state.debugMode) &&
                Objects.equals(treeViewTabs, state.treeViewTabs) &&
                Objects.equals(output, state.output) &&
                Objects.equals(left, state.left) &&
                Objects.equals(base, state.base) &&
                Objects.equals(right, state.right) &&
                Objects.equals(jDime, state.jDime) &&
                Objects.equals(cmdArgs, state.cmdArgs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(treeViewTabs, output, left, base, right, jDime, cmdArgs, debugMode);
    }

    /**
     * Returns a hash of the fields that are included in the output of {@link History#store(File)} or
     * {@link History#store(OutputStream)}.
     *
     * @return the hash code
     */
    public int storeHash() {
        return Objects.hash(output, left, base, right, jDime, cmdArgs, debugMode);
    }

    /**
     * This method will be called by the JVM after deserialization. In it we ensure that the 'treeViewTabs' list
     * (which is implicitly represented in the XML) is not null (but rather empty) if it was empty at the time of
     * serialization.
     *
     * @return <code>this</code>
     */
    private Object readResolve() {

        if (treeViewTabs == null) {
            treeViewTabs = new ArrayList<>();
        }

        return this;
    }
}
