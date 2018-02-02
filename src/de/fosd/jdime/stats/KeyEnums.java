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
package de.fosd.jdime.stats;

/**
 * Container class to hold enums used as categories for the <code>Statistics</code> class.
 */
public final class KeyEnums {

    /**
     * Utility class.
     */
    private KeyEnums() {}

    /**
     * <code>Artifact</code>s in a tree to be merged (be it an AST or a directory tree) are of one of the types in this
     * enum. <code>FILE</code>, <code>DIRECTORY</code> and <code>LINE</code> are used to address corresponding
     * statistics container in the <code>Statistics</code> class.
     */
    public enum Type {

        /**
         * A <code>FileArtifact</code> representing a file (not a directory).
         */
        FILE,

        /**
         * A <code>FileArtifact</code> representing a directory.
         */
        DIRECTORY,

        /**
         * A line in the output of JDime.
         */
        LINE,

        /**
         * Any AST node.
         */
        NODE,

        /**
         * An AST node representing a class declaration.
         */
        CLASS,

        /**
         * An AST node representing a method declaration.
         */
        METHOD,

        /**
         * An AST node representing a try statement
         */
        TRY,

        /**
         * An AST node representing a generic block statement.
         */
        BLOCK
    }

    /**
     * AST nodes occur on one of the levels represented by this enum.
     */
    public enum Level {

        /**
         * <code>Artifact</code>s like <code>FileArtifact</code>s have no level as they do not represent a piece of
         * code.
         */
        NONE,

        /**
         * Everything above class declaration, e.g. import statements.
         */
        TOP,

        /**
         * Everything within class declaration but outside methods.
         */
        CLASS,

        /**
         * Everything inside methods.
         */
        METHOD
    }
}
