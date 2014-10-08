/**
 * 
 */
package de.fosd.jdime;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author lessenic
 *
 */
public class EnvironmentTest {

	@Test
	public final void test() {
		try {
			InternalTests.runEnvironmentTest();
		} catch (Exception e ) {
			fail("Internal tests failed. Check your setup!");
		}
	}

}
