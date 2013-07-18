/**
 * 
 */
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import AST.ASTNode;
import AST.BytecodeParser;
import AST.CompilationUnit;
import AST.JavaParser;
import AST.Program;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.strategy.ASTNodeStrategy;
import de.fosd.jdime.strategy.MergeStrategy;

/**
 * @author Olaf Lessenich
 * 
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(ASTNodeArtifact.class);
	
	/**
	 * Encapsulated ASTNode.
	 */
	private ASTNode<?> astnode = null;
	
	/**
	 * Initializes a program.
	 * @return program
	 */
	private static Program initProgram() {
		Program program = new Program();
		program.state().reset();
		program.initBytecodeReader(new BytecodeParser());
		initParser(program);
		return program;
	}

	/**
	 * Initializes parser.
	 * 
	 * @param p
	 *            program
	 */
	private static void initParser(final Program p) {
		p.initJavaParser(new JavaParser() {

			@Override
			public CompilationUnit parse(final java.io.InputStream is, 
						final String fileName)
					throws java.io.IOException, beaver.Parser.Exception {
				return new parser.JavaParser().parse(is, fileName);
			}
		});
	}
	
	/**
	 * Constructor class.
	 */
	public ASTNodeArtifact() {
		
	}
	
	/**
	 * Constructs an ASTNodeArtifact from a FileArtifact. 
	 * @param artifact file artifact
	 */
	public ASTNodeArtifact(final FileArtifact artifact) {
		assert (artifact != null);
		
		if (artifact.isEmptyDummy()) {
			astnode = new ASTNode();
			setEmptyDummy(true);
		} else {
			Program p = initProgram();
			p.addSourceFile(artifact.getPath());
			astnode = p;
		}
		
		assert (astnode != null);
		}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#addChild(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final ASTNodeArtifact addChild(final ASTNodeArtifact child)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#copyArtifact(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final void copyArtifact(final ASTNodeArtifact destination)
			throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#createArtifact(boolean)
	 */
	@Override
	public final void createArtifact(final boolean isLeaf) throws IOException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#createEmptyDummy()
	 */
	@Override
	public final ASTNodeArtifact createEmptyDummy()
			throws FileNotFoundException {
		ASTNodeArtifact dummy = new ASTNodeArtifact();
		dummy.astnode = new ASTNode();
		dummy.setEmptyDummy(true);
		return dummy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#exists()
	 */
	@Override
	public final boolean exists() {
		return astnode != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.common.Artifact#getChild(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final ASTNodeArtifact getChild(final ASTNodeArtifact otherChild) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#getId()
	 */
	@Override
	public final String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#getName()
	 */
	@Override
	public final String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#hashCode()
	 */
	@Override
	public final int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#initializeChildren()
	 */
	@Override
	public final void initializeChildren() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#isLeaf()
	 */
	@Override
	public final boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#merge(de.fosd.jdime.common.operations.
	 * MergeOperation, de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation, 
				final MergeContext context)
			throws IOException, InterruptedException {
		assert (operation != null);
		assert (context != null);
		
		MergeStrategy<ASTNodeArtifact> strategy = new ASTNodeStrategy();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Using strategy: " + strategy.toString());
		}
		
		strategy.merge(operation, context);
		
		if (!context.isQuiet()) {
			System.out.println(context.getStdIn());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#write(java.lang.String)
	 */
	@Override
	public final void write(final String str) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public final String getStatsKey(final MergeContext context) {
		return "nodes";
	}

}
