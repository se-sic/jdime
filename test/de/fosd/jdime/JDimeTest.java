/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
package de.fosd.jdime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Contains useful methods for running tests for JDime.
 */
public class JDimeTest {

    protected static File leftDir;
    protected static File baseDir;
    protected static File rightDir;

    @BeforeClass
    public static void initDirectories() throws Exception {

        leftDir = file("threeway", "left");
        baseDir = file("threeway", "base");
        rightDir = file("threeway", "right");

        Arrays.asList(leftDir, baseDir, rightDir).forEach(f -> {
            assertTrue(f.getAbsolutePath() + " is not a directory.", f.isDirectory());
        });
    }

    /**
     * Analogous to {@link File#File(File, String)}. Additionally asserts that the constructed <code>File</code>
     * exists.
     *
     * @param parent
     *         the parent abstract pathname
     * @param child
     *         the child pathname string
     * @return the constructed <code>File</code>
     */
    protected static File file(File parent, String child) {
        File f = new File(parent, child);
        assertTrue(f + " does not exist.", f.exists());

        return f;
    }

    /**
     * Analogous to {@link File#File(File, String)} with the child path constructed from the given names.
     *
     * @param parent
     *         the parent abstract pathname
     * @param name
     *         the first element of the child path
     * @param names
     *         the other elements of the child path
     * @return the resulting <code>File</code>
     */
    protected static File file(File parent, String name, String... names) {

        if (names != null) {
            String path = String.format("%s/%s", name, String.join("/", names));
            return file(parent, path);
        } else {
            return file(parent, name);
        }
    }

    /**
     * Returns a file using the {@link Class#getResource(String)} method of the class <code>JDimeTest</code> and
     * the given path.
     *
     * @param path
     *         the file path
     * @return the resulting <code>File</code>
     * @throws AssertionError
     *         if the file does not exist or there is an exception constructing it
     */
    protected static File file(String path) {
        URL res = JDimeTest.class.getResource(path);

        assertNotNull("The file " + path + " was not found.", res);

        try {
            return new File(res.toURI());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    /**
     * Constructs an absolute (in the classpath) path from the given names an passes it to {@link #file(String)}.
     *
     * @param name
     *         the first element of the path
     * @param names
     *         the other elements of the path
     * @return the resulting <code>File</code>
     * @throws AssertionError
     *         if the file does not exist or there is an exception constructing it
     */
    protected static File file(String name, String... names) {

        if (names != null) {
            String path = String.format("/%s/%s", name, String.join("/", names));
            return file(path);
        } else {
            return file("/" + name);
        }
    }

    /**
     * Removes everything after the conflict marker in any line starting with one.
     *
     * @param content
     *         the content to normalize
     * @return the normalized <code>String</code>
     */
    protected static String normalize(String content) {
        String conflictStart = "<<<<<<<";
        String conflictEnd = ">>>>>>>";
        String lineSeparator = System.lineSeparator();
        StringBuilder b = new StringBuilder(content.length());

        try (BufferedReader r = new BufferedReader(new StringReader(content))) {
            for (Iterator<String> it = r.lines().iterator(); it.hasNext(); ) {
                String l = it.next();

                if (l.startsWith(conflictStart)) {
                    l = conflictStart;
                } else if (l.startsWith(conflictEnd)) {
                    l = conflictEnd;
                }

                b.append(l);

                if (it.hasNext()) {
                    b.append(lineSeparator);
                }
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return b.toString();
    }
}
