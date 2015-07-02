/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
 */
package de.fosd.jdime.common;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;

/**
 * A generic <code>Artifact</code> that has a tree structure.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public abstract class Artifact<T extends Artifact<T>> implements Comparable<T> {

	/**
	 * Used to renumber artifacts.
	 * This number is mainly used for debugging purposes or when drawing the tree.
	 */
	private static int count = 1;

	/**
	 * Recursively renumbers the tree.
	 *
	 * @param artifact
	 *            root of the tree to renumber
	 */
	private static void renumber(final Artifact<?> artifact) {
		artifact.number = count;
		count++;
		for (int i = 0; i < artifact.getNumChildren(); i++) {
			renumber(artifact.getChild(i));
		}
	}

	/**
	 * Recursively renumbers the tree.
	 *
	 */
	public void renumberTree() {
		Artifact.count = 1;
		renumber(this);
	}

	/**
	 * Children of the artifact.
	 */
	protected ArtifactList<T> children = null;

	/**
	 * Left side of a conflict.
	 */
	T left = null;

	/**
	 * Right side of a conflict.
	 */
	T right = null;

	/**
	 * Whether this artifact represents a conflict.
	 */
	private boolean conflict = false;

	/**
	 * Map to store matches.
	 */
	LinkedHashMap<Revision, Matching<T>> matches = null;

	/**
	 * Whether the artifact has been already merged.
	 */
	private boolean merged;

	/**
	 * Number used to identify the artifact.
	 */
	private int number = -1;

	/**
	 * Parent artifact.
	 */
	private T parent;

	/**
	 * Revision the artifact belongs to.
	 */
	private Revision revision;

	/**
	 * Adds a child.
	 *
	 * @param child
	 *            child to add
	 * @return added child
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract T addChild(final T child) throws IOException;

	/**
	 * Adds a matching.
	 *
	 * @param matching
	 * 		matching to be added
	 */
	public void addMatching(Matching<T> matching) {
		if (matches == null) {
			matches = new LinkedHashMap<>();
		}

		matches.put(matching.getMatchingArtifact(this).getRevision(), matching);
	}

	/**
	 * Clones matches from another artifact.
	 *
	 * @param other
	 *            artifact to clone matches from
	 */
	@SuppressWarnings("unchecked")
	public void cloneMatches(T other) {

		if (other.matches == null) {
			return;
		}

		matches = new LinkedHashMap<>();

		for (Map.Entry<Revision, Matching<T>> entry : other.matches.entrySet()) {
			Matching<T> m = entry.getValue().clone();
			m.updateMatching((T) this);

			matches.put(entry.getKey(), m);
		}
	}

	public abstract Object clone();

	/**
	 * Returns an <code>Artifact</code> that represents a merge conflict.
	 * A conflict contains two alternative <code>Artifact</code> (left and right) and is handled in a special way
	 * while pretty-printed.
	 *
	 * @param left
	 *            left alternative <code>Artifact</code>
	 * @param right
	 *            right alternative <code>Artifact</code>
	 * @return conflict <code>Artifact</code>
	 */
	public abstract T createConflictArtifact(T left, T right);

	/**
	 * Returns an empty <code>Artifact</code>. This is used while performing two-way merges where the
	 * base <code>Artifact</code> is empty.
	 *
	 * @return empty <code>Artifact</code>
	 * @throws IOException
	 *             If a file is not found or cannot be created
	 */
	public abstract T createEmptyArtifact() throws IOException;

	/**
	 * Finds the root artifact and calls <code>dumpTree()</code> on it.
	 *
	 * This method is used for debugging JDime.
	 *
	 * @return <code>dumpTree()</code> of root artifact
	 */
	public String dumpRootTree() {
		if (getParent() != null) {
			return getParent().dumpRootTree();
		} else {
			return dumpTree();
		}
	}

	/**
	 * Returns the structure of the artifact as indented plain text.
	 *
	 * This method is used for debugging JDime.
	 *
	 * @return artifact structure as indented plain text
	 */
	public String dumpTree() {
		return dumpTree("");
	}

	/**
	 * Returns the structure of the artifact as indented plain text.
	 *
	 * This method is used for debugging JDime.
	 *
	 * @param indent
	 *            String used to indent the current artifact
	 *
	 * @return artifact structure as indented plain text
	 */
	protected abstract String dumpTree(String indent);

	/**
	 * Returns the AST in dot-format. {@link #toString()} will be used to label the nodes.
	 *
	 * @param includeNumbers
	 *            include node number in label if true
	 * @return AST in dot-format.
	 */
	public String dumpGraphvizTree(boolean includeNumbers, int virtualcount) {
		StringBuilder sb = new StringBuilder();

		if (isConflict()) {
			// insert virtual node
			String conflictId = "\"c" + virtualcount + "\"";
			sb.append(conflictId);
			sb.append("[label=\"Conflict\", fillcolor = red, style = filled]").append(System.lineSeparator());

			// left alternative
			sb.append(left.dumpGraphvizTree(includeNumbers, virtualcount));
			sb.append(conflictId).append("->").append(getGraphvizId(left)).
					append("[label=\"").append(left.getRevision()).append("\"]").append(";").append(System.lineSeparator());

			// right alternative
			sb.append(right.dumpGraphvizTree(includeNumbers, virtualcount));
			sb.append(conflictId).append("->").append(getGraphvizId(right)).
					append("[label=\"").append(right.getRevision()).append("\"]").append(";").append(System.lineSeparator());
		} else {
			sb.append(getGraphvizId(this)).append("[label=\"");

			// node label
			if (includeNumbers) {
				sb.append("(").append(getNumber()).append(") ");
			}

			sb.append(toString());

			sb.append("\"");

			if (hasMatches()) {
				sb.append(", fillcolor = green, style = filled");
			}

			sb.append("];");
			sb.append(System.lineSeparator());

			// children
			for (Artifact<T> child : getChildren()) {
				String childId = getGraphvizId(child);

				if (child.isConflict()) {
					virtualcount++;
					childId = "\"c" + virtualcount + "\"";
				}

				sb.append(child.dumpGraphvizTree(includeNumbers, virtualcount));

				// edge
				sb.append(getGraphvizId(this)).append("->").append(childId).append(";").append(System.lineSeparator());
			}
		}

		return sb.toString();
	}

	private String getGraphvizId(Artifact<T> artifact) {
		return "\"" + artifact.getId() + "\"";
	}

	/**
	 * Pretty-prints the <code>Artifact</code> to source code.
	 *
	 * @return Pretty-printed AST (source code)
	 */
	public abstract String prettyPrint();

	/**
	 * Returns true if this artifact physically exists.
	 *
	 * @return true if the artifact exists.
	 */
	public abstract boolean exists();

	/**
	 * Return child <code>Artifact</code> at position i.
	 *
	 * @param i
	 * 			position of child <code>Artifact</code>
	 * @return child <code>Artifact</code> at position i
	 */
	public T getChild(int i) {
		assert (children != null);
		return children.get(i);
	}

	/**
	 * Returns all children of the <code>Artifact</code>.
	 *
	 * @return the children of the <code>Artifact</code>
	 */
	public ArtifactList<T> getChildren() {
		if (isLeaf()) {
			return new ArtifactList<>();
		}

		return children;
	}

	public abstract void deleteChildren() throws IOException;

	/**
	 * Returns the identifier of the <code>Artifact</code>,
	 * which contains the <code>Revision</code> name and a number.
	 *
	 * This method is basically useful for debugging JDime.
	 *
	 * @return identifier of the <code>Artifact</code>
	 */
	public abstract String getId();

	/**
	 * Returns the <code>Matching</code> for a specific <code>Revision</code> or <code>null</code> if there is no such
	 * <code>Matching</code>.
	 *
	 * @param rev
	 *            <code>Revision</code>
	 * @return <code>Matching</code> with <code>Revision</code>
	 */
	public Matching<T> getMatching(Revision rev) {
		return matches == null ? null : matches.get(rev);
	}

	/**
	 * Returns the number of the <code>Artifact</code>.
	 *
	 * @return number of the <code>Artifact</code>
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Returns the number of children the <code>Artifact</code> has.
	 *
	 * @return number of children
	 */
	public int getNumChildren() {
		if (isLeaf()) {
			return 0;
		}

		return children == null ? 0 : children.size();
	}

	/**
	 * Returns the parent <code>Artifact</code>.
	 *
	 * @return the parent <code>Artifact></code>
	 */
	public T getParent() {
		return parent;
	}

	/**
	 * Returns the <code>Revision</code> the <code>Artifact</code> belongs to.
	 *
	 * @return the <code>Revision</code> the <code>Artifact</code> belongs to.
	 */
	public Revision getRevision() {
		return revision;
	}

	/**
	 * Returns key of statistical element.
	 *
	 * @param context
	 *            merge context
	 * @return key of statistical element
	 */
	public abstract String getStatsKey(MergeContext context);

	/**
	 * Returns the size of the subtree. The <code>Artifact</code> itself is not included.
	 *
	 * @return size of subtree
	 */
	public int getSubtreeSize() {
		int size = getNumChildren();

		for (int i = 0; i < getNumChildren(); i++) {
			size += getChild(i).getSubtreeSize();
		}

		return size;
	}

	/**
	 * Returns the size of the tree. The <code>Artifact</code> itself is also included.
	 *
	 * @return size of tree
	 */
	public int getTreeSize() {
		return getSubtreeSize() + 1;
	}

	/**
	 * Returns whether the <code>Artifact</code> or its subtree has changes.
	 *
	 * @return whether the <code>Artifact</code> or its subtree has changes
	 */
	public boolean hasChanges() {
		boolean hasChanges = !hasMatches();

		for (int i = 0; !hasChanges && i < getNumChildren(); i++) {
			hasChanges = getChild(i).hasChanges();
		}

		return hasChanges;
	}

	/**
	 * Returns true if the <code>Artifact</code> is a change.
	 *
	 * @return true if the <code>Artifact</code> is a change
	 */
	public boolean isChange() {
		return !hasMatches();
	}

	/**
	 * Returns true if the <code>Artifact</code> has children.
	 *
	 * @return true if the <code>Artifact</code> has children
	 */
	public boolean hasChildren() {
		return getNumChildren() > 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		return getId().equals(((T) o).getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	/**
	 * Returns whether this <code>Artifact</code> has any matches.
	 *
	 * @return true if the <code>Artifact</code> has matches
	 */
	public boolean hasMatches() {
		return matches != null && !matches.isEmpty();
	}

	/**
	 * Returns whether this <code>Artifact</code> has a <code>Matching</code> for a specific <code>Revision</code>.
	 *
	 * @param rev
	 *            <code>Revision</code>
	 * @return true if <code>Artifact</code> has a <code>Matching</code> with <code>Revision</code>
	 */
	public boolean hasMatching(Revision rev) {
		return matches != null && matches.containsKey(rev);
	}

	/**
	 * Returns whether a <code>Matching</code> exists for a specific <code>Artifact</code>.
	 *
	 * @param other
	 *            other <code>Artifact</code> to search <code>Matching</code>s for
	 * @return whether a <code>Matching</code> exists
	 */
	public boolean hasMatching(T other) {

		if (matches == null) {
			return false;
		}

		Matching<T> m = matches.get(other.getRevision());
		return m != null && m.getMatchingArtifact(this) == other;
	}

	/**
	 * Returns whether this <code>Artifact</code> has unique labels.
	 * If this is the case, a more efficient <code>UnorderedMatcher</code> can be used.
	 *
	 * @return whether the <code>Artifact</code> has unique labels
	 */
	public abstract boolean hasUniqueLabels();

	/**
	 * Returns true if the <code>Artifact</code> is a conflict node.
	 *
	 * @return true if the <code>Artifact</code> represents a conflict
	 */
	public boolean isConflict() {
		return conflict;
	}

	/**
	 * Returns true if the <code>Artifact</code> is empty.
	 *
	 * @return true if the <code>Artifact</code> is empty
	 */
	public abstract boolean isEmpty();

	/**
	 * Returns true if the <code>Artifact</code> is a leaf.
	 *
	 * @return true if the <code>Artifact</code> is a leaf
	 */
	public abstract boolean isLeaf();

	/**
	 * Returns true if the <code>Artifact</code> has already been merged.
	 * @return true if the <code>Artifact</code> has already been merged
	 */
	public boolean isMerged() {
		return merged;
	}

	/**
	 * Returns true if the declaration order of the <code>Artifact</code> is essential.
	 *
	 * @return true if the declaration order of the <code>Artifact</code> is essential
	 */
	public abstract boolean isOrdered();

	/**
	 * Returns true if the <code>Artifact</code> is the root node.
	 *
	 * @return true if the <code>Artifact</code> is the root node
	 */
	public boolean isRoot() {
		return getParent() == null;
	}

	/**
	 * Returns true, if this <code>Artifact</code> matches another <code>Artifact</code>.
	 *
	 * @param other
	 *            other <code>Artifact</code>
	 * @return true, if the <code>Artifact</code>s match
	 */
	public abstract boolean matches(T other);

	/**
	 * Returns true if matches were previously computed.
	 *
	 * @return true if matches were already computed
	 */
	public boolean matchingComputed() {
		return matches != null;
	}

	/**
	 * Performs a merge on the provided merge triple.
	 * This method selects the <code>MergeStrategy</code> and triggers the merge.
	 *
	 * @param operation
	 *            merge operation
	 * @param context
	 *            merge context
	 * @throws InterruptedException
	 *             If a thread is interrupted
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void merge(MergeOperation<T> operation, MergeContext context)
			throws IOException, InterruptedException;

	/**
	 * Sets the children of the <code>Artifact</code>.
	 *
	 * @param children
	 *            the new children to set
	 */
	public void setChildren(ArtifactList<T> children) {
		this.children = children;
	}

	/**
	 * Marks this <code>Artifact</code> as a conflict.
	 *
	 * @param left
	 *            left alternative
	 * @param right
	 *            right alternative
	 */
	void setConflict(T left, T right) {
		this.conflict = true;
		this.left = left;
		this.right = right;
	}

	/**
	 * Set whether the <code>Artifact</code> has already been merged.
	 */
	public void setMerged() {
		this.merged = true;
	}

	/**
	 * Sets the number of the <code>Artifact</code>
	 * @param number
	 *            the number to set
	 */
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * Sets the parent <code>Artifact</code>.
	 *
	 * @param parent
	 *            the parent to set
	 */
	void setParent(T parent) {
		this.parent = parent;
	}

	/**
	 * Sets the <code>Revision</code>.
	 *
	 * @param revision
	 *            the <code>Revision</code> to set
	 */
	public void setRevision(Revision revision) {
		setRevision(revision, false);
	}

	public void setRevision(Revision revision, boolean recursive) {
		this.revision = revision;

		if (recursive && children != null) {
			for (T child : children) {
				child.setRevision(revision, recursive);
			}
		}
	}

	@Override
	public abstract String toString();
}
