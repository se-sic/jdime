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

import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.stats.KeyEnums;

/**
 * A simple <code>Artifact</code> to test the functionality of <code>MCESubtreeMatcher</code>. Not all all methods
 * are implemented. Should be deleted later on.
 */
public class TestArtifact extends Artifact<TestArtifact> {

    private static final Revision testRevision = new Revision("TEST");

    public TestArtifact() {
        this(testRevision);
    }

    public TestArtifact(Revision rev) {
        super(rev);
        this.children = new ArtifactList<>();
    }

    public TestArtifact(int id) {
        super(testRevision);
        this.setNumber(id);
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
        clone.setNumber(getNumber());
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

    public TestArtifact createEmptyArtifact(Revision revision) {
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
        return String.valueOf(getNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestArtifact that = (TestArtifact) o;

        return getNumber() == that.getNumber();
    }

    @Override
    public int hashCode() {
        return getNumber();
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
        return getNumber() == other.getNumber();
    }

    @Override
    public void merge(MergeOperation<TestArtifact> operation, MergeContext context) {

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + String.valueOf(getNumber());
    }

    @Override
    public int compareTo(TestArtifact o) {
        return getNumber() - o.getNumber();
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
