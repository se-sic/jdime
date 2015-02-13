/*******************************************************************************
 * Copyright (C) 2013-2015 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;

/**
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
	protected static void renumber(final Artifact<?> artifact) {
		artifact.number = count;
		count++;
		for (int i = 0; i < artifact.getNumChildren(); i++) {
			renumber(artifact.getChild(i));
		}
	}

	/**
	 * Recursively renumbers the tree.
	 *
	 * @param count
	 *            number to start with
	 * @param artifact
	 *            root of the tree to renumber
	 */
	protected static void renumber(final int count, final Artifact<?> artifact) {
		Artifact.count = count;
		renumber(artifact);
	}

	/**
	 * Children of the artifact.
	 */
	protected ArtifactList<T> children = null;

	/**
	 * Left side of a conflict.
	 */
	protected T left = null;

	/**
	 * Right side of a conflict.
	 */
	protected T right = null;

	/**
	 * Whether this artifact represents a conflict.
	 */
	private boolean conflict = false;

	/**
	 * If true, this artifact is an empty dummy.
	 */
	private boolean emptyDummy = false;

	/**
	 * Map to store matches.
	 */
	protected LinkedHashMap<Revision, Matching<T>> matches = null;

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
	 *            matching to be added
	 */
	@SuppressWarnings("unchecked")
	public final void addMatching(final Matching<T> matching) {
		if (matches == null) {
			matches = new LinkedHashMap<>();
		}

		assert (matching != null);
		matches.put(matching.getMatchingArtifact((T) this).getRevision(),
				matching);
	}

	/**
	 * Clones matches from another artifact.
	 *
	 * @param other
	 *            artifact to clone matches from
	 */
	@SuppressWarnings("unchecked")
	public final void cloneMatches(final T other) {
		if (other.matches != null) {
			matches = new LinkedHashMap<>();
			Set<Revision> matchingRevisions = other.matches.keySet();

			for (Revision rev : matchingRevisions) {
				Matching<T> m = (Matching<T>) other.matches.get(rev).clone();
				m.updateMatching((T) this);
				matches.put(rev, m);
			}
		}
	}

	/**
	 * Copies an @code{Artifact}.
	 *
	 * @param destination
	 *            destination artifact
	 * @throws IOException
	 *             If an input or output exception occurs.
	 */
	public abstract void copyArtifact(final T destination) throws IOException;

	/**
	 * Creates an @code{Artifact}.
	 *
	 * @param isLeaf
	 *            if true, a leaf type artifact will be created
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void createArtifact(boolean isLeaf) throws IOException;

	/**
	 * Returns a conflict artifact.
	 *
	 * @param type
	 *            of node
	 * @param left
	 *            left alternatives
	 * @param right
	 *            right alternatives
	 * @return conflict artifact
	 * @throws FileNotFoundException
	 *             If a file is not found
	 */
	public abstract T createConflictDummy(final T type, final T left,
			final T right) throws FileNotFoundException;

	/**
	 * Returns a dummy @code{Artifact}.
	 *
	 * @return dummy artifact
	 * @throws FileNotFoundException
	 *             If a file is not found
	 */
	public abstract T createEmptyDummy() throws FileNotFoundException;

	/**
	 * Finds the root artifact and calls <code>dumpTree()</code> on it.
	 *
	 * @return <code>dumpTree()</code> of root artifact
	 */
	public final String dumpRootTree() {
		if (getParent() != null) {
			return getParent().dumpRootTree();
		} else {
			return dumpTree();
		}
	}

	/**
	 * Returns the structure of the artifact as indented plain text.
	 *
	 * @return artifact structure as indented plain text
	 */
	public final String dumpTree() {
		return dumpTree("");
	}

	/**
	 * Returns the structure of the artifact as indented plain text.
	 *
	 * @param indent
	 *            String used to indent the current artifact
	 *
	 * @return artifact structure as indented plain text
	 */
	protected abstract String dumpTree(final String indent);

	/**
	 * Returns true if this artifact physically exists.
	 *
	 * @return true if the artifact exists.
	 */
	public abstract boolean exists();

	/**
	 * Force renumbering of the tree.
	 */
	public final void forceRenumbering() {
		renumber(1, this);
	}

	/**
	 * Return child at position i.
	 *
	 * @param i
	 *            position of child
	 * @return child at position i
	 */
	public final T getChild(final int i) {
		assert (children != null);
		return children.get(i);
	}

	/**
	 * Returns the children of the artifact.
	 *
	 * @return the children of the artifact
	 */
	public final ArtifactList<T> getChildren() {
		if (isLeaf()) {
			return new ArtifactList<>();
		}

		if (children == null) {
			initializeChildren();
		}

		return children;
	}

	/**
	 * Returns identifier in the form of revision:number.
	 *
	 * @return identifier in the form revision:number
	 */
	public abstract String getId();

	/**
	 * Returns the matching for a specific revision or null if there is no such
	 * matching.
	 *
	 * @param rev
	 *            revision
	 * @return matching with revision
	 */
	public final Matching<T> getMatching(final Revision rev) {
		return matches == null ? null : matches.get(rev);
	}

	/**
	 * @return the number
	 */
	public final int getNumber() {
		return number;
	}

	/**
	 * Returns the number of children the artifact has.
	 *
	 * @return number of children
	 */
	public final int getNumChildren() {
		if (isLeaf()) {
			return 0;
		}

		if (children == null) {
			initializeChildren();
		}

		return children == null ? 0 : children.size();
	}

	/**
	 * Returns the number of matches.
	 *
	 * @return number of matches
	 */
	public final int getNumMatches() {
		return matches == null ? 0 : matches.size();
	}

	/**
	 * Returns the parent artifact.
	 *
	 * @return the parent artifact
	 */
	public final T getParent() {
		return parent;
	}

	/**
	 * Returns the revision the artifact belongs to.
	 *
	 * @return the revision the artifact belongs to.
	 */
	public final Revision getRevision() {
		return revision;
	}

	/**
	 * Returns key of statistical element.
	 *
	 * @param context
	 *            merge context
	 * @return key of statistical element
	 */
	public abstract String getStatsKey(final MergeContext context);

	/**
	 * Returns the size of the subtree. The node itself is not included.
	 *
	 * @return size of subtree
	 */
	public final int getSubtreeSize() {
		int size = getNumChildren();

		for (int i = 0; i < getNumChildren(); i++) {
			size += getChild(i).getSubtreeSize();
		}

		return size;
	}

	/**
	 * Returns the size of the tree. The node itself is also included.
	 *
	 * @return size of tree
	 */
	public final int getTreeSize() {
		return getSubtreeSize() + 1;
	}

	/**
	 * Returns whether the subtree has changes.
	 *
	 * @return whether subtree has changes
	 */
	public final boolean hasChanges() {
		boolean hasChanges = !hasMatches();

		for (int i = 0; !hasChanges && i < getNumChildren(); i++) {
			hasChanges = getChild(i).hasChanges();
		}

		return hasChanges;
	}

	/**
	 * Returns whether the artifact has changes.
	 *
	 * @param recursive
	 *            If true, the whole subtree is checked
	 * @return whether artifact has changes
	 */
	public final boolean hasChanges(final boolean recursive) {
		if (recursive) {
			return hasChanges();
		} else {
			boolean hasChanges = !hasMatches();

			for (int i = 0; !hasChanges && i < getNumChildren(); i++) {
				hasChanges = !getChild(i).hasMatches();
			}

			return hasChanges;
		}
	}

	/**
	 * Returns true if the artifact has children.
	 *
	 * @return true if the artifact has children
	 */
	public final boolean hasChildren() {
		return getNumChildren() > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public abstract int hashCode();

	/**
	 * Returns whether this node has any matches.
	 *
	 * @return true if the node has matches
	 */
	public final boolean hasMatches() {
		return matches != null && !matches.isEmpty();
	}

	/**
	 * Returns whether this node has a matching for a specific revision.
	 *
	 * @param rev
	 *            revision
	 * @return true if node has a matching with revision
	 */
	public final boolean hasMatching(final Revision rev) {
		return matches != null && matches.containsKey(rev);
	}

	/**
	 * Returns whether a matching exists for a specific node.
	 *
	 * @param other
	 *            other node to search for in matches
	 * @return whether a matching exists
	 */
	@SuppressWarnings("unchecked")
	public final boolean hasMatching(final T other) {
		if (matches == null) {
			return false;
		}

		Matching<T> m = matches.get(other.getRevision());
		return m != null && m.getMatchingArtifact((T) this) == other;
	}

	/**
	 * Returns whether this artifact has unique labels.
	 *
	 * @return whether the artifact has unique labels
	 */
	public abstract boolean hasUniqueLabels();

	/**
	 * Initializes the children of the artifact.
	 */
	public abstract void initializeChildren();

	/**
	 * Returns true if the artifact is a conflict node.
	 *
	 * @return true if the artifact represents a conflict
	 */
	public final boolean isConflict() {
		return conflict;
	}

	/**
	 * Returns true if the artifact is empty.
	 *
	 * @return true if the artifact is empty
	 */
	public abstract boolean isEmpty();

	/**
	 * Returns whether the artifact is an empty dummy.
	 *
	 * @return true, if the artifact is an empty dummy.
	 */
	public final boolean isEmptyDummy() {
		return emptyDummy;
	}

	/**
	 * Returns true, if the artifact is a leaf.
	 *
	 * @return true, if the artifact is a leaf
	 */
	public abstract boolean isLeaf();

	/**
	 * @return the merged
	 */
	public final boolean isMerged() {
		return merged;
	}

	/**
	 * Returns true if the declaration order of the artifact is essential.
	 *
	 * @return true if the declaration order of the artifact is essential
	 */
	public abstract boolean isOrdered();

	/**
	 * Returns true if the artifact is the root node.
	 *
	 * @return true if the artifact is the root node
	 */
	public final boolean isRoot() {
		return getParent() == null;
	}

	/**
	 * Returns true, if the artifact matches another artifact.
	 *
	 * @param other
	 *            other artifact
	 * @return true, if the artifacts match
	 */
	public abstract boolean matches(final T other);

	/**
	 * Returns true if matches were previously computed.
	 *
	 * @return true if matches were already computed
	 */
	public final boolean matchingComputed() {
		return matches != null;
	}

	/**
	 * Performs a merge on the provided merge triple.
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
	 * Sets the children of the artifact.
	 *
	 * @param children
	 *            the new children to set
	 */
	public final void setChildren(final ArtifactList<T> children) {
		this.children = children;
	}

	/**
	 * Marks this artifact as a conflict.
	 *
	 * @param left
	 *            left alternative
	 * @param right
	 *            right alternative
	 */
	public final void setConflict(final T left, final T right) {
		this.conflict = true;
		this.left = left;
		this.right = right;
	}

	/**
	 * Set whether the artifact is an empty dummy.
	 *
	 * @param emptyDummy
	 *            true, if the artifact is an emptyDummy
	 */
	public final void setEmptyDummy(final boolean emptyDummy) {
		this.emptyDummy = emptyDummy;
	}

	/**
	 * @param merged
	 *            the merged to set
	 */
	public final void setMerged(final boolean merged) {
		this.merged = merged;
	}

	/**
	 * @param number
	 *            the number to set
	 */
	public final void setNumber(final int number) {
		this.number = number;
	}

	/**
	 * Sets the parent artifact.
	 *
	 * @param parent
	 *            the parent to set
	 */
	public final void setParent(final T parent) {
		this.parent = parent;
	}

	/**
	 * Sets the revision.
	 *
	 * @param revision
	 *            the revision to set
	 */
	public final void setRevision(final Revision revision) {
		this.revision = revision;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();

	/**
	 * Read from a BufferedReader and writes to the artifact.
	 *
	 * @param str
	 *            String to write x
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void write(String str) throws IOException;
}
