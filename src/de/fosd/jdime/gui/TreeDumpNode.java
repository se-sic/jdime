package de.fosd.jdime.gui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TreeDumpNode {

	private IntegerProperty id;
	private StringProperty astType;

	public TreeDumpNode(int id, String astType) {
		this.id = new SimpleIntegerProperty(id);
		this.astType = new SimpleStringProperty(astType);
	}

	public int getId() {
		return id.get();
	}

	public IntegerProperty idProperty() {
		return id;
	}

	public void setId(int id) {
		this.id.set(id);
	}

	public String getAstType() {
		return astType.get();
	}

	public StringProperty astTypeProperty() {
		return astType;
	}

	public void setAstType(String astType) {
		this.astType.set(astType);
	}
}
