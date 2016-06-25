package de.fosd.jdime.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public final class Artifacts {

    private Artifacts() {
        // UTILITY CLASS
    }

    public static  <T extends Artifact<T>> List<T> bfs(T treeRoot) {
        List<T> bfs = new ArrayList<>();
        Deque<T> wait = new ArrayDeque<>();

        wait.addFirst(treeRoot);

        while (!wait.isEmpty()) {
            T t = wait.removeFirst();

            bfs.add(t);
            t.getChildren().forEach(wait::addLast);
        }

        return bfs;
    }

    public static <T extends Artifact<T>> List<T> dfs(T treeRoot) {
        List<T> dfs = new ArrayList<>();
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
