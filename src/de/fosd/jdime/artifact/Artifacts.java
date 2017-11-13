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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class consists of {@code static} utility methods for operating on {@code Artifact} instances.
 */
public final class Artifacts {

    /**
     * An {@code Iterator} over the elements of an {@code Artifact} tree in breadth-first order.
     *
     * @param <T>
     *         the {@code Artifact} type
     */
    private static final class BFSIterator<T extends Artifact<T>> implements Iterator<T> {

        private Deque<T> wait;

        /**
         * Constructs a new {@link BFSIterator} over the given tree.
         *
         * @param treeRoot
         *         the root of the tree to traverse
         */
        private BFSIterator(T treeRoot) {
            wait = new ArrayDeque<>();
            wait.addFirst(treeRoot);
        }

        @Override
        public boolean hasNext() {
            return !wait.isEmpty();
        }

        @Override
        public T next() {
            T next = wait.removeFirst();
            next.getChildren().forEach(wait::addLast);

            return next;
        }
    }

    /**
     * An {@code Iterator} over the elements of an {@code Artifact} tree in depth-first order.
     *
     * @param <T>
     *         the {@code Artifact} type
     */
    private static final class DFSIterator<T extends Artifact<T>> implements Iterator<T> {

        private Deque<T> wait;

        /**
         * Constructs a new {@code DFSIterator} over the given tree.
         *
         * @param treeRoot
         *         the root of the tree to traverse
         */
        private DFSIterator(T treeRoot) {
            wait = new ArrayDeque<>();
            wait.addFirst(treeRoot);
        }

        @Override
        public boolean hasNext() {
            return !wait.isEmpty();
        }

        @Override
        public T next() {
            T next = wait.removeFirst();

            if (next.getNumChildren() == 1) {
                wait.addFirst(next.getChild(0));
            } else if (next.getNumChildren() > 1) {
                ArrayList<T> ch = new ArrayList<>(next.getChildren());
                Collections.reverse(ch);

                ch.forEach(wait::addFirst);
            }

            return next;
        }
    }

    private Artifacts() {
        // UTILITY CLASS
    }

    /**
     * Returns the root of the tree {@code artifact} is a part of.
     *
     * @param artifact
     *         the {@link Artifact} for whose tree the root is to be returned
     * @param <T>
     *         the {@code Artifact} type
     * @return the root of the tree
     */
    public static <T extends Artifact<T>> T root(T artifact) {
        T root = artifact;

        while (!root.isRoot()) {
            root = root.getParent();
        }

        return root;
    }

    /**
     * Returns the tree rooted in {@code treeRoot} in breadth-first order.
     *
     * @param treeRoot
     *         the root of the tree to return in breadth-first order
     * @param <T>
     *         the {@code Artifact} type
     * @return the nodes of the tree rooted in {@code treeRoot} in breadth-first order
     */
    public static <T extends Artifact<T>> List<T> bfs(T treeRoot) {
        return bfsStream(treeRoot).collect(Collectors.toList());
    }

    /**
     * Returns a {@code Stream} consisting of the elements of the tree rooted in {@code treeRoot} in breadth-first order.
     *
     * @param treeRoot
     *         the root of the tree to return in breadth-first order
     * @param <T>
     *         the {@code Artifact} type
     * @return the nodes of the tree rooted in {@code treeRoot} in breadth-first order as a {@code Stream}
     */
    public static <T extends Artifact<T>> Stream<T> bfsStream(T treeRoot) {
        return StreamSupport.stream(bfsIterable(treeRoot).spliterator(), false);
    }

    /**
     * Returns an {@code Iterable} returning the elements of the tree rooted in {@code treeRoot} in breadth-first order
     * from the {@link Iterator#hasNext()} method of its {@code Iterator}.
     *
     * @param treeRoot
     *         the root of the tree to return in breadth-first order
     * @param <T>
     *         the {@code Artifact} type
     * @return an {@code Iterable} over the elements of the tree rooted in {@code treeRoot} in breadth-first order
     */
    public static <T extends Artifact<T>> Iterable<T> bfsIterable(T treeRoot) {
        return () -> new BFSIterator<>(treeRoot);
    }

    /**
     * Returns the tree rooted in {@code treeRoot} in depth-first order.
     *
     * @param treeRoot
     *         the root of the tree to return in depth-first order
     * @param <T>
     *         the {@code Artifact} type
     * @return the nodes of the tree rooted in {@code treeRoot} in depth-first order
     */
    public static <T extends Artifact<T>> List<T> dfs(T treeRoot) {
        return dfsStream(treeRoot).collect(Collectors.toList());
    }

    /**
     * Returns a {@code Stream} consisting of the elements of the tree rooted in {@code treeRoot} in depth-first order.
     *
     * @param treeRoot
     *         the root of the tree to return in depth-first order
     * @param <T>
     *         the {@code Artifact} type
     * @return the nodes of the tree rooted in {@code treeRoot} in depth-first order as a {@code Stream}
     */
    public static <T extends Artifact<T>> Stream<T> dfsStream(T treeRoot) {
        return StreamSupport.stream(dfsIterable(treeRoot).spliterator(), false);
    }

    /**
     * Returns an {@code Iterable} returning the elements of the tree rooted in {@code treeRoot} in depth-first order
     * from the {@link Iterator#hasNext()} method of its {@code Iterator}.
     *
     * @param treeRoot
     *         the root of the tree to return in depth-first order
     * @param <T>
     *         the {@code Artifact} type
     * @return an {@code Iterable} over the elements of the tree rooted in {@code treeRoot} in depth-first order
     */
    public static <T extends Artifact<T>> Iterable<T> dfsIterable(T treeRoot) {
        return () -> new DFSIterator<>(treeRoot);
    }

    /**
     * Copies the given tree of {@link Artifact Artifacts}.
     *
     * @param treeRoot
     *         the root of the tree to copy
     * @param <T>
     *         the {@code Artifact} type
     * @return the root of the copied tree
     */
    public static <T extends Artifact<T>> T copyTree(T treeRoot) {
        T copy = treeRoot.copy();
        treeRoot.getChildren().forEach(c -> copy.addChild(copyTree(c)));

        return copy;
    }
}
