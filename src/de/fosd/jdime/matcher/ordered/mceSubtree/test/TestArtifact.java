package de.fosd.jdime.matcher.ordered.mceSubtree.test;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.operations.MergeOperation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple <code>Artifact</code> to test the functionality of <code>MCESubtreeMatcher</code>. Not all all methods
 * are implemented. Should be deleted later on.
 */
public class TestArtifact extends Artifact<TestArtifact> {

	private static int nextID = 0;

	private List<TestArtifact> children;
	private int id;

	public TestArtifact() {
		this.children = new ArrayList<>();
		this.id = nextID++;
	}

	@Override
	public TestArtifact addChild(TestArtifact child) throws IOException {
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
	public TestArtifact createConflictDummy(TestArtifact type, TestArtifact left, TestArtifact right) throws FileNotFoundException {
		return null;
	}

	@Override
	public TestArtifact createEmptyDummy() throws FileNotFoundException {
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
		return false;
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
		return false;
	}

	@Override
	public boolean isOrdered() {
		return false;
	}

	@Override
	public boolean matches(TestArtifact other) {
		return false;
	}

	@Override
	public void merge(MergeOperation<TestArtifact> operation, MergeContext context) throws IOException, InterruptedException {

	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public void write(String str) throws IOException {

	}

	@Override
	public int compareTo(TestArtifact o) {
		return 0;
	}
}
