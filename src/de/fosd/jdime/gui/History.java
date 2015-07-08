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
 * A history of GUI <code>State</code>s. The <code>History</code> stores an index into the list of stored
 * <code>State</code>s it represents. The <code>applyX</code> methods advance/regress the index by one and apply
 * the <code>State</code> at the new index to the <code>GUI</code>.
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

    /**
     * If possible, advances the index by one and applies the new <code>State</code> to the <code>GUI</code>.
     * If the new index is the size of the list the previously stored in-progress <code>State</code> of the
     * <code>GUI</code> is applied.
     */
    public void applyNext() {
        if (getIndex() == getSize()) {
            return;
        }
        
        index.setValue(getIndex() + 1);

        if (getIndex() == getSize()) {
            inProgress.applyTo(gui);
        } else {
            history.get(getIndex()).applyTo(gui);
        }
    }

    /**
     * If possible, regresses the index by one and applies the new <code>State</code> to the <code>GUI</code>.
     * If the index was the size of the list (indicating that the current <code>State</code> of the <code>GUI</code>
     * if not one of the archived states) the current <code>State</code> is stored.
     */
    public void applyPrevious() {
        if (getIndex() == 0) {
            return;
        }
        
        if (getIndex() == getSize()) {
            inProgress = State.of(gui);
        }

        index.setValue(getIndex() - 1);
        history.get(getIndex()).applyTo(gui);
    }

    /**
     * Adds the current state of the <code>GUI</code> to the <code>History</code> and sets the index to the size of the
     * list (indicating that the current state of the GUI is not one of the archived states).
     */
    public void storeCurrent() {
        State currentState = State.of(gui);

        if (history.isEmpty() || !history.get(getSize() - 1).equals(currentState)) {
            history.add(currentState);
            index.setValue(getSize());
        }
    }

    public int getSize() {
        return history.size();
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
