/**
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.stats.StatisticsInterface;
import de.fosd.jdime.strdump.DumpMode;

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
    protected Map<Revision, Matching<T>> matches;

    /**
     * Whether the artifact has been already merged.
     */
    private boolean merged;

    /**
     * Parent artifact.
     */
    private T parent;

    /**
     * Revision the artifact belongs to.
     */
    private Revision revision;

    /**
     * Number used to identify the artifact.
     */
    private int number;

    /**
     * Constructs a new <code>Artifact</code>.
     *
     * @param rev
     *         the <code>Revision</code> for the <code>Artifact</code>
     * @param number
     *         the DFS index of the <code>Artifact</code> in the <code>Artifact</code> tree it is a part of
     */
    protected Artifact(Revision rev, int number) {
        this.matches = new LinkedHashMap<>();
        this.revision = rev;
        this.number = number;
    }

    /**
     * Adds a child.
     *
     * @param child
     *            child to add
     * @return added child
     */
    public abstract T addChild(T child);

    /**
     * Adds a matching.
     *
     * @param matching
     *         matching to be added
     */
    public void addMatching(Matching<T> matching) {
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
     */
    public abstract T createChoiceArtifact(String condition, T artifact);

    /**
     * Returns an empty <code>Artifact</code>. This is used while performing two-way merges where the
     * base <code>Artifact</code> is empty.
     *
     * @param revision
     *         the <code>Revision</code> for the artifact
     * @return an empty artifact
     */
    public abstract T createEmptyArtifact(Revision revision);

    /**
     * Pretty-prints the <code>Artifact</code> to source code.
     *
     * @return Pretty-printed AST (source code)
     */
    public abstract String prettyPrint();

    /**
     * Dumps this <code>Artifact</code> to a <code>String</code> using the given <code>DumpMode</code>. Uses the
     * {@link Artifact#toString()} method for producing labels for nodes.
     *
     * @param mode
     *         the <code>DumpMode</code> to use
     * @return the dump result
     */
    public String dump(DumpMode mode) {
        return dump(mode, Artifact::toString);
    }

    /**
     * Dumps this <code>Artifact</code> to a <code>String</code> using the given <code>DumpMode</code>.
     *
     * @param mode
     *         the <code>DumpMode</code> to use
     * @param getLabel
     *         the <code>Function</code> for producing labels for nodes
     * @return the dump result
     */
    public String dump(DumpMode mode, Function<Artifact<T>, String> getLabel) {
        return mode.getDumper().dump(this, getLabel);
    }

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

    public abstract void deleteChildren();

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
        return matches.get(rev);
    }

    /**
     * Returns all <code>Matching</code>s added for this <code>Artifact</code>.
     *
     * @return the <code>Matching</code>s
     */
    public Set<Matching<T>> getMatchings() {
        return new HashSet<>(matches.values());
    }

    /**
     * Returns an unmodifiable view of the map used to store the <code>Matchings</code> of this <code>Artifact</code>.
     *
     * @return the matchings
     */
    public Map<Revision, Matching<T>> getMatches() {
        return Collections.unmodifiableMap(matches);
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
     * Sets the number of all <code>Artifact</code>s contained in the same tree as this <code>Artifact</code> to their
     * index in a DFS traversal of the tree.
     */
    public void renumberTree() {
        findRoot().renumber(new AtomicInteger()::getAndIncrement);
    }

    /**
     * Sets the number of this <code>Artifact</code> to the first <code>Integer</code> supplied by <code>number</code>
     * and then calls this method for all children with the given <code>number</code>.
     *
     * @param number
     *         the supplier for the new numbers
     */
    private void renumber(Supplier<Integer> number) {
        this.number = number.get();

        for (Artifact<T> child : children) {
            child.renumber(number);
        }
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
        // FIXME: this method does currently not detect deletions as changes.

        boolean hasChanges = !hasMatches();

        for (int i = 0; !hasChanges && i < getNumChildren(); i++) {
            hasChanges = getChild(i).hasChanges();
        }

        return hasChanges;
    }

    /**
     * Returns whether the <code>Artifact</code> or its subtree has changes compared to <code>Revision</code> revision.
     *
     * @param revision <Code>Revision</Code> to compare to
     * @return whether the <code>Artifact</code> or its subtree has changes compared to <code>Revision</code> revision
     */
    public boolean hasChanges(Revision revision) {

        boolean hasChanges = !hasMatching(revision);

        if (!hasChanges) {
            T baseArtifact = getMatching(revision).getMatchingArtifact(this);
            hasChanges = baseArtifact.hasChanges();
        }

        for (int i = 0; !hasChanges && i < getNumChildren(); i++) {
            hasChanges = getChild(i).hasChanges(revision);
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

    /**
     * Returns whether this <code>Artifact</code> has any matches.
     *
     * @return true if the <code>Artifact</code> has matches
     */
    public boolean hasMatches() {
        return !matches.isEmpty();
    }

    /**
     * Returns whether this <code>Artifact</code> has a <code>Matching</code> for a specific <code>Revision</code>.
     *
     * @param rev
     *            <code>Revision</code>
     * @return true if <code>Artifact</code> has a <code>Matching</code> with <code>Revision</code>
     */
    public final boolean hasMatching(Revision rev) {
        boolean hasMatching = matches.containsKey(rev);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getId() + ".hasMatching(" + rev + ")");
            if (!matches.isEmpty()) {
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
        boolean hasMatching = matches.containsKey(otherRev) && matches.get(otherRev).getMatchingArtifact(this) == other;

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(getId() + ".hasMatching(" + other.getId() + ")");
            if (!matches.isEmpty()) {
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
     * Returns a <code>Supplier</code> producing a unique label for this <code>Artifact</code> or an empty optional
     * if there is no such label. If there is a unique label a more efficient <code>UnorderedMatcher</code> can be used.
     *
     * @return optionally a <code>Supplier</code> producing a unique label
     */
    public abstract Optional<Supplier<String>> getUniqueLabel();

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
     * Returns the root node of the tree this <code>Artifact</code> is part of.
     *
     * @return the root node
     */
    public Artifact<T> findRoot() {
        Artifact<T> current = this;

        while (!current.isRoot()) {
            current = current.getParent();
        }

        return current;
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
     * Performs a merge on the provided merge triple.
     * This method selects the <code>MergeStrategy</code> and triggers the merge.
     *
     * @param operation
     *            merge operation
     * @param context
     *            merge context
     */
    public abstract void merge(MergeOperation<T> operation, MergeContext context);

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
     * Returns the left alternative of a conflict.
     *
     * @return the left <code>Artifact</code>
     */
    public T getLeft() {
        return left;
    }

    /**
     * Returns the right alternative of a conflict.
     *
     * @return the right <code>Artifact</code>
     */
    public T getRight() {
        return right;
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

    @Override
    public final int compareTo(T o) {
        return getId().compareTo(o.getId());
    }

    /**
     * If the artifact is a choice node, it has variants (values of map) that are present under conditions (keys of map)
     */
    public HashMap<String, T> getVariants() {
        return variants;
    }

    /**
     * Attempts to find an <code>Artifact</code> with the given number in the <code>Artifact</code> tree with this
     * <code>Artifact</code> at its root.
     *
     * @param number
     *         the number of the <code>Artifact</code> to find
     * @return optionally the <code>Artifact</code> with the sought number
     */
    public Optional<Artifact<T>> find(int number) {
        if (this.number == number) {
            return Optional.of(this);
        }

        return children.stream().map(c -> c.find(number)).filter(Optional::isPresent).findFirst().map(Optional::get);
    }
}
