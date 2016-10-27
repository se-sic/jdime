package de.fosd.jdime.matcher.cost_model;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Optional;

import de.fosd.jdime.JDimeTest;
import de.fosd.jdime.artifact.ASTNodeArtifact;
import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.FileArtifact;
import de.fosd.jdime.artifact.TestArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;
import de.fosd.jdime.strdump.DumpMode;
import de.fosd.jdime.strdump.MatchingsTreeDump;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static de.fosd.jdime.artifact.Artifacts.root;
import static de.fosd.jdime.config.merge.MergeScenario.LEFT;
import static de.fosd.jdime.config.merge.MergeScenario.RIGHT;
import static de.fosd.jdime.stats.KeyEnums.Type.NODE;
import static org.junit.Assert.assertEquals;

public class CostModelMatcherTest extends JDimeTest {

    private CostModelMatcher<TestArtifact> matcher;

    private TestArtifact left;
    private TestArtifact right;

    private TestArtifact l0, l1, l2, l3, l4, l5, l6, l7;
    private TestArtifact r0, r1, r2, r3, r4, r5, r6, r7, r8, r9;

    @Before
    public void setUp() throws Exception {
        matcher = new CostModelMatcher<>();

        l0 = new TestArtifact(LEFT, "A", NODE);
        l1 = new TestArtifact(LEFT, "B", NODE);
        l2 = new TestArtifact(LEFT, "C", NODE);
        l3 = new TestArtifact(LEFT, "C", NODE);
        l4 = new TestArtifact(LEFT, "D", NODE);
        l5 = new TestArtifact(LEFT, "F", NODE);
        l6 = new TestArtifact(LEFT, "F", NODE);
        l7 = new TestArtifact(LEFT, "G", NODE);

        l0.addChild(l1);
        l0.addChild(l4);

        l1.addChild(l2);
        l1.addChild(l3);

        l4.addChild(l5);
        l4.addChild(l6);
        l4.addChild(l7);
        
        r0 = new TestArtifact(RIGHT, "A", NODE);
        r1 = new TestArtifact(RIGHT, "G", NODE);
        r2 = new TestArtifact(RIGHT, "D", NODE);
        r3 = new TestArtifact(RIGHT, "B", NODE);
        r4 = new TestArtifact(RIGHT, "C", NODE);
        r5 = new TestArtifact(RIGHT, "F", NODE);
        r6 = new TestArtifact(RIGHT, "F", NODE);
        r7 = new TestArtifact(RIGHT, "F", NODE);
        r8 = new TestArtifact(RIGHT, "C", NODE);
        r9 = new TestArtifact(RIGHT, "C", NODE);

        r0.addChild(r1);
        r0.addChild(r2);
        r0.addChild(r3);

        r3.addChild(r4);
        r3.addChild(r8);
        r3.addChild(r9);

        r4.addChild(r5);
        r4.addChild(r6);
        r4.addChild(r7);

        left = l0;
        right = r0;

        root(left).renumber();
        root(right).renumber();
    }

    // TODO remove
    boolean show = true;

    public static void main(String[] args) throws Exception { // TODO remove
        JDimeTest.initDirectories();

        String filePath = "SimpleTests/MovedMethod.java";
        ASTNodeArtifact l = new ASTNodeArtifact(new FileArtifact(LEFT, file(leftDir, filePath)));
        ASTNodeArtifact r = new ASTNodeArtifact(new FileArtifact(RIGHT, file(rightDir, filePath)));

        MergeContext context = new MergeContext();
        context.setWr(0.935f);
        context.setWn(0.413f);
        context.setWa(0.008f);
        context.setWs(0.777f);
        context.setWo(0.829f);

        // TODO extract parameters (or constants)
        context.setpAssign(0.7f);
        context.setSeed(Optional.of(42L));
        context.setCostModelIterations(100);
        context.setCmMatcherParallel(true);
        context.setCmMatcherFixRandomPercentage(false);

        CostModelMatcher<ASTNodeArtifact> matcher = new CostModelMatcher<>();

        System.in.read();

        long start = System.currentTimeMillis();
        Matchings<ASTNodeArtifact> matchings = matcher.match(context, l, r);
        System.out.println(System.currentTimeMillis() - start + "ms");

        Matcher<ASTNodeArtifact> m = new Matcher<>();
        m.storeMatchings(context, matchings, Color.BLUE);

        System.out.println(l.dump(DumpMode.PLAINTEXT_TREE));
        System.out.println(r.dump(DumpMode.PLAINTEXT_TREE));
    }

    @Test
    public void testShow() throws Exception {
        Matchings<TestArtifact> m = Matchings.of(left, right, 0);
        show(m);
    }

    @Test
    public void paperA() throws Exception {
        Matchings<TestArtifact> expected = new Matchings<>();

        expected.add(new Matching<>(l0, r0, 0));

        expected.add(new Matching<>(l1, r3, 0));
        expected.add(new Matching<>(l2, r8, 0));
        expected.add(new Matching<>(l3, r9, 0));

        expected.add(new Matching<>(l4, r4, 0));
        expected.add(new Matching<>(l5, r5, 0));
        expected.add(new Matching<>(l6, r6, 0));
        expected.add(new Matching<>(l7, r7, 0));

        testCostModelMatching(expected, 0.9f, 1.0f, 1.0f, 0.1f, 0);
    }

    @Test
    public void paperB() throws Exception {
        Matchings<TestArtifact> expected = new Matchings<>();

        expected.add(new Matching<>(l0, r0, 0));

        expected.add(new Matching<>(l1, r2, 0));
        expected.add(new Matching<>(l4, r3, 0));

        expected.add(new Matching<>(l2, r8, 0));
        expected.add(new Matching<>(l3, r9, 0));

        expected.add(new Matching<>(l5, r5, 0));
        expected.add(new Matching<>(l6, r6, 0));
        expected.add(new Matching<>(l7, r7, 0));

        testCostModelMatching(expected, 0.9f, 1.0f, 0.1f, 1.0f, 0);
    }

    @Test
    public void paperC() throws Exception {
        Matchings<TestArtifact> expected = new Matchings<>();

        expected.add(new Matching<>(l0, r0, 0));

        expected.add(new Matching<>(l1, r3, 0));
        expected.add(new Matching<>(l2, r8, 0));
        expected.add(new Matching<>(l3, r9, 0));

        expected.add(new Matching<>(l4, r2, 0));

        expected.add(new Matching<>(l5, r5, 0));
        expected.add(new Matching<>(l6, r6, 0));

        expected.add(new Matching<>(l7, r1, 0));

        testCostModelMatching(expected, 1.0f, 1.0f, 0.5f, 0.5f, 0);
    }

    private void testCostModelMatching(Matchings<TestArtifact> expected, float wr, float wn, float wa, float ws, float wo) throws Exception {
        MergeContext context = new MergeContext();

        context.setWr(wr);
        context.setWn(wn);
        context.setWa(wa);
        context.setWs(ws);
        context.setWo(wo);

        // TODO extract parameters (or constants)
        context.setFixLower(.25f);
        context.setFixUpper(.50f);
        context.setpAssign(0.7f);
        context.setSeed(Optional.of(42L));
        context.setCostModelIterations(100);
        context.setCmMatcherParallel(true);
        context.setCmMatcherFixRandomPercentage(true);

        Matchings<TestArtifact> actual = matcher.match(context, left, right);

        if (show) {
            System.out.println("Actual cost is " + matcher.cost(context, actual, left, right));
            System.out.println("Expected cost is " + matcher.cost(context, expected, left, right));
            show(actual);
        }

        assertEquals(expected, actual);
    }

    // TODO remove, or make it more robust
    private static  <T extends Artifact<T>> void show(Matchings<T> matchings) throws Exception {
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