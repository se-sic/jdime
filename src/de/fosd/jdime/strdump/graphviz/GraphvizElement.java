package de.fosd.jdime.strdump.graphviz;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * An interface implemented by all classes used for building the Graphviz graph. Containts the methods used for
 * dumping the object structure to DOT language source code.
 */
public interface GraphvizElement {

    /**
     * Dumps this <code>GraphvizElement</code> to DOT language source code and returns the result as a
     * <code>String</code>.
     *
     * @return the resulting DOT language source code
     */
    default String dump() {
        StringWriter str = new StringWriter();

        try (PrintWriter out = new PrintWriter(str)) {
            dump(out);
        }

        return str.toString();
    }

    /**
     * Dumps this <code>GraphvizElement</code> as DOT language source code to the given <code>OutputStream</code>.
     * UTF-8 encoding will be used.
     *
     * @param out
     *         the <code>OutputStream</code> to dump to
     */
    default void dump(OutputStream out) {
        Charset utf8 = StandardCharsets.UTF_8;

        try (PrintWriter pOut = new PrintWriter(new OutputStreamWriter(out, utf8))) {
            dump(pOut);
        }
    }

    /**
     * Dumps this <code>GraphvizElement</code> as DOT language source code to the given <code>PrintWriter</code>.
     *
     * @param out
     *         the <code>PrintWriter</code> to dump to
     */
    default void dump(PrintWriter out) {
        dump("", out);
    }

    /**
     * Dumps this <code>GraphvizElement</code> as DOT language source code the given <code>PrintWriter</code>.
     * The specified <code>indent</code> will be prepended.
     *
     * @param indent
     *         the indentation to use
     * @param out
     *         the <code>PrintWriter</code> to dump to
     */
    void dump(String indent, PrintWriter out);
}
