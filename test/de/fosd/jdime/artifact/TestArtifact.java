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
package de.fosd.jdime.artifact;

import java.util.Optional;
import java.util.function.Supplier;

import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.execption.NotYetImplementedException;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.stats.KeyEnums;

/**
 * A simple <code>Artifact</code> to test the functionality of <code>MCESubtreeMatcher</code>. Not all all methods
 * are implemented. Should be deleted later on.
 */
public class TestArtifact extends Artifact<TestArtifact> {

    private static final Revision testRevision = new Revision("TEST");

    private String label;
    private KeyEnums.Type type;

    public TestArtifact(String label, KeyEnums.Type type) {
        this(testRevision, label, type);
    }

    public TestArtifact(Revision rev, String label, KeyEnums.Type type) {
        super(rev, 0);
        this.label = label;
        this.type = type;
    }

    @Override
    protected TestArtifact self() {
        return this;
    }

    @Override
    public TestArtifact copy() {
        throw new NotYetImplementedException();
    }

    @Override
    public TestArtifact createConflictArtifact(TestArtifact left, TestArtifact right) {
        TestArtifact conflict = new TestArtifact(MergeScenario.CONFLICT, "Conflict", KeyEnums.Type.NODE);
        conflict.setConflict(left, right);

        return conflict;
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
    public String getId() {
        return String.format("%s : %s : %d", getRevision(), label, getNumber());
    }

    @Override
    protected String hashId() {
        return label + type.name();
    }

    @Override
    public Optional<Supplier<String>> getUniqueLabel() {
        return Optional.empty();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isOrdered() {
        return type != KeyEnums.Type.METHOD;
    }

    @Override
    public boolean matches(TestArtifact other) {
        return this.type == other.type && this.label.equals(other.label);
    }

    @Override
    public boolean categoryMatches(TestArtifact other) {
        return this.type == other.type;
    }

    @Override
    public void merge(MergeOperation<TestArtifact> operation, MergeContext context) {

    }

    @Override
    public String toString() {
        return getId() + " (" + type + ")";
    }

    @Override
    public KeyEnums.Type getType() {
        return type;
    }

    @Override
    public KeyEnums.Level getLevel() {
        return null;
    }
}
