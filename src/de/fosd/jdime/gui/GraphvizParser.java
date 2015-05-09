package de.fosd.jdime.gui;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A <code>Task</code> parsing a <code>String</code> produced by JDime using '-mode dumpgraph' into
 * a <code>List</code> of <code>TreeItem</code>s representing root nodes for trees of AST nodes.
 */
class GraphvizParser extends Task<List<TreeItem<TreeDumpNode>>> {

	private static final Pattern node = Pattern.compile("([1-9]+)\\[label=\"\\([1-9]+\\) (.+)\"\\];");
	private static final Pattern connection = Pattern.compile("([1-9]+)->([1-9]+);");

	private String text;

	/**
	 * Constructs a new <code>GraphvizParser</code> parsing the given <code>String</code> when {@link #call()} is
	 * called.
	 *
	 * @param text the <code>String</code> to parse
	 */
	public GraphvizParser(String text) {
		this.text = text;
	}

	@Override
	protected List<TreeItem<TreeDumpNode>> call() throws Exception {
		List<TreeItem<TreeDumpNode>> roots = new ArrayList<>();

		return roots;
	}
}
