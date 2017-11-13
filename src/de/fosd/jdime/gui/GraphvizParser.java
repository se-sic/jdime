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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

/**
 * A <code>Task</code> parsing a <code>String</code> produced by JDime using '-mode dumpgraph' into a <code>List</code>
 * of <code>TreeItem</code>s representing root nodes for trees of AST nodes.
 */
class GraphvizParser extends Task<List<TreeItem<TreeDumpNode>>> {

    private static final Pattern digraphStart = Pattern.compile(".*digraph ast \\{");
    private static final Pattern digraphEnd = Pattern.compile("\\}");
    private static final Pattern node = Pattern.compile("\"?([^\"]+)\"?\\[label=\"\\([0-9]+\\) (.+)\"(, fillcolor = (.+),)?.*\\];");
    private static final Pattern connection = Pattern.compile("\"?([^\"]+)\"?->\"?([^\"]+)\"?;");

    private List<String> text;

    /**
     * Constructs a new <code>GraphvizParser</code> parsing the given lines when {@link #call()} is
     * called.
     *
     * @param text
     *         the lines to parse
     */
    public GraphvizParser(List<String> text) {
        this.text = text;
    }

    @Override
    protected List<TreeItem<TreeDumpNode>> call() {
        List<TreeItem<TreeDumpNode>> roots = new ArrayList<>();
        Map<String, TreeItem<TreeDumpNode>> nodes = new HashMap<>();
        List<TreeItem<TreeDumpNode>> noParent = new ArrayList<>();

        for (String line : text) {

            if (digraphStart.matcher(line).matches()) {
                nodes.clear();
                noParent.clear();
            } else if (digraphEnd.matcher(line).matches()) {
                if (noParent.size() == 1) {
                    roots.add(noParent.get(0));
                } else {
                    System.err.println("Connections in the Graphviz format are wrong, there are " + noParent.size() +
                            " nodes without a parent node.");
                }
            } else {
                Matcher matcher;

                if ((matcher = node.matcher(line)).matches()) {
                    String id = matcher.group(1);
                    String label = matcher.group(2);
                    String color = matcher.group(4);
                    TreeDumpNode node = new TreeDumpNode(id, label);
                    TreeItem<TreeDumpNode> item = new TreeItem<>(node);

                    if (color != null) {
                        node.setFillColor(color);
                    }

                    nodes.put(id, item);
                    noParent.add(item);
                } else if ((matcher = connection.matcher(line)).matches()) {
                    String leftId = matcher.group(1);
                    String rightId = matcher.group(2);
                    TreeItem<TreeDumpNode> left = nodes.get(leftId);
                    TreeItem<TreeDumpNode> right = nodes.get(rightId);

                    left.getChildren().add(right);
                    noParent.remove(right);
                }
            }
        }

        return roots;
    }
}
