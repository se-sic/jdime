package de.fosd.jdime.strdump;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.Artifacts;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

public class MatchingsTreeDump {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public <T extends Artifact<T>> void dump(Matchings<T> matchings, OutputStream os) {

        if (matchings.isEmpty()) {
            return;
        }

        T lRoot, rRoot;

        {
            Matching<T> first = matchings.iterator().next();

            lRoot = Artifacts.root(first.getLeft());
            rRoot = Artifacts.root(first.getRight());
        }

        String indent = "  ";

        Map<T, String> ids = assignIDs(lRoot, "L");
        ids.putAll(assignIDs(rRoot, "R"));

        try (PrintWriter out = new PrintWriter(new BufferedOutputStream(os))) {
            out.printf("graph \"TollerGraph\" {%n");

            writeAttributes(indent, "node", out, "shape", "box");
            writeStatement(indent, "rankdir", "TB", out);

            writeSubgraph(lRoot, "Left", indent, ids, out);
            writeSubgraph(rRoot, "Right", indent, ids, out);

            for (Matching<T> matching : matchings) {
                writeEdge(indent, ids.get(matching.getLeft()), ids.get(matching.getRight()), out, "constraint", "false");
            }

            out.print("}");
        }
    }

    private <T extends Artifact<T>> Map<T, String> assignIDs(T root, String prefix) {
        Revision.SuccessiveNameSupplier name = new Revision.SuccessiveNameSupplier();
        Map<T, String> ids = new HashMap<>();

        Artifacts.dfs(root).forEach(a -> ids.put(a, prefix + name.get()));

        return ids;
    }

    private void writeStatement(String indent, String left, String right, PrintWriter out) {
        out.printf("%s%s=%s;%n", indent, left, right);
    }

    private void writeNode(String indent, String id, PrintWriter out, String... kvAttributes) {
        out.printf("%s%s", indent, id);
        writeAttributeSet(out, kvAttributes);
        out.printf(";%n");
    }

    private void writeEdge(String indent, String lId, String rId, PrintWriter out, String... kvAttributes) {
        out.printf("%s%s -- %s", indent, lId, rId);
        writeAttributeSet(out, kvAttributes);
        out.printf(";%n");
    }

    private void writeAttributes(String indent, String target, PrintWriter out, String... kvAttributes) {
        out.print(indent);
        out.print(target);
        writeAttributeSet(out, kvAttributes);
        out.printf(";%n");
    }

    private void writeAttributeSet(PrintWriter out, String... kvAttributes) {
        if (kvAttributes != null && kvAttributes.length != 0) {
            if (kvAttributes.length % 2 != 0) {
                throw new IllegalArgumentException("Number of given attribute strings must be even.");
            }

            out.write(" [");

            for (int i = 0; i < kvAttributes.length; i += 2) {
                out.printf("%s=%s", enquote(kvAttributes[i]), enquote(kvAttributes[i + 1]));

                if (i < kvAttributes.length - 2) {
                    out.write(", ");
                }
            }

            out.write(']');
        }
    }

    private <T extends Artifact<T>> void writeSubgraph(T root, String id, String indent, Map<T, String> ids, PrintWriter out) {
        String cIndent = indent + "  ";

        out.printf("%ssubgraph cluster%s {%n", indent, id);

        writeStatement(cIndent, "label", id, out);
        writeAttributes(cIndent, "edge", out, "color", toHexString(0, 0, 0, 100));

        List<T> bfs = Artifacts.bfs(root);

        for (T artifact : bfs) {
            writeNode(cIndent, ids.get(artifact), out, "label", artifact.getId());
        }

        for (T artifact : bfs) {
            for (T c : artifact.getChildren()) {
                writeEdge(cIndent, ids.get(artifact), ids.get(c), out);
            }
        }

        out.printf("%s}%n", indent);
    }

    private String enquote(String s) {
        if (WHITESPACE.matcher(s).find()) {
            return String.format("\"%s\"", s);
        } else {
            return s;
        }
    }

    private String toHexString(int r, int g, int b, int a) {
        return String.format("\"#%02x%02x%02x%02x\"", r, g, b, a);
    }
}
