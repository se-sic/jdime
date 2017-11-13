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

import de.fosd.jdime.stats.KeyEnums;
import de.fosd.jdime.util.Tuple;

import static de.fosd.jdime.artifact.Artifacts.root;
import static de.fosd.jdime.config.merge.MergeScenario.LEFT;
import static de.fosd.jdime.config.merge.MergeScenario.RIGHT;
import static de.fosd.jdime.stats.KeyEnums.Type.CLASS;
import static de.fosd.jdime.stats.KeyEnums.Type.METHOD;
import static de.fosd.jdime.stats.KeyEnums.Type.NODE;
import static de.fosd.jdime.stats.KeyEnums.Type.TRY;

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
        TestArtifact t = new TestArtifact("0", NODE);
        TestArtifact t1 = new TestArtifact("1", NODE);
        TestArtifact t2 = new TestArtifact("2", NODE);
        TestArtifact t3 = new TestArtifact("3", NODE);
        TestArtifact t4 = new TestArtifact("4", NODE);
        TestArtifact t5 = new TestArtifact("5", NODE);
        TestArtifact t6 = new TestArtifact("6", NODE);
        TestArtifact t7 = new TestArtifact("7", NODE);
        TestArtifact t8 = new TestArtifact("8", NODE);
        TestArtifact t9 = new TestArtifact("9", NODE);
        TestArtifact t10 = new TestArtifact("10", NODE);
        TestArtifact t11 = new TestArtifact("11", NODE);
        TestArtifact t12 = new TestArtifact("12", NODE);
        TestArtifact t13 = new TestArtifact("13", NODE);
        TestArtifact t14 = new TestArtifact("14", NODE);
        TestArtifact t15 = new TestArtifact("15", NODE);
        TestArtifact t16 = new TestArtifact("16", NODE);
        TestArtifact t17 = new TestArtifact("17", NODE);
        TestArtifact t18 = new TestArtifact("18", NODE);

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

        root(t).renumber();

        return t;
    }

    /**
     * Returns two simple test trees.
     *
     * @return the trees
     */
    public static Tuple<TestArtifact, TestArtifact> simpleTree() {
        TestArtifact classLeft = new TestArtifact(LEFT, "Class1", KeyEnums.Type.CLASS);
        TestArtifact classRight = new TestArtifact(RIGHT, "Class1", KeyEnums.Type.CLASS);

        TestArtifact m1Left = new TestArtifact(LEFT, "Method1", METHOD);
        TestArtifact m2Left = new TestArtifact(LEFT, "Method2", METHOD);

        TestArtifact m1Right = new TestArtifact(RIGHT,"Method1", METHOD);
        TestArtifact m2Right = new TestArtifact(RIGHT,"Method2", METHOD);
        TestArtifact m3Right = new TestArtifact(RIGHT,"Method3", METHOD);

        TestArtifact s1Left = new TestArtifact(LEFT, "Statement1", KeyEnums.Type.NODE);
        TestArtifact s2Left = new TestArtifact(LEFT, "Statement2", KeyEnums.Type.NODE);
        TestArtifact s3Left = new TestArtifact(LEFT, "Statement3", KeyEnums.Type.NODE);
        TestArtifact s4Left = new TestArtifact(LEFT, "Statement4", KeyEnums.Type.NODE);
        TestArtifact s5Left = new TestArtifact(LEFT, "Statement5", KeyEnums.Type.NODE);

        TestArtifact s1Right = new TestArtifact(RIGHT, "Statement1", KeyEnums.Type.NODE);
        TestArtifact s2Right = new TestArtifact(RIGHT, "Statement2", KeyEnums.Type.NODE);
        TestArtifact s3Right = new TestArtifact(RIGHT, "Statement3", KeyEnums.Type.NODE);
        TestArtifact s4Right = new TestArtifact(RIGHT, "Statement4", KeyEnums.Type.NODE);
        TestArtifact s5Right = new TestArtifact(RIGHT, "Statement5", KeyEnums.Type.NODE);
        TestArtifact s6Right = new TestArtifact(RIGHT, "Statement6", KeyEnums.Type.NODE);

        classLeft.addChild(m1Left);
        classLeft.addChild(m2Left);

        m1Left.addChild(s1Left);
        m1Left.addChild(s2Left);
        m2Left.addChild(s3Left);
        m2Left.addChild(s4Left);
        m2Left.addChild(s5Left);

        classRight.addChild(m1Right);
        classRight.addChild(m2Right);
        classRight.addChild(m3Right);

        m1Right.addChild(s1Right);
        m1Right.addChild(s2Right);
        m2Right.addChild(s3Right);
        m2Right.addChild(s4Right);
        m2Right.addChild(s5Right);
        m3Right.addChild(s6Right);

        root(classLeft).renumber();
        root(classRight).renumber();

        return Tuple.of(classLeft, classRight);
    }

    /**
     * Returns two simple test trees. They contain Statements enclosed in a TRY node an a renamed method.
     *
     * @return the trees
     */
    public static Tuple<TestArtifact, TestArtifact> tryTree() {
        TestArtifact classLeft = new TestArtifact(LEFT, "Class1", KeyEnums.Type.CLASS);
        TestArtifact classRight = new TestArtifact(RIGHT, "Class1", KeyEnums.Type.CLASS);

        TestArtifact m1Left = new TestArtifact(LEFT, "Method1", METHOD);
        TestArtifact m2Left = new TestArtifact(LEFT, "Method2", METHOD);

        TestArtifact m1Right = new TestArtifact(RIGHT, "Method1", METHOD);
        TestArtifact m2Right = new TestArtifact(RIGHT, "Method2#", METHOD);

        TestArtifact sTryLeft = new TestArtifact(LEFT, "Try", TRY);
        TestArtifact s1Left = new TestArtifact(LEFT, "Statement1", KeyEnums.Type.NODE);
        TestArtifact s2Left = new TestArtifact(LEFT, "Statement2", KeyEnums.Type.NODE);

        TestArtifact s3Left = new TestArtifact(LEFT, "Statement3", KeyEnums.Type.NODE);
        TestArtifact s4Left = new TestArtifact(LEFT, "Statement4", KeyEnums.Type.NODE);

        TestArtifact s1Right = new TestArtifact(RIGHT, "Statement1", KeyEnums.Type.NODE);
        TestArtifact s2Right = new TestArtifact(RIGHT, "Statement2", KeyEnums.Type.NODE);

        TestArtifact s3Right = new TestArtifact(RIGHT, "Statement3", KeyEnums.Type.NODE);
        TestArtifact s4Right = new TestArtifact(RIGHT, "Statement4", KeyEnums.Type.NODE);

        classLeft.addChild(m1Left);
        classLeft.addChild(m2Left);

        m1Left.addChild(sTryLeft);
        sTryLeft.addChild(s1Left);
        sTryLeft.addChild(s2Left);

        m2Left.addChild(s3Left);
        m2Left.addChild(s4Left);

        classRight.addChild(m1Right);
        classRight.addChild(m2Right);

        m1Right.addChild(s1Right);
        m1Right.addChild(s2Right);

        m2Right.addChild(s3Right);
        m2Right.addChild(s4Right);

        root(classLeft).renumber();
        root(classRight).renumber();

        return Tuple.of(classLeft, classRight);
    }

    public static TestArtifact conflictTree() {
        TestArtifact clazz = new TestArtifact("Class", CLASS);
        TestArtifact method = new TestArtifact("Method", CLASS);

        TestArtifact lBody = new TestArtifact("Body", NODE);
        TestArtifact rBody = new TestArtifact("Body", NODE);

        TestArtifact sTryLeft = new TestArtifact("Try", TRY);
        TestArtifact s1Left = new TestArtifact("Statement1", KeyEnums.Type.NODE);
        TestArtifact s2Left = new TestArtifact("Statement2", KeyEnums.Type.NODE);

        TestArtifact s3Left = new TestArtifact("Statement3", KeyEnums.Type.NODE);
        TestArtifact s4Left = new TestArtifact("Statement4", KeyEnums.Type.NODE);

        TestArtifact s1Right = new TestArtifact("Statement1", KeyEnums.Type.NODE);
        TestArtifact s2Right = new TestArtifact("Statement2", KeyEnums.Type.NODE);

        TestArtifact s3Right = new TestArtifact("Statement3", KeyEnums.Type.NODE);
        TestArtifact s4Right = new TestArtifact("Statement4", KeyEnums.Type.NODE);

        clazz.addChild(method);
        method.addChild(clazz.createConflictArtifact(lBody, rBody));

        lBody.addChild(sTryLeft);
        sTryLeft.addChild(s1Left); sTryLeft.addChild(s2Left);
        lBody.addChild(s3Left); lBody.addChild(s4Left);

        rBody.addChild(s1Right);
        rBody.addChild(s2Right);
        rBody.addChild(s3Right);
        rBody.addChild(s4Right);

        root(clazz).renumber();

        return clazz;
    }
}