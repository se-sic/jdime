package de.fosd.jdime.matcher.ordered.mceSubtree.test;

import de.fosd.jdime.common.Tuple;
import de.fosd.jdime.matcher.NewMatching;
import de.fosd.jdime.matcher.ordered.mceSubtree.BalancedSequence;

import java.io.IOException;
import java.util.Set;

/**
 * Contains methods for constructing <code>TestArtifact</code> trees
 */
public class TestTrees {

	/**
	 * Returns the trees used as examples in the paper.
	 *
	 * @return the trees
	 */
	private static Tuple<TestArtifact, TestArtifact> paperTree() {
		return null;
	}

	/**
	 * Returns two simple test trees.
	 *
	 * @return the trees
	 */
	private static Tuple<TestArtifact, TestArtifact> simpleTree() throws IOException {
		TestArtifact t0 = new TestArtifact();
		TestArtifact t1 = new TestArtifact();
		TestArtifact t2 = new TestArtifact();
		TestArtifact t3 = new TestArtifact();
		TestArtifact t4 = new TestArtifact();

		t0.addChild(t1);
		t0.addChild(t2);
		t2.addChild(t3);
		t2.addChild(t4);

		return new Tuple<>(t0, t2);
	}

	public static void main(String[] args) throws IOException {
		Tuple<TestArtifact, TestArtifact> trees = simpleTree();

		BalancedSequence<TestArtifact> s = new BalancedSequence<>(trees.x);
		BalancedSequence<TestArtifact> t = new BalancedSequence<>(trees.y);

		Set<NewMatching<TestArtifact>> lcs = BalancedSequence.lcs(s, t);

		System.out.println("Left tree as BalancedSequence:");
		System.out.println(s);
		System.out.println("Right tree as BalancedSequence:");
		System.out.println(t.toString() + '\n');

		System.out.println("Found Matchings:");
		for (NewMatching<TestArtifact> lc : lcs) {
			System.out.println(lc);
		}
	}
}
