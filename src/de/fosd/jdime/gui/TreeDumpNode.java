package de.fosd.jdime.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A bean class encapsulating parameters parsed from a Graphviz node declaration.
 */
class TreeDumpNode {

	private StringProperty id;
	private StringProperty label;
	private StringProperty fillColor;

	/**
	 * Creates a new <code>TreeDumpNode</code> encapsulating the given <code>id</code> and <code>astType</code>.
	 *
	 * @param id the ID of the node
	 * @param label the label of the node
	 */
	public TreeDumpNode(String id, String label) {
		this.id = new SimpleStringProperty(id);
		this.label = new SimpleStringProperty(label);
		this.fillColor = new SimpleStringProperty();
	}

	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id.get();
	}

	/**
	 * Returns the ID property.
	 *
	 * @return the ID <code>StringProperty</code>
	 */
	public StringProperty idProperty() {
		return id;
	}

	/**
	 * Sets id to the given value.
	 *
	 * @param id the new id
	 */
	public void setId(String id) {
		this.id.set(id);
	}

	/**
	 * Returns the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label.get();
	}

	/**
	 * Returns the label property.
	 *
	 * @return the label <code>StringProperty</code>
	 */
	public StringProperty labelProperty() {
		return label;
	}

	/**
	 * Sets label to the given value.
	 *
	 * @param label the new label
	 */
	public void setLabel(String label) {
		this.label.set(label);
	}

	/**
	 * Returns the fill color.
	 *
	 * @return the fill color
	 */
	public String getFillColor() {
		return fillColor.get();
	}

	/**
	 * Returns the fill color property.
	 *
	 * @return the fill color <code>StringProperty</code>
	 */
	public StringProperty fillColorProperty() {
		return fillColor;
	}

	/**
	 * Sets fill color to the given value.
	 *
	 * @param fillColor the new fill color
	 */
	public void setFillColor(String fillColor) {
		this.fillColor.set(fillColor);
	}
}
