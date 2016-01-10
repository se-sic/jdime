package de.fosd.jdime.strdump;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import de.fosd.jdime.common.Artifact;

/**
 * Dumps the given <code>Artifact</code> tree using the TGF format.
 *
 * @see <href link="http://docs.yworks.com/yfiles/doc/developers-guide/tgf.html">TGF</href>
 */
public class TGFTreeDump implements StringDumper {

    @Override
    public <T extends Artifact<T>> String dump(Artifact<T> artifact, Function<Artifact<T>, String> getLabel) {
        AtomicInteger nextId = new AtomicInteger(); // an easy way to encapsulate an Integer for the lambdas
        Map<Artifact<T>, Integer> ids = new HashMap<>();
        List<String> nodeIDs = new ArrayList<>();
        List<String> connections = new ArrayList<>();
        Deque<Artifact<T>> q = new ArrayDeque<>(Collections.singleton(artifact));

        while (!q.isEmpty()) {
            Artifact<T> current = q.removeFirst();

            Integer fromId = ids.computeIfAbsent(current, a -> nextId.getAndIncrement());
            nodeIDs.add(String.format("%d %s", fromId, current.toString()));

            for (T t : current.getChildren()) {
                Integer toId = ids.computeIfAbsent(t, a -> nextId.getAndIncrement());
                connections.add(String.format("%d %d", fromId, toId));
            }

            current.getChildren().forEach(q::addFirst);
        }

        String ls = System.lineSeparator();
        return String.format("%s%n#%n%s", String.join(ls, nodeIDs), String.join(ls, connections));
    }
}
