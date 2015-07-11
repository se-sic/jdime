package de.fosd.jdime.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import com.sun.javafx.collections.ObservableListWrapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * A history of GUI <code>State</code>s. The <code>History</code> stores an index into the list of stored
 * <code>State</code>s it represents. The <code>applyX</code> methods advance/regress the index by one and apply
 * the <code>State</code> at the new index to the <code>GUI</code>.
 */
public class History {

    private static XStream serializer;

    static {
        serializer = new XStream();
        serializer.registerConverter(new Converter() {

            private Converter c = serializer.getConverterLookup().lookupConverterForType(ArrayList.class);

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                c.marshal(new ArrayList<>((Collection<?>) source), writer, context);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                return FXCollections.observableArrayList((Collection<?>) c.unmarshal(reader, context));
            }

            @Override
            public boolean canConvert(Class type) {
                return type.equals(ObservableListWrapper.class);
            }
        });

        serializer.alias("root", ObservableListWrapper.class);

        serializer.omitField(State.class, "treeViewTabs");
        serializer.addImplicitCollection(State.class, "output");
        serializer.alias("state", State.class);
    }

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

        this.hasPrevious = prevProperty;
        this.hasNext = nextProperty;
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

    public static History load(GUI gui, File file) throws IOException {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return load(gui, is);
        }
    }

    public static History load(GUI gui, InputStream stream) {
        History history = new History(gui);
        history.history.setAll((Collection<? extends State>) serializer.fromXML(stream));
        history.history.forEach(state -> {
            GraphvizParser p = new GraphvizParser(state.getOutput());
            state.setTreeViewTabs(p.call().stream().map(GUI::getTreeTableViewTab).collect(Collectors.toList()));
        });

        return history;
    }

    public void store(File file) throws IOException {

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            store(os);
        }
    }

    public void store(OutputStream stream) {
        serializer.toXML(history.get(), stream);
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
