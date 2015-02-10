package de.fosd.jdime;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ EnvironmentTest.class, PrettyPrintTest.class, MergeTest.class })
public class AllTests {

}
