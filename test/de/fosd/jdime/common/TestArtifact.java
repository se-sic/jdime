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
package de.fosd.jdime.common;

import java.io.IOException;

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.KeyEnums;

/**
 * A simple <code>Artifact</code> to test the functionality of <code>MCESubtreeMatcher</code>. Not all all methods
 * are implemented. Should be deleted later on.
 */
public class TestArtifact extends Artifact<TestArtifact> {

    private static int nextID = 0;

    private int id;

    public TestArtifact() {
        this.id = nextID++;
        this.children = new ArtifactList<>();
    }

    public TestArtifact(int id) {
        this.id = id;
        this.children = new ArtifactList<>();
    }

    @Override
    public TestArtifact addChild(TestArtifact child) {
        children.add(child);
        return child;
    }

    @Override
    public TestArtifact clone() {
        TestArtifact clone = new TestArtifact();
        clone.id = id;
        clone.children = children;
        return clone;
    }

    @Override
    public TestArtifact createConflictArtifact(TestArtifact left, TestArtifact right) {
        return null;
    }

    @Override
    public TestArtifact createChoiceArtifact(String condition, TestArtifact artifact) {
        return null;
    }
    public TestArtifact createEmptyArtifact() {
        return null;
    }

    @Override
    protected String dumpTree(String indent) {
        return null;
    }

    @Override
    public String prettyPrint() {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public void deleteChildren() {
        this.children = new ArtifactList<>();
    }

    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestArtifact that = (TestArtifact) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean hasUniqueLabels() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public boolean isOrdered() {
        return true;
    }

    @Override
    public boolean matches(TestArtifact other) {
        return id == other.id;
    }

    @Override
    public void merge(MergeOperation<TestArtifact> operation, MergeContext context) {

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + String.valueOf(id);
    }

    @Override
    public int compareTo(TestArtifact o) {
        return id - o.id;
    }

    @Override
    public KeyEnums.Type getType() {
        return null;
    }

    @Override
    public KeyEnums.Level getLevel() {
        return null;
    }
}
