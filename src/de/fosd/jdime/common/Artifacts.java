package de.fosd.jdime.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * This class consists of {@code static} utility methods for operating on {@code Artifact} instances.
 */
public final class Artifacts {

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
        List<T> bfs = new ArrayList<>(treeRoot.getTreeSize());
        Deque<T> wait = new ArrayDeque<>();

        wait.addFirst(treeRoot);

        while (!wait.isEmpty()) {
            T t = wait.removeFirst();

            bfs.add(t);
            t.getChildren().forEach(wait::addLast);
        }

        return bfs;
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
        List<T> dfs = new ArrayList<>(treeRoot.getTreeSize());
        Deque<T> wait = new ArrayDeque<>();

        wait.addFirst(treeRoot);

        while (!wait.isEmpty()) {
            T t = wait.removeFirst();

            dfs.add(t);

            List<T> ch = new ArrayList<>(t.getChildren());
            Collections.reverse(ch);

            ch.forEach(wait::addFirst);
        }

        return dfs;
    }
}
