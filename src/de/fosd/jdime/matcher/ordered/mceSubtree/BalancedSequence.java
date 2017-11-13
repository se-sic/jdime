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
package de.fosd.jdime.matcher.ordered.mceSubtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.util.Tuple;

/**
 * Trees can be described as balanced sequences. A balanced sequence is a sequence of even length over the alphabet
 * {0, 1}. The balanced sequence of a leaf node is the empty sequence. The balanced sequence of a non leaf node is the
 * concatenation of the balanced sequences of its children, every one preceded by a 0 and followed by a 1. The balanced
 * sequence of a tree is the balanced sequence of its root node. This implementation adds the root node to the
 * balanced sequence (as if by adding a virtual root node with a single child (the root node) and constructing the
 * normal balanced sequence of the tree).
 *
 * @param <T>
 *         the type of the <code>Artifact</code> whose balanced sequence is to be constructed
 *
 * @author Georg Seibt
 * @see <a href="http://www.cs.upc.edu/~antoni/subtree.pdf">This Paper</a>
 */
public class BalancedSequence<T extends Artifact<T>> {

    @SuppressWarnings("unchecked")
    private static final BalancedSequence<?> EMPTY_SEQ = new BalancedSequence<>(Collections.EMPTY_LIST);

    private List<T> seq;
    private int hashCode;

    private Map<BalancedSequence<T>, Set<BalancedSequence<T>>> decompositionCache;

    /**
     * Constructs a new <code>BalancedSequence</code> representing the given <code>tree</code> structure.
     *
     * @param tree
     *         the tree of <code>Artifact</code>s
     */
    public BalancedSequence(T tree) {
        this.seq = new ArrayList<>(Collections.nCopies(tree.getTreeSize() * 2, null));
        initSeq(tree, Integer.MAX_VALUE);
    }

    /**
     * Constructs a new <code>BalancedSequence</code> representing the given <code>tree</code> structure.
     * All nodes with depth <code>maxDepth</code> will be considered leaf nodes.
     *
     * @param tree
     *         the tree of <code>Artifact</code>s
     * @param maxDepth
     *         the maximum depth of nodes to consider
     */
    public BalancedSequence(T tree, int maxDepth) {
        this.seq = new ArrayList<>(Collections.nCopies(getSize(tree, maxDepth) * 2, null));
        initSeq(tree, maxDepth);
    }

    /**
     * Constructs a new <code>BalancedSequence</code> wrapping the given <code>seq</code>.
     *
     * @param seq
     *         the sequence to wrap
     */
    private BalancedSequence(List<T> seq) {
        this.seq = seq;
        this.hashCode = seq.hashCode();
    }

    /**
     * Returns the number of nodes in the tree that have at most the given <code>depth</code>.
     *
     * @param tree
     *         the tree whose nodes are to be counted
     * @param depth
     *         the maximum depth of nodes to count
     *
     * @return the number of nodes
     */
    private int getSize(T tree, int depth) {

        if (depth == 0) {
            return 1;
        }

        int num = 0;
        for (T t : tree.getChildren()) {
            num += getSize(t, depth - 1);
        }

        return num + 1;
    }

    /**
     * Initializes the <code>seq</code> array to the balanced sequence of the <code>tree</code>.
     *
     * @param tree
     *         the tree whose balanced sequence is to be inserted in the <code>seq</code> array
     * @param maxDepth
     *         the maximum depth of nodes to add
     */
    private void initSeq(T tree, int maxDepth) {
        seq.set(0, tree);
        initSeqRec(tree, 1, 0, maxDepth);
        hashCode = seq.hashCode();
    }

    /**
     * Initializes the <code>seq</code> array to the balanced sequence of the <code>tree</code>.
     *
     * @param tree
     *         the tree whose balanced sequence is to be inserted in the <code>seq</code> array
     * @param index
     *         the index for the 0 before the first child
     * @param currentDepth
     *         the current depth in the tree
     * @param maxDepth
     *         the maximum depth of nodes to add
     *
     * @return the index after the last index written to;
     */
    private int initSeqRec(T tree, int index, int currentDepth, int maxDepth) {

        if (currentDepth < maxDepth) {
            for (T t : tree.getChildren()) {
                seq.set(index++, t);
                index = initSeqRec(t, index, currentDepth + 1, maxDepth);
                index++;
            }
        }

        return index;
    }

    /**
     * Partitions the balanced sequence into its head and tail. The head and tail of a balanced sequence <code>s</code>
     * are unique balanced sequences such that <code>s = 0 head(s) 1 tail(s)</code>.
     *
     * @return a <code>Pair</code> of (<code>head(s), tail(s)</code>)
     */
    public Tuple<BalancedSequence<T>, BalancedSequence<T>> partition() {

        if (seq.size() == 0 || seq.size() == 2) {
            return Tuple.of(emptySeq(), emptySeq());
        }

        int numZeros = 0;
        int index = 0;

        do {
            if (seq.get(index++) == null) {
                numZeros--;
            } else {
                numZeros++;
            }
        } while (numZeros > 0);

        BalancedSequence<T> head;
        BalancedSequence<T> tail;

        int headLength = index - 2;
        int tailLength = seq.size() - index;

        if (headLength != 0) {
            head = new BalancedSequence<>(seq.subList(1, 1 + headLength));
            head.setDecompositionCache(decompositionCache);
        } else {
            head = emptySeq();
        }

        if (tailLength != 0) {
            tail = new BalancedSequence<>(seq.subList(index, index + tailLength));
            tail.setDecompositionCache(decompositionCache);
        } else {
            tail = emptySeq();
        }

        return Tuple.of(head, tail);
    }

    /**
     * Returns an empty sequence.
     *
     * @param <T>
     *         the type of the <code>Artifact</code>
     * @return an empty <code>BalancedSequence</code>
     */
    @SuppressWarnings("unchecked")
    private static <T extends Artifact<T>> BalancedSequence<T> emptySeq() {
        return (BalancedSequence<T>) EMPTY_SEQ;
    }

    /**
     * An expensive part of the algorithm implemented in {@link BalancedSequence#lcs(BalancedSequence, BalancedSequence)}
     * is the decomposition of one <code>BalancedSequence</code> into a <code>Set</code> of <code>BalancedSequences</code>.
     * When performing multiple calls to {@link BalancedSequence#lcs(BalancedSequence, BalancedSequence)} for similar
     * <code>BalancedSequences</code> performance can be improved by using a persistent cache for all of them.
     * <p>
     * The given cache will be used and updated in the {@link #decompose()} method. It will also be passed to the
     * produced <code>BalancedSequences</code> in {@link #decompose()} and {@link #partition()}.
     *
     * @param decompositionCache
     *         the decomposition cache
     */
    public void setDecompositionCache(Map<BalancedSequence<T>, Set<BalancedSequence<T>>> decompositionCache) {
        this.decompositionCache = decompositionCache;
    }

    /**
     * Returns the decomposition of this balanced sequence. The decomposition of the empty balanced sequence is a set
     * containing only the empty balanced sequence. For all other sequences s the decomposition is the union of a
     * set containing s and the decompositions of head(s), tail(s) and the concatenation of head(s) and tail(s).
     *
     * @return the decomposition of this balanced sequence
     */
    public Set<BalancedSequence<T>> decompose() {

        if (isEmpty()) {
            return Collections.singleton(emptySeq());
        }

        Function<BalancedSequence<T>, Set<BalancedSequence<T>>> calcDecomp = seq -> {
            Set<BalancedSequence<T>> decomposition = new HashSet<>(Collections.singleton(seq));

            Tuple<BalancedSequence<T>, BalancedSequence<T>> partition = partition();
            BalancedSequence<T> head = partition.getX();
            BalancedSequence<T> tail = partition.getY();

            decomposition.addAll(head.decompose());
            decomposition.addAll(tail.decompose());
            decomposition.addAll(concatenate(head, tail).decompose());

            return decomposition;
        };

        if (decompositionCache != null) {
            return decompositionCache.computeIfAbsent(this, calcDecomp);
        } else {
            return calcDecomp.apply(this);
        }
    }

    /**
     * Concatenates the two given <code>BalancedSequence</code>s.
     *
     * @param left
     *         the left part of the resulting <code>BalancedSequence</code>
     * @param right
     *         the right part of the resulting <code>BalancedSequence</code>
     * @param <T>
     *         the type of the <code>Artifact</code>s
     *
     * @return the concatenation result
     */
    private static <T extends Artifact<T>> BalancedSequence<T> concatenate(BalancedSequence<T> left, BalancedSequence<T> right) {
        int length = left.seq.size() + right.seq.size();

        if (length == 0) {
            return emptySeq();
        }

        List<T> result = new ArrayList<>(length);
        result.addAll(left.seq);
        result.addAll(right.seq);

        BalancedSequence<T> res = new BalancedSequence<>(result);

        if (left.decompositionCache != null) {
            res.setDecompositionCache(left.decompositionCache);
        } else if (right.decompositionCache != null) {
            res.setDecompositionCache(right.decompositionCache);
        }

        return res;
    }

    /**
     * Returns the length (being the number of nodes of the tree it represents) of the longest common balanced sequence
     * between the balanced sequences <code>s</code> and <code>t</code>.
     *
     * @param s
     *         the first <code>BalancedSequence</code>
     * @param t
     *         the second <code>BalancedSequence</code>
     * @param <T>
     *         the type of the <code>Artifact</code>s
     *
     * @return the length of the longest common balanced sequence
     */
    public static <T extends Artifact<T>> Integer lcs(BalancedSequence<T> s, BalancedSequence<T> t) {
        Map<Integer, Integer> codes = new HashMap<>();
        Integer[][] results;
        int code = 0;

        /*
         * The decompositions of s and t contain all balanced sequences that will be produced during the recursion
         * through lcsRec. We assign each balanced sequence an index into the results array.
         */
        Set<BalancedSequence<T>> dec = new HashSet<>(s.decompose());
        dec.addAll(t.decompose());

        for (BalancedSequence<T> seq : dec) {
            codes.put(seq.hashCode(), code++);
        }

        /*
         * We build a triangular array because the lcs problem is symmetric (lcs(s, t) = lcs(t, s)).
         * The functions lookup and store are then used to correctly address sub-problems (the recursive cases in
         * lcsRec) using the codes of their balanced sequences.
         */
        results = new Integer[codes.size()][];

        for (int i = 0; i < results.length; i++) {
            results[i] = new Integer[i + 1];
        }

        return lcsRec(s, t, codes, results);
    }

    /**
     * Recursive helper function for {@link BalancedSequence#lcs(BalancedSequence, BalancedSequence)}. Computes
     * the longest common balanced sequence between the balanced sequences <code>s</code> and <code>t</code> using
     * the <code>results</code> array to store results of sub-problems. <code>codes</code> must
     * contain mappings for the hashes of every balanced sequence in the decompositions of <code>s</code> and
     * <code>t</code> to an index in the array <code>results</code>.
     *
     * @param s
     *         the first <code>BalancedSequence</code>
     * @param t
     *         the seconds <code>BalancedSequence</code>
     * @param codes
     *         the codes of the <code>BalancedSequence</code>s in the decompositions
     * @param results
     *         the array of solutions to sub-problems
     * @param <T>
     *         the type of the <code>Artifact</code>s
     *
     * @return the length of the longest common balanced sequence
     */
    private static <T extends Artifact<T>> Integer lcsRec(BalancedSequence<T> s, BalancedSequence<T> t,
            Map<Integer, Integer> codes, Integer[][] results) {

        if (s.isEmpty() || t.isEmpty()) {
            return 0;
        }

        Integer codeS = codes.get(s.hashCode());
        Integer codeT = codes.get(t.hashCode());
        Integer result = lookup(codeS, codeT, results);

        if (result != null) {
            return result;
        }

        Tuple<BalancedSequence<T>, BalancedSequence<T>> sPart = s.partition();
        Tuple<BalancedSequence<T>, BalancedSequence<T>> tPart = t.partition();
        BalancedSequence<T> sHead = sPart.getX();
        BalancedSequence<T> tHead = tPart.getX();
        BalancedSequence<T> sTail = sPart.getY();
        BalancedSequence<T> tTail = tPart.getY();

        Integer a = lcsRec(concatenate(sHead, sTail), t, codes, results);
        Integer b = lcsRec(s, concatenate(tHead, tTail), codes, results);

        if (s.seq.get(0).matches(t.seq.get(0))) {
            Integer c = lcsRec(sHead, tHead, codes, results) + lcsRec(sTail, tTail, codes, results) + 1;
            result = max(a, max(b, c));
        } else {
            result = max(a, b);
        }

        store(codeS, codeT, results, result);

        return result;
    }

    /**
     * Looks up the result of the lcs problem for the balanced sequences with the given codes in the
     * <code>results</code> array.
     *
     * @param codeA
     *         the code of the first balanced sequence
     * @param codeB
     *         the code of the second balanced sequence
     * @param results
     *         the results array
     *
     * @return the solution to the lcs problem or <code>null</code> if <code>results</code> contains none
     */
    private static Integer lookup(Integer codeA, Integer codeB, Integer[][] results) {
        return codeA.compareTo(codeB) > 0 ? results[codeA][codeB] : results[codeB][codeA];
    }

    /**
     * Stores the solution to an lcs problem between the balanced sequences with the given codes in the
     * <code>results</code> array.
     *
     * @param codeA
     *         the code of the first balanced sequence
     * @param codeB
     *         the code of the second balanced sequence
     * @param results
     *         the results array
     * @param result
     *         the result to be stored
     */
    private static void store(Integer codeA, Integer codeB, Integer[][] results, Integer result) {

        if (codeA.compareTo(codeB) > 0) {
            results[codeA][codeB] = result;
        } else {
            results[codeB][codeA] = result;
        }
    }

    /**
     * Returns the maximum of two <code>Integer</code>s.
     *
     * @param a
     *         the first <code>Integer</code>
     * @param b
     *         the second <code>Integer</code>
     *
     * @return the bigger of both <code>Integer</code>s
     */
    private static Integer max(Integer a, Integer b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    /**
     * Returns whether this <code>BalancedSequence</code> is empty.
     *
     * @return true iff the <code>BalancedSequence</code> is empty
     */
    public boolean isEmpty() {
        return seq.isEmpty();
    }

    /**
     * Returns the root of the tree of <code>Artifact</code>s this <code>BalancedSequence</code> represents.
     *
     * @return the root of the tree
     */
    public T getRoot() {
        return seq.get(0);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BalancedSequence<?> that = (BalancedSequence<?>) o;

        return seq.equals(that.seq);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return seq.stream().map(bit -> bit == null ? "1" : "0").reduce("", String::concat);
    }
}
