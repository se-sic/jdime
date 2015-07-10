package de.fosd.jdime.gui;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;

/**
 * A Bean encapsulating the state of the gui at one point.
 */
final class State implements Serializable {

    private List<Tab> treeViewTabs;
    private ObservableList<String> output;
    private String left;
    private String base;
    private String right;
    private String jDime;
    private String cmdArgs;
    private boolean debugMode;

    private State() {

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
}
