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
package de.fosd.jdime.artifact;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.StatisticsInterface;
import de.fosd.jdime.strdump.DumpMode;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

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
    private List<T> children;

    /**
     * Left side of a conflict.
     */
    protected T left;

    /**
     * Right side of a conflict.
     */
    protected T right;

    /**
     * Whether this artifact represents a conflict.
     */
    private boolean conflict;

    /**
     * Whether this artifact represents a choice node.
     */
    private boolean choice;

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

    private boolean hashValid;
    private String hash;

    /**
     * Constructs a new <code>Artifact</code>.
     *
     * @param rev
     *         the <code>Revision</code> for the <code>Artifact</code>
     * @param number
     *         the DFS index of the <code>Artifact</code> in the <code>Artifact</code> tree it is a part of
     */
    protected Artifact(Revision rev, int number) {
        this.children = new ArtifactList<>();
        this.matches = new HashMap<>();
        this.revision = rev;
        this.number = number;
        this.hashValid = false;
        this.hash = null;
    }

    /**
     * Copies the given {@link Artifact} detached from its tree.
     *
     * @param toCopy
     *         the {@link Artifact} to copy
     * @see #copy()
     */
    protected Artifact(Artifact<T> toCopy) {
        this.children = new ArtifactList<>();
        this.left = toCopy.left != null ? Artifacts.copyTree(toCopy.left) : null;
        this.right = toCopy.right != null ? Artifacts.copyTree(toCopy.right) : null;

        if (toCopy.variants != null) {
            this.variants = new HashMap<>();
            toCopy.variants.entrySet().forEach(en -> variants.put(en.getKey(), Artifacts.copyTree(en.getValue())));
        }

        copyMatches(toCopy);

        this.conflict = toCopy.conflict;
        this.choice = toCopy.choice;
        this.merged = toCopy.merged;
        this.revision = toCopy.revision;
        this.number = toCopy.number;
    }

    /**
     * Must be implemented as:
     * <p>
     * {@code
     * protected T self() {
     *     return this;
     * }
     * }
     * <p>
     * This method solves some issues caused by the recursive generic type signature of the {@link Artifact} class.
     *
     * @return {@code this}
     */
    protected abstract T self();

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
     * Copies this {@link Artifact} detached from its tree (meaning {@link #parent} will be {@code null} and
     * {@link Artifact#children} will be empty. Subclasses must implement this method by using their own private
     * copy constructor that calls the protected copy constructor of the {@link Artifact} base class.
     *
     * @return a copy of this {@link Artifact}
     * @see Artifacts#copyTree(Artifact)
     */
    public abstract T copy();

    /**
     * Copies the {@link Artifact#matches} of {@code toCopy}, replaces {@code toCopy} with {@code this} in them and
     * adds them to {@code this} {@link Artifact}.
     *
     * @param toCopy
     *         the {@link Artifact} to copy the {@link Artifact#matches} from
     */
    public void copyMatches(Artifact<T> toCopy) {

        if (toCopy.matches == null) {
            return;
        }

        this.matches = new HashMap<>();

        toCopy.matches.entrySet().forEach(en -> {
            Matching<T> clone = en.getValue().clone();
            clone.updateMatching(self(), toCopy.self());

            matches.put(en.getKey(), clone);
        });
    }

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
     * Adds the given {@link Artifact} to the children of this {@link Artifact} and sets the {@link #parent}
     * accordingly.
     *
     * @param child
     *         the {@link Artifact} to add as a child
     */
    public void addChild(T child) {

        if (canAddChild(child)) {
            children.add(child);
            child.setParent(self());
            invalidateHash();
        }
    }

    /**
     * Replaces the child at the specified position in the list of children of this {@link Artifact} with the specified
     * child.
     *
     * @param child
     *         the child to be added at the specified position
     * @param index
     *         the index of the child to replace
     * @see List#set(int, Object)
     */
    public void setChild(T child, int index) {

        if (canAddChild(child)) {
            children.set(index, child);
            child.setParent(self());
            invalidateHash();
        }
    }

    /**
     * Determines whether the given {@link Artifact} {@code toAdd} may be added to the children of this
     * {@link Artifact}. Any child passed to {@link #addChild(Artifact)} will not be added if this method returns
     * {@code false} for it. Subclasses overriding this method should log the reason for returning {@code false} if they
     * do so. The default implementation returns {@code true}.
     *
     * @param toAdd
     *         the {@link Artifact} to add
     * @return whether the {@code child} may be added
     */
    protected boolean canAddChild(T toAdd) {
        return true;
    }

    /**
     * Removes all children of this {@link Artifact}.
     */
    public void clearChildren() {
        if (hasChildren()) {
            children.clear();
            invalidateHash();
        }
    }

    /**
     * Returns the index of the first occurrence of the specified child in the list of children, or -1 if the given
     * {@link Artifact} is not a child of this {@link Artifact}.
     *
     * @param child
     *         the child whose index is to be returned
     * @return the index of the child or -1
     * @see List#indexOf(Object)
     */
    public int indexOf(T child) {
        return children.indexOf(child);
    }

    /**
     * Return child <code>Artifact</code> at position i.
     *
     * @param i
     *             position of child <code>Artifact</code>
     * @return child <code>Artifact</code> at position i
     */
    public T getChild(int i) {
        return children.get(i);
    }

    /**
     * Returns an unmodifiable view of the children of this {@code Artifact}.
     *
     * @return an unmodifiable view of the children of this {@code Artifact}
     * @see Collections#unmodifiableList(List)
     */
    public List<T> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Sets the children of the <code>Artifact</code>.
     *
     * @param children
     *         the new children to set
     * @throws NullPointerException
     *         if {@code children} is {@code null}
     */
    public void setChildren(List<T> children) {
        Objects.requireNonNull(children, "The list of children must not be null.");

        this.children = children;
        invalidateHash();
    }

    /**
     * Applies the given {@code action} to the children of this {@link Artifact} and invalidates the tree hash as
     * necessary.
     *
     * @param action
     *         the action to apply to the list of {@link #children}
     */
    protected void modifyChildren(Consumer<List<T>> action) {
        int hashBefore = children.hashCode();
        action.accept(children);

        if (children.hashCode() != hashBefore) {
            invalidateHash();
        }
    }

    /**
     * Returns the number of children the <code>Artifact</code> has.
     *
     * @return number of children
     */
    public int getNumChildren() {
        return children.size();
    }

    /**
     * Returns true if the <code>Artifact</code> has children.
     *
     * @return true if the <code>Artifact</code> has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
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
     * Returns a hash of the tree rooted in this {@code Artifact}.
     *
     * @return the tree hash
     */
    public String getTreeHash() {

        if (hashValid) {
            return hash;
        }

        MessageDigest digest = DigestUtils.getSha256Digest();
        DigestUtils.updateDigest(digest, hashId());

        if (hasChildren()) {
            children.forEach(c -> DigestUtils.updateDigest(digest, c.getTreeHash()));
            hash = "1" + Hex.encodeHexString(digest.digest());
        } else {
            hash = "0" + Hex.encodeHexString(digest.digest());
        }

        hashValid = true;
        return hash;
    }

    /**
     * Returns the {@code String} identifying this {@code Artifact} for the purposes of calculating the tree hash in
     * {@link #getTreeHash()};
     *
     * @return the identifying {@code String} to be hashed
     */
    protected abstract String hashId();

    /**
     * Invalidates the hashes of this {@code Artifact} and all its parents.
     */
    protected void invalidateHash() {
        hashValid = false;
        hash = null;

        if (parent != null) {
            parent.invalidateHash();
        }
    }

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
     * Sets the number of all <code>Artifact</code>s contained in the tree rooted at this artifact to their index in
     * a depth-first traversal of the tree.
     */
    public void renumber() {
        renumber(new AtomicInteger()::getAndIncrement);
    }

    /**
     * Sets the number of all <code>Artifact</code>s contained in the tree rooted at this artifact to the number
     * supplied by <code>number</code> when traversing the tree in depth-first order.
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
     * Returns whether the subtree rooted in this {@link Artifact} has changes compared to the given {@link Revision}.
     * Returns {@code false} if {@code revision} is the {@link Revision} of this {@link Artifact}.
     *
     * @param revision the opposite {@link Revision}
     * @return true iff any {@link Artifact} in the tree under this {@link Artifact} represents a changed compared to
     *         the given {@link Revision}
     */
    public boolean hasChanges(Revision revision) {

        if (this.revision.equals(revision)) {
            return false;
        }

        if (!hasMatching(revision)) {
            return true;
        }

        T match = getMatching(revision).getMatchingArtifact(this);

        return getTreeSize() != match.getTreeSize() || Artifacts.bfsStream(self()).anyMatch(a -> {
            // We use Artifact#hashId here since it is implemented for SemiStructuredArtifacts using the pretty printed content.
            // This ensures that matched SemiStructuredArtifacts are detected as changes if their contents do not match.
            return !a.hasMatching(revision) || !a.getMatching(revision).getMatchingArtifact(a).hashId().equals(a.hashId());
        });
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
     * Returns whether this {@link Artifact} has been matched with an {@link Artifact} from the given {@link Revision}.
     *
     * @param rev
     *         the opposite {@link Revision}
     * @return true iff there is a match from the given {@code revision}
     */
    public boolean hasMatching(Revision rev) {
        logMatchings(rev);

        if (isChoice()) {
            return variants.entrySet().stream().map(Entry::getValue).anyMatch(var -> var.hasMatching(rev));
        } else {
            return matches.containsKey(rev);
        }
    }

    /**
     * Returns whether this {@link Artifact} has been matched with the given {@link Artifact} {@code other}.
     *
     * @param other
     *         the opposite {@link Artifact}
     * @return true iff this {@link Artifact} has been matched with the given {@link Artifact} {@code other}
     */
    public boolean hasMatching(T other) {
        Revision otherRev = other.getRevision();
        logMatchings(otherRev);

        if (isChoice()) {
            return variants.entrySet().stream().map(Entry::getValue).anyMatch(var -> var.hasMatching(other));
        } else {
            return matches.containsKey(otherRev) && matches.get(otherRev).getMatchingArtifact(this) == other;
        }
    }

    /**
     * Logs (if FINEST is enabled) what matchings exist for this {@link Artifact} in the given {@link Revision}.
     *
     * @param rev
     *         the opposite {@link Revision}
     */
    private void logMatchings(Revision rev) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Checking for matchings for " + getId() + " in revision " + rev + ".");

            if (matches.isEmpty()) {
                LOG.finest("No matchings for " + getId() + " in revision " + rev + ".");
            } else {

                for (Entry<Revision, Matching<T>> entry : matches.entrySet()) {
                    Revision otherRev = entry.getKey();
                    T matchedArtifact = entry.getValue().getMatchingArtifact(this);
                    LOG.finest("Matching found for revision " + otherRev + " is " + matchedArtifact.getId());
                }
            }
        }
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
     * Returns whether this {@link Artifact} is a virtual one.
     *
     * @return true iff the {@link Artifact} is virtual
     */
    public final boolean isVirtual() {
        return isConflict() || isChoice();
    }

    /**
     * Returns true if the <code>Artifact</code> is empty.
     *
     * @return true if the <code>Artifact</code> is empty
     */
    public abstract boolean isEmpty();

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
     * Returns true, if this <code>Artifact</code> is assigned the same
     * category as the other <code>Artifact</code>.
     *
     * @param other
     *            other <code>Artifact</code>
     * @return true, if the <code>Artifact</code>s' categories match
     */
    public abstract boolean categoryMatches(T other);

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
     * Marks this <code>Artifact</code> as a conflict.
     *
     * @param left
     *            left alternative
     * @param right
     *            right alternative
     */
    protected void setConflict(T left, T right) {
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
    protected void setParent(T parent) {
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

        if (recursive) {
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
     *
     * @return the variants represented by this choice {@link Artifact}
     */
    public Map<String, T> getVariants() {
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
