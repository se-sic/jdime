package de.fosd.jdime.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

/**
 * A history of GUI <code>State</code>s.
 */
public class History {

    private GUI gui;
    
    private IntegerProperty index;
    private SimpleListProperty<State> history;
    private State inProgress;

    private ReadOnlyBooleanProperty hasPrevious;
    private ReadOnlyBooleanProperty hasNext;

    /**
     * Constructs a new <code>History</code> for the <code>State</code>s of the given <code>GUI</code>.
     *
     * @param gui the <code>GUI</code> whose <code>State</code>s are to be saved
     */
    public History(GUI gui) {
        this.gui = gui;
        this.index = new SimpleIntegerProperty(0);
        this.history = new SimpleListProperty<>(FXCollections.observableArrayList());

        BooleanProperty prevProperty = new SimpleBooleanProperty();
        prevProperty.bind(history.emptyProperty().or(index.isEqualTo(0)).not());

        BooleanProperty nextProperty = new SimpleBooleanProperty();
        nextProperty.bind(history.emptyProperty().or(index.greaterThanOrEqualTo(history.sizeProperty())).not());

        this.hasPrevious = ReadOnlyBooleanProperty.readOnlyBooleanProperty(prevProperty);
        this.hasNext = ReadOnlyBooleanProperty.readOnlyBooleanProperty(nextProperty);
    }

    public void applyNext() {
        index.setValue(index.get() + 1);

        if (index.get() == history.size()) {
            inProgress.applyTo(gui);
        } else {
            history.get(index.get()).applyTo(gui);
        }
    }

    public void applyPrevious() {
        if (index.get() == history.size()) {
            inProgress = State.of(gui);
        }

        index.setValue(index.get() - 1);
        history.get(index.get()).applyTo(gui);
    }

    public void storeCurrent() {
        State currentState = State.of(gui);

        if (history.isEmpty() || !history.get(history.size() - 1).equals(currentState)) {
            history.add(currentState);
            index.setValue(history.size());
        }
    }

    public int getSize() {
        return history.sizeProperty().get();
    }

    public ReadOnlyIntegerProperty sizeProperty() {
        return history.sizeProperty();
    }

    public int getIndex() {
        return index.get();
    }

    public IntegerProperty indexProperty() {
        return index;
    }

    public boolean hasPrevious() {
        return hasPrevious.get();
    }

    public ReadOnlyBooleanProperty hasPreviousProperty() {
        return hasPrevious;
    }

    public boolean hasNext() {
        return hasNext.get();
    }

    public ReadOnlyBooleanProperty hasNextProperty() {
        return hasNext;
    }
}
