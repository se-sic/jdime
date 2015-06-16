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

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
	public final void cloneMatches(final T other) {

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

	/**
	 * Copies the <code>Artifact</code> into an existing <code>Artifact</code>.
	 *
	 * @param destination
	 *            destination artifact
	 * @throws IOException
	 *             If an input or output exception occurs.
	 */
	public abstract void copyArtifact(final T destination) throws IOException;

	/**
	 * Creates a new <code>Artifact</code>,
	 *
	 * @param isLeaf
	 *            if true, a leaf type artifact will be created
	 * @throws IOException
	 *             If an input output exception occurs
	 */
	public abstract void createArtifact(boolean isLeaf) throws IOException;

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
	 * @throws FileNotFoundException
	 *             If a file is not found
	 */
	public abstract T createConflictArtifact(final T left, final T right) throws FileNotFoundException;

	/**
	 * Returns an empty <code>Artifact</code>. This is used while performing two-way merges where the
	 * base <code>Artifact</code> is empty.
	 *
	 * @return empty <code>Artifact</code>
	 * @throws FileNotFoundException
	 *             If a file is not found
	 */
	public abstract T createEmptyArtifact() throws FileNotFoundException;

	/**
	 * Finds the root artifact and calls <code>dumpTree()</code> on it.
	 *
	 * This method is used for debugging JDime.
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
	 * This method is used for debugging JDime.
	 *
	 * @return artifact structure as indented plain text
	 */
	public final String dumpTree() {
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
	 * Return child <code>Artifact</code> at position i.
	 *
	 * @param i
	 * 			position of child <code>Artifact</code>
	 * @return child <code>Artifact</code> at position i
	 */
	public final T getChild(final int i) {
		assert (children != null);
		return children.get(i);
	}

	/**
	 * Returns all children of the <code>Artifact</code>.
	 *
	 * @return the children of the <code>Artifact</code>
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
	public final Matching<T> getMatching(final Revision rev) {
		return matches == null ? null : matches.get(rev);
	}

	/**
	 * Returns the number of the <code>Artifact</code>.
	 *
	 * @return number of the <code>Artifact</code>
	 */
	public final int getNumber() {
		return number;
	}

	/**
	 * Returns the number of children the <code>Artifact</code> has.
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
	 * Returns the parent <code>Artifact</code>.
	 *
	 * @return the parent <code>Artifact></code>
	 */
	public final T getParent() {
		return parent;
	}

	/**
	 * Returns the <code>Revision</code> the <code>Artifact</code> belongs to.
	 *
	 * @return the <code>Revision</code> the <code>Artifact</code> belongs to.
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
	 * Returns the size of the subtree. The <code>Artifact</code> itself is not included.
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
	 * Returns the size of the tree. The <code>Artifact</code> itself is also included.
	 *
	 * @return size of tree
	 */
	public final int getTreeSize() {
		return getSubtreeSize() + 1;
	}

	/**
	 * Returns whether the <code>Artifact</code> or its subtree has changes.
	 *
	 * @return whether the <code>Artifact</code> or its subtree has changes
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
	 *            If true, the whole subtree is checked.
	 *            If false, only the <code>Artifact</code> itself and its direct children are checked.
	 * @return whether the <code>Artifact</code> has changes
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
	 * Returns true if the <code>Artifact</code> has children.
	 *
	 * @return true if the <code>Artifact</code> has children
	 */
	public final boolean hasChildren() {
		return getNumChildren() > 0;
	}

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	/**
	 * Returns whether this <code>Artifact</code> has any matches.
	 *
	 * @return true if the <code>Artifact</code> has matches
	 */
	public final boolean hasMatches() {
		return matches != null && !matches.isEmpty();
	}

	/**
	 * Returns whether this <code>Artifact</code> has a <code>Matching</code> for a specific <code>Revision</code>.
	 *
	 * @param rev
	 *            <code>Revision</code>
	 * @return true if <code>Artifact</code> has a <code>Matching</code> with <code>Revision</code>
	 */
	public final boolean hasMatching(final Revision rev) {
		return matches != null && matches.containsKey(rev);
	}

	/**
	 * Returns whether a <code>Matching</code> exists for a specific <code>Artifact</code>.
	 *
	 * @param other
	 *            other <code>Artifact</code> to search <code>Matching</code>s for
	 * @return whether a <code>Matching</code> exists
	 */
	public final boolean hasMatching(final T other) {

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
	 * Initializes the children of the artifact.
	 *
	 * FIXME: can this somehow be done in the constructors so we can get rid of this method?
	 */
	public abstract void initializeChildren();

	/**
	 * Returns true if the <code>Artifact</code> is a conflict node.
	 *
	 * @return true if the <code>Artifact</code> represents a conflict
	 */
	public final boolean isConflict() {
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
	public final boolean isMerged() {
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
	public final boolean isRoot() {
		return getParent() == null;
	}

	/**
	 * Returns true, if this <code>Artifact</code> matches another <code>Artifact</code>.
	 *
	 * @param other
	 *            other <code>Artifact</code>
	 * @return true, if the <code>Artifact</code>s match
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
	public final void setChildren(final ArtifactList<T> children) {
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
	protected final void setConflict(final T left, final T right) {
		this.conflict = true;
		this.left = left;
		this.right = right;
	}

	/**
	 * Set whether the <code>Artifact</code> has already been merged.
	 * @param merged
	 *            whether the <code>Artifact</code> has already been merged
	 */
	public final void setMerged(final boolean merged) {
		this.merged = merged;
	}

	/**
	 * Sets the number of the <code>Artifact</code>
	 * @param number
	 *            the number to set
	 */
	public final void setNumber(final int number) {
		this.number = number;
	}

	/**
	 * Sets the parent <code>Artifact</code>.
	 *
	 * @param parent
	 *            the parent to set
	 */
	public final void setParent(final T parent) {
		this.parent = parent;
	}

	/**
	 * Sets the <code>Revision</code>.
	 *
	 * @param revision
	 *            the <code>Revision</code> to set
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
}
