/**
 * 
 */
package de.fosd.jdime;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;

/**
 * @author lessenic
 *
 */
public class MergeTest {

	private MergeContext context;
	private static final String[] STRATEGIES = { "linebased", "structured",
			"combined" };

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// initialize logger
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		// initialize context
		context = new MergeContext();
		context.setQuiet(true);

		// initialize temporary output file
		File out = Files.createTempFile("jdime-tests", ".java").toFile();
		context.setOutputFile(new FileArtifact(out));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		context.getOutputFile().remove();
	}

	private final void runMerge(String filepath, String strategy,
			boolean threeway) throws IOException, InterruptedException {
		// initialize input files
		ArtifactList<FileArtifact> inputArtifacts = new ArtifactList<>();
		inputArtifacts.add(new FileArtifact(new File("testfiles/left/"
				+ filepath)));
		if (threeway) {
			inputArtifacts.add(new FileArtifact(new File("testfiles/base/"
					+ filepath)));
		}
		inputArtifacts.add(new FileArtifact(new File("testfiles/right/"
				+ filepath)));

		// setup context
		context.setMergeStrategy(MergeStrategy.parse(strategy));
		context.setInputFiles(inputArtifacts);
		Main.merge(context);
	}

	/**
	 * Test method for
	 * {@link de.fosd.jdime.Main#merge(de.fosd.jdime.common.MergeContext)}.
	 */
	@Test
	public final void testBag() {
		try {
			String in = "SimpleTests/Bag/Bag.java";

			for (String strategy : STRATEGIES) {
				runMerge(in, strategy, true);
				File expected = new File("testfiles" + File.separator
						+ strategy + File.separator + in);
				System.out.println("----------Expected:-----------");
				System.out.println(FileUtils.readFileToString(expected));
				System.out.println("----------Received:-----------");
				System.out.println(context.getOutputFile().getContent());
				System.out.println("------------------------------");
				assertTrue("Strategy " + strategy
						+ " resulted in unexpected output",
						FileUtils.contentEquals(context.getOutputFile()
								.getFile(), expected));
			}
		} catch (Exception e) {
			fail(e.toString());
		}
	}
}
