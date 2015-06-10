package de.fosd.jdime.matcher.ordered.mceSubtree.test;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.Tuple;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matchings;
import de.fosd.jdime.matcher.NewMatching;
import de.fosd.jdime.matcher.ordered.mceSubtree.BalancedSequence;
import de.fosd.jdime.matcher.ordered.mceSubtree.MCESubtreeMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains methods for constructing <code>TestArtifact</code> trees
 */
public class TestTrees {

	/**
	 * Returns the trees used as examples in the paper.
	 *
	 * @return the trees
	 */
	private static TestArtifact paperTree() {
		TestArtifact t = new TestArtifact();
		TestArtifact t1 = new TestArtifact();
		TestArtifact t2 = new TestArtifact();
		TestArtifact t3 = new TestArtifact();
		TestArtifact t4 = new TestArtifact();
		TestArtifact t5 = new TestArtifact();
		TestArtifact t6 = new TestArtifact();
		TestArtifact t7 = new TestArtifact();
		TestArtifact t8 = new TestArtifact();
		TestArtifact t9 = new TestArtifact();
		TestArtifact t10 = new TestArtifact();
		TestArtifact t11 = new TestArtifact();
		TestArtifact t12 = new TestArtifact();
		TestArtifact t13 = new TestArtifact();
		TestArtifact t14 = new TestArtifact();
		TestArtifact t15 = new TestArtifact();
		TestArtifact t16 = new TestArtifact();
		TestArtifact t17 = new TestArtifact();
		TestArtifact t18 = new TestArtifact();

		t.addChild(t1);
		t.addChild(t2);

		t1.addChild(t3);
		t1.addChild(t4);

		t2.addChild(t5);
		t2.addChild(t6);

		t4.addChild(t7);
		t4.addChild(t8);

		t7.addChild(t11);
		t7.addChild(t12);

		t5.addChild(t9);
		t5.addChild(t10);

		t10.addChild(t13);
		t10.addChild(t14);

		t13.addChild(t15);
		t13.addChild(t16);

		t16.addChild(t17);
		t16.addChild(t18);

		return t;
	}

	/**
	 * Returns two simple test trees.
	 *
	 * @return the trees
	 */
	private static Tuple<TestArtifact, TestArtifact> simpleTree() {
		TestArtifact t0 = new TestArtifact();
		TestArtifact t1 = new TestArtifact();
		TestArtifact t2 = new TestArtifact();
		TestArtifact t3 = new TestArtifact();
		TestArtifact t4 = new TestArtifact();

		t0.addChild(t1);
		t0.addChild(t2);
		t2.addChild(t3);
		t2.addChild(t4);

		return Tuple.of(t0, t2);
	}

	public static void main(String[] args) {
		Tuple<TestArtifact, TestArtifact> trees = simpleTree();

		BalancedSequence<TestArtifact> s = new BalancedSequence<>(trees.x);
		BalancedSequence<TestArtifact> t = new BalancedSequence<>(trees.y);

		MCESubtreeMatcher<TestArtifact> matcher = new MCESubtreeMatcher<>(new Matcher<TestArtifact>());
		Matchings<TestArtifact> matchings = matcher.match(null, trees.x, trees.y, MergeContext.LOOKAHEAD_FULL);

		System.out.println("Left tree as BalancedSequence:");
		System.out.println(s);
		System.out.println("Right tree as BalancedSequence:");
		System.out.println(t.toString() + '\n');

		List<NewMatching<TestArtifact>> l = new ArrayList<>(matchings);
		Collections.sort(l);

		for (NewMatching<TestArtifact> matching : l) {
			System.out.println(matching);
		}
	}
}
