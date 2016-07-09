package de.fosd.jdime.strdump.graphviz;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface GraphvizElement {

    default String dump() {
        StringWriter str = new StringWriter();

        try (PrintWriter out = new PrintWriter(str)) {
            dump(out);
        }

        return str.toString();
    }

    default void dump(OutputStream out) {
        Charset utf8 = StandardCharsets.UTF_8;

        try (PrintWriter pOut = new PrintWriter(new OutputStreamWriter(out, utf8))) {
            dump(pOut);
        }
    }

    default void dump(PrintWriter out) {
        dump("", out);
    }

    void dump(String indent, PrintWriter out);
}
