/**
 * 
 */
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;

import AST.ASTNode;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * @author Olaf Lessenich
 *
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {
	
	ASTNode astnode;

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#addChild(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final ASTNodeArtifact addChild(ASTNodeArtifact child) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#copyArtifact(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final void copyArtifact(ASTNodeArtifact destination) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#createArtifact(boolean)
	 */
	@Override
	public final void createArtifact(boolean isLeaf) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#createEmptyDummy()
	 */
	@Override
	public final ASTNodeArtifact createEmptyDummy() throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#exists()
	 */
	@Override
	public final boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#getChild(de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final ASTNodeArtifact getChild(ASTNodeArtifact otherChild) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#getId()
	 */
	@Override
	public final String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#getName()
	 */
	@Override
	public final String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#hashCode()
	 */
	@Override
	public final int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#initializeChildren()
	 */
	@Override
	public final void initializeChildren() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#isLeaf()
	 */
	@Override
	public final boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#merge(de.fosd.jdime.common.operations.MergeOperation, de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(MergeOperation operation, MergeContext context)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.fosd.jdime.common.Artifact#write(java.lang.String)
	 */
	@Override
	public final void write(String str) throws IOException {
		// TODO Auto-generated method stub

	}

}
