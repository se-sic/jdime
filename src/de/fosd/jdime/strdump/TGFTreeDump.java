package de.fosd.jdime.strdump;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.fosd.jdime.common.Artifact;

/**
 *
 *
 * @param <T>
 */
public class TGFTreeDump<T extends Artifact<T>> extends StringDumper<T> {

    /**
     * Constructs a new <code>StringDumper</code> for the given <code>Artifact</code>.
     *
     * @param artifact
     *         the <code>Artifact</code> to dump to a <code>String</code>
     */
    public TGFTreeDump(T artifact) {
        super(artifact);
    }

    @Override
    protected void buildString() {
        AtomicInteger nextId = new AtomicInteger(); // an easy way to encapsulate an Integer for the lambdas
        Map<Artifact<T>, Integer> ids = new HashMap<>();
        List<String> nodeIDs = new ArrayList<>();
        List<String> connections = new ArrayList<>();
        Deque<Artifact<T>> q = new ArrayDeque<>(Collections.singleton(artifact));

        while (!q.isEmpty()) {
            Artifact<T> artifact = q.removeFirst();

            Integer fromId = ids.computeIfAbsent(artifact, a -> nextId.getAndIncrement());
            nodeIDs.add(String.format("%d %s", fromId, artifact.toString()));

            for (T t : artifact.getChildren()) {
                Integer toId = ids.computeIfAbsent(t, a -> nextId.getAndIncrement());
                connections.add(String.format("%d %d", fromId, toId));
            }

            artifact.getChildren().forEach(q::addFirst);
        }

        String ls = System.lineSeparator();
        String rep = String.format("%s%n#%n%s", String.join(ls, nodeIDs), String.join(ls, connections));

        builder.append(rep);
    }
}
