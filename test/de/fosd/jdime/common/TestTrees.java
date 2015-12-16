package de.fosd.jdime.common;

/**
 * Contains methods for constructing <code>TestArtifact</code> trees
 */
public final class TestTrees {

    /**
     * Returns the trees used as examples in the paper.
     *
     * @return the trees
     */
    public static TestArtifact paperTree() {
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
    public static Tuple<TestArtifact, TestArtifact> simpleTree() {
        TestArtifact t1 = new TestArtifact(1);
        TestArtifact t2 = new TestArtifact(2);
        TestArtifact t3 = new TestArtifact(3);
        TestArtifact t4 = new TestArtifact(4);
        TestArtifact t5 = new TestArtifact(5);

        t1.addChild(t2);
        t1.addChild(t3);
        t3.addChild(t4);
        t3.addChild(t5);

        return Tuple.of(t1, t3);
    }
}
