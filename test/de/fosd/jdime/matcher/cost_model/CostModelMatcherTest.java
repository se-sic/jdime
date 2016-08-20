package de.fosd.jdime.matcher.cost_model;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Optional;

import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.TestArtifact;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.strdump.MatchingsTreeDump;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static de.fosd.jdime.common.MergeScenario.LEFT;
import static de.fosd.jdime.common.MergeScenario.RIGHT;
import static de.fosd.jdime.stats.KeyEnums.Type.NODE;
import static org.junit.Assert.assertEquals;

public class CostModelMatcherTest {

    private CostModelMatcher<TestArtifact> matcher;

    private TestArtifact left;
    private TestArtifact right;

    private TestArtifact l1, l2, l3, l4, l5, l6, l7, l8;
    private TestArtifact r1, r2, r3, r4, r5, r6, r7, r8, r9, r10;

    @Before
    public void setUp() throws Exception {
        matcher = new CostModelMatcher<>();

        l1 = new TestArtifact(LEFT, "A", NODE);
        l2 = new TestArtifact(LEFT, "B", NODE);
        l3 = new TestArtifact(LEFT, "C", NODE);
        l4 = new TestArtifact(LEFT, "C", NODE);
        l5 = new TestArtifact(LEFT, "D", NODE);
        l6 = new TestArtifact(LEFT, "F", NODE);
        l7 = new TestArtifact(LEFT, "F", NODE);
        l8 = new TestArtifact(LEFT, "G", NODE);

        l1.addChild(l2);
        l1.addChild(l5);

        l2.addChild(l3);
        l2.addChild(l4);

        l5.addChild(l6);
        l5.addChild(l7);
        l5.addChild(l8);
        
        r1 = new TestArtifact(RIGHT, "A", NODE);
        r2 = new TestArtifact(RIGHT, "G", NODE);
        r3 = new TestArtifact(RIGHT, "D", NODE);
        r4 = new TestArtifact(RIGHT, "B", NODE);
        r5 = new TestArtifact(RIGHT, "C", NODE);
        r6 = new TestArtifact(RIGHT, "F", NODE);
        r7 = new TestArtifact(RIGHT, "F", NODE);
        r8 = new TestArtifact(RIGHT, "F", NODE);
        r9 = new TestArtifact(RIGHT, "C", NODE);
        r10 = new TestArtifact(RIGHT, "C", NODE);

        r1.addChild(r2);
        r1.addChild(r3);
        r1.addChild(r4);

        r4.addChild(r5);
        r4.addChild(r9);
        r4.addChild(r10);

        r5.addChild(r6);
        r5.addChild(r7);
        r5.addChild(r8);

        left = l1;
        right = r1;

        left.renumberTree();
        right.renumberTree();
    }

    // TODO remove
    boolean show = true;

    @Test
    public void testShow() throws Exception {
        Matchings<TestArtifact> m = Matchings.of(left, right, 0);
        show(m);
    }

    @Test
    public void paperA() throws Exception {
        Matchings<TestArtifact> expected = new Matchings<>();

        expected.add(new Matching<>(l1, r1, 0));

        expected.add(new Matching<>(l2, r4, 0));
        expected.add(new Matching<>(l3, r9, 0));
        expected.add(new Matching<>(l4, r10, 0));

        expected.add(new Matching<>(l5, r5, 0));
        expected.add(new Matching<>(l6, r6, 0));
        expected.add(new Matching<>(l7, r7, 0));
        expected.add(new Matching<>(l8, r8, 0));

        testCostModelMatching(expected, 0.9f, 1.0f, 1.0f, 0.1f);
    }

    @Test
    public void paperB() throws Exception {
        Matchings<TestArtifact> expected = new Matchings<>();

        expected.add(new Matching<>(l1, r1, 0));

        expected.add(new Matching<>(l2, r3, 0));
        expected.add(new Matching<>(l5, r4, 0));

        expected.add(new Matching<>(l3, r9, 0));
        expected.add(new Matching<>(l4, r10, 0));

        expected.add(new Matching<>(l6, r6, 0));
        expected.add(new Matching<>(l7, r7, 0));
        expected.add(new Matching<>(l8, r8, 0));

        testCostModelMatching(expected, 0.9f, 1.0f, 0.1f, 1.0f);
    }

    @Test
    public void paperC() throws Exception {
        Matchings<TestArtifact> expected = new Matchings<>();

        expected.add(new Matching<>(l1, r1, 0));

        expected.add(new Matching<>(l2, r4, 0));
        expected.add(new Matching<>(l3, r9, 0));
        expected.add(new Matching<>(l4, r10, 0));

        expected.add(new Matching<>(l5, r3, 0));

        expected.add(new Matching<>(l6, r6, 0));
        expected.add(new Matching<>(l7, r7, 0));

        expected.add(new Matching<>(l8, r2, 0));

        testCostModelMatching(expected, 1.0f, 1.0f, 0.5f, 0.5f);
    }

    private void testCostModelMatching(Matchings<TestArtifact> expected, float wr, float wn, float wa, float ws) throws Exception {
        MergeContext context = new MergeContext();

        context.wr = wr;
        context.wn = wn;
        context.wa = wa;
        context.ws = ws;

        // TODO extract parameters (or constants)
        context.pAssign = 0.7f;
        context.seed = Optional.of(42L);
        context.costModelIterations = 100;

        Matchings<TestArtifact> actual = matcher.match(context, left, right);

        if (show) {
            System.out.println("Expected cost is " + matcher.cost(expected, left, right));
            show(actual);
        }

        assertEquals(expected, actual);
    }

    // TODO remove, or make it more robust
    private void show(Matchings<TestArtifact> matchings) throws Exception {
        File dotFormat = Files.createTempFile(null, null).toFile();
        File image = new File(dotFormat.getParentFile(), dotFormat.getName() + ".png");

        FileUtils.forceDeleteOnExit(dotFormat);

        try (FileOutputStream out = new FileOutputStream(dotFormat)) {
            MatchingsTreeDump mDump = new MatchingsTreeDump();
            mDump.toGraphvizGraph(matchings).dump(out);
        }

        String[] args = {"dot", "-Tpng", "-O", dotFormat.getAbsolutePath()};
        Runtime.getRuntime().exec(args, null, dotFormat.getParentFile()).waitFor();

        Desktop.getDesktop().open(image);
    }
}