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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.strategy.StatisticsInterface;

/**
 * A generic <code>Artifact</code> that has a tree structure.
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public abstract class Artifact<T extends Artifact<T>> implements Comparable<T>, StatisticsInterface {

    private static final Logger LOG = Logger.getLogger(Artifact.class.getCanonicalName());

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
     * Whether this artifact represents a choice node.
     */
    private boolean choice = false;

    /**
     * If the artifact is a choice node, it has variants (values of map) that are present under conditions (keys of map)
     */
    protected HashMap<String, T> variants;

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

    protected static int virtualcount = 1;

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
     *         matching to be added
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

    public abstract T clone();

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
     * Returns a choice artifact.
     *
     * @param condition presence condition
     * @param artifact conditional artifact
     * @return choice artifact
     * @throws IOException If a file is not found
     */
    public abstract T createChoiceArtifact(final String condition, final T artifact) throws IOException;

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

        if (isConflict() || isChoice()) {
            // insert virtual node
            String virtualId = "\"c" + virtualcount + "\"";
            String virtualLabel = isConflict() ? "\"Conflict\"" : "\"Choice\"";
            String virtualColor = isConflict() ? "red" : "blue";
            sb.append(virtualId);
            sb.append("[label=").append(virtualLabel);
            sb.append(", fillcolor = ").append(virtualColor);
            sb.append(", style = filled]").append(System.lineSeparator());

            if (isConflict()) {
                // left alternative
                sb.append(left.dumpGraphvizTree(includeNumbers, virtualcount));
                sb.append(virtualId).append("->").append(getGraphvizId(left)).
                        append("[label=\"").append(left.getRevision()).append("\"]").append(";").append(System.lineSeparator());

                // right alternative
                sb.append(right.dumpGraphvizTree(includeNumbers, virtualcount));
                sb.append(virtualId).append("->").append(getGraphvizId(right)).
                        append("[label=\"").append(right.getRevision()).append("\"]").append(";").append(System.lineSeparator());
            } else {
                // choice node
                for (String condition : getVariants().keySet()) {
                    Artifact<T> variant = getVariants().get(condition);
                    sb.append(variant.dumpGraphvizTree(includeNumbers, virtualcount));
                    sb.append(virtualId).append("->").append(getGraphvizId(variant)).
                            append("[label=\"").append(condition).append("\"]").append(";").append(System.lineSeparator());
                }
            }
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
                if (child.isConflict() || child.isChoice()) {
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
     *             position of child <code>Artifact</code>
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
     * @return the parent <code>Artifact</code>
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
     * Returns the maximum depth of any node in the tree.
     *
     * @return the maximum depth
     */
    public int getMaxDepth() {
        return 1 + children.parallelStream().map(T::getMaxDepth).max(Integer::compare).orElse(0);
    }

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

        Artifact<?> artifact = (Artifact<?>) o;

        return Objects.equals(getId(), artifact.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
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
    public final boolean hasMatching(Revision rev) {
        boolean hasMatching = matches != null && matches.containsKey(rev);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getId() + ".hasMatching(" + rev + ")");
            if (matches != null) {
                for (Revision r : matches.keySet()) {
                    LOG.finest("Matching found with: " + r + " (" + matches.get(r).getMatchingArtifact(this).getId() + ")");
                    LOG.finest("hasMatching(" + r + ") = " + hasMatching);
                }
            } else {
                LOG.finest("no matches for " + getId() + " and " + rev);
            }
        }

        if (!hasMatching && isChoice()) {
            // choice nodes have to be treated specially ...
            for (T variant: variants.values()) {
                if (variant.hasMatching(rev)) {
                    hasMatching = true;
                    break;
                }
            }
        }

        return hasMatching;
    }

    /**
     * Returns whether a <code>Matching</code> exists for a specific <code>Artifact</code>.
     *
     * @param other
     *            other <code>Artifact</code> to search <code>Matching</code>s for
     * @return whether a <code>Matching</code> exists
     */
    public final boolean hasMatching(T other) {
        Revision otherRev = other.getRevision();
        boolean hasMatching = matches != null && matches.containsKey(otherRev) && matches.get(otherRev).getMatchingArtifact(this) == other;

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getId() + ".hasMatching(" + other.getId() + ")");
            if (matches != null) {
                for (Revision r : matches.keySet()) {
                    LOG.finest("Matching found with: " + r + " (" + other.getId() + ")");
                    LOG.finest("hasMatching(" + r + ") = " + hasMatching);
                }
            } else {
                LOG.finest("no matches for " + getId() + " and " + other.getId());
            }
        }

        if (!hasMatching && isChoice()) {
            // choice nodes have to be treated specially ...
            for (T variant: variants.values()) {
                if (variant.hasMatching(otherRev) && matches.get(otherRev).getMatchingArtifact(variant) == other) {
                    hasMatching = true;
                    break;
                }
            }
        }
        return hasMatching;
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
     * Returns true if the artifact is a choice node.
     *
     * @return true if the artifact represents a choice node
     */
    public final boolean isChoice() {
        return choice;
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
    public boolean matchingComputed(Revision rev) {
        return matches != null && hasMatching(rev);
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
     * Marks this artifact as a choice.
     *
     * @param condition presence condition
     * @param artifact conditional artifact
     */
    public final void setChoice(final String condition, final T artifact) {
        this.choice = true;
        if (condition == null) {
            throw new RuntimeException("condition must not be null!");
        }
        addVariant(condition, artifact);
    }

    public void addVariant(String condition, final T artifact) {
        if (!choice) {
            throw new RuntimeException("addVariant() can only be called on choice nodes!");
        }
        if (condition == null) {
            throw new RuntimeException("condition must not be null!");
        }

        LOG.fine("Add node " + artifact.getId() + " under condition " + condition);

        if (variants == null) {
            variants = new HashMap<>();
        }

        // merge conditions for same artifact
        List<String> mergedConditions = new ArrayList<>();
        for (String existingCondition : variants.keySet()) {
            if (variants.get(existingCondition).equals(artifact)) {
                mergedConditions.add(existingCondition);
                condition = existingCondition + " || " + condition;
            }
        }
        for (String mergedCondition : mergedConditions) {
            variants.remove(mergedCondition);
        }

        variants.put(condition, artifact);
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
                child.setRevision(revision, true);
            }
        }
    }

    @Override
    public abstract String toString();

    /**
     * If the artifact is a choice node, it has variants (values of map) that are present under conditions (keys of map)
     */
    public HashMap<String, T> getVariants() {
        return variants;
    }
}
