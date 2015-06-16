package de.fosd.jdime.matcher.ordered.mceSubtree.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.operations.MergeOperation;

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
	public void copyArtifact(TestArtifact destination) throws IOException {

	}

	@Override
	public void createArtifact(boolean isLeaf) throws IOException {

	}

	@Override
	public TestArtifact createConflictArtifact(TestArtifact left, TestArtifact right) {
		return null;
	}

	@Override
	public TestArtifact createChoiceDummy(String condition, TestArtifact artifact) throws FileNotFoundException {
		return null;
	}
	public TestArtifact createEmptyArtifact() throws FileNotFoundException {
		return null;
	}

	@Override
	protected String dumpTree(String indent) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getId() {
		return String.valueOf(id);
	}

	@Override
	public String getStatsKey(MergeContext context) {
		return null;
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
	public void initializeChildren() {

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
	public void merge(MergeOperation<TestArtifact> operation, MergeContext context) throws IOException, InterruptedException {

	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + String.valueOf(id);
	}

	@Override
	public int compareTo(TestArtifact o) {
		return id - o.id;
	}
}
