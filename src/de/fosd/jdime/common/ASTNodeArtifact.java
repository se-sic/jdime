/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import AST.ASTNode;
import AST.BytecodeParser;
import AST.CompilationUnit;
import AST.ConstructorDecl;
import AST.FieldDecl;
import AST.FieldDeclaration;
import AST.ImportDecl;
import AST.InterfaceDecl;
import AST.JavaParser;
import AST.MethodDecl;
import AST.Program;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;
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
	 * Initializes parser.
	 * 
	 * @param p
	 *            program
	 */
	private static void initParser(final Program p) {
		p.initJavaParser(new JavaParser() {

			@Override
			public CompilationUnit parse(final java.io.InputStream is,
					final String fileName) throws java.io.IOException,
					beaver.Parser.Exception {
				return new parser.JavaParser().parse(is, fileName);
			}
		});
	}

	/**
	 * Initializes a program.
	 * 
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
	 * Encapsulated ASTNode.
	 */
	private ASTNode<?> astnode = null;

	/**
	 * Constructor class.
	 */
	public ASTNodeArtifact() {

	}

	/**
	 * @param astnode
	 *            astnode
	 */
	public ASTNodeArtifact(final ASTNode<?> astnode) {
		assert (astnode != null);
		this.astnode = astnode;
	}

	/**
	 * Constructs an ASTNodeArtifact from a FileArtifact.
	 * 
	 * @param artifact
	 *            file artifact
	 */
	public ASTNodeArtifact(final FileArtifact artifact) {
		assert (artifact != null);

		setRevision(artifact.getRevision());

		ASTNode<?> astnode = null;
		if (artifact.isEmptyDummy()) {
			astnode = new ASTNode<>();
			setEmptyDummy(true);
		} else {
			Program p = initProgram();
			p.addSourceFile(artifact.getPath());
			astnode = p;
		}

		assert (astnode != null);
		this.astnode = astnode;
		renumber(1, this);
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
		if (child.isConflict()) {
			child.setParent(this);
			children.add(child);
			return child;
		}

		ASTNodeArtifact myChild;
		try {
			myChild = new ASTNodeArtifact((ASTNode<?>) child.astnode.clone());
			myChild.deleteChildren();
			myChild.setRevision(child.getRevision());
			myChild.setParent(this);
			myChild.astnode.setParent(astnode);
			myChild.setRevision(child.getRevision());
			myChild.setNumber(child.getNumber());
			myChild.cloneMatches(child);

			if (children == null) {
				initializeChildren();
			}
			children.add(myChild);
			// astnode.flushCaches();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Added child " + myChild.getId() + " to parent node "
						+ getId());
			}
			return myChild;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NotYetImplementedException();
		}

	}

	@Override
	public final int compareTo(final ASTNodeArtifact o) {
		if (hasUniqueLabels()) {
			return astnode.dumpString().compareTo(o.astnode.dumpString());
		} else {
			throw new RuntimeException("This artifact is not comparable.");
		}
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
		ASTNodeArtifact copy = destination.addChild(this);
		if (!isConflict() && hasChildren()) {
			for (ASTNodeArtifact child : getChildren()) {
				child.copyArtifact(copy);
			}
		}

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
		dummy.astnode = new ASTNode<>();
		dummy.setEmptyDummy(true);
		dummy.setRevision(getRevision());
		return dummy;
	}

	/**
	 * 
	 */
	public final void deleteChildren() {
		while (hasChildren()) {
			ASTNodeArtifact child = getChild(0);
			child.astnode = null;
			child = null;
			children.remove(0);
		}
	}

	/**
	 * Returns the AST in dot-format.
	 * 
	 * @param includeNumbers
	 *            include node number in label if true
	 * @return AST in dot-format.
	 */
	public final String dumpGraphvizTree(final boolean includeNumbers) {
		assert (astnode != null);
		StringBuffer sb = new StringBuffer();
		sb.append(getNumber() + "[label=\"");

		// node label
		if (includeNumbers) {
			sb.append("(" + getNumber() + ") ");
		}

		sb.append(astnode.dumpString());

		sb.append("\"");

		if (hasMatches()) {
			sb.append(", fillcolor = green, style = filled");
		}

		sb.append("];");
		sb.append(System.lineSeparator());

		// children
		for (ASTNodeArtifact child : getChildren()) {
			sb.append(child.dumpGraphvizTree(includeNumbers));

			// edge
			sb.append(getNumber() + "->" + child.getNumber() + ";"
					+ System.lineSeparator());
		}

		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#dumpTree(java.lang.String)
	 */
	@Override
	protected final String dumpTree(final String indent) {
		assert (astnode != null);
		StringBuffer sb = new StringBuffer();

		// node itself

		Matching<ASTNodeArtifact> m = null;

		// color
		if (!isConflict() && hasMatches()) {

			Set<Revision> matchingRevisions = matches.keySet();

			// print color code
			String color = "";

			for (Revision rev : matchingRevisions) {
				m = getMatching(rev);
				color = m.getColor().toShell();
			}

			sb.append(color);
		}

		if (isConflict()) {
			sb.append(Color.RED.toShell());
			sb.append(indent + "(" + getId() + ") ");
			sb.append(this);
			sb.append(System.lineSeparator());
			sb.append(Color.RED.toShell());
			sb.append("<<<<<<< ");
			sb.append(System.lineSeparator());
			// children
			if (left != null) {
				sb.append(left.dumpTree(indent));
			}
			sb.append(Color.RED.toShell());
			sb.append("======= ");
			sb.append(System.lineSeparator());
			// children
			if (right != null) {
				sb.append(right.dumpTree(indent));
			}
			
			sb.append(Color.RED.toShell());
			sb.append(">>>>>>> ");
			sb.append(System.lineSeparator());
		} else {
			sb.append(indent + "(" + getId() + ") ");
			sb.append(this);

			if (hasMatches()) {
				assert (m != null);
				sb.append(" <=> (" + m.getMatchingArtifact(this).getId() + ")");
				sb.append(Color.DEFAULT.toShell());
			}
			sb.append(System.lineSeparator());

			// children
			for (ASTNodeArtifact child : getChildren()) {
				sb.append(child.dumpTree(indent + "  "));
			}
		}

		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
//	@Override
//	public final boolean equals(final Object obj) {
//		assert (astnode != null);
//		assert (obj != null);
//		assert (obj instanceof ASTNodeArtifact);
//		return astnode.dumpString().equals(
//				((ASTNodeArtifact) obj).astnode.dumpString());
//	}

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
	 * @see de.fosd.jdime.common.Artifact#getId()
	 */
	@Override
	public final String getId() {
		return getRevision() + ":" + getNumber();
	}

	@Override
	public final String getStatsKey(final MergeContext context) {
		return "nodes";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#hashCode()
	 */
	@Override
	public final int hashCode() {
		return astnode.dumpString().hashCode();
	}

	@Override
	public final boolean hasUniqueLabels() {
		if (astnode instanceof ImportDecl) {
			return true;
		}
		
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#initializeChildren()
	 */
	@Override
	public final void initializeChildren() {
		assert (astnode != null);
		ArtifactList<ASTNodeArtifact> children = new ArtifactList<ASTNodeArtifact>();
		for (int i = 0; i < astnode.getNumChildNoTransform(); i++) {
			ASTNodeArtifact child = new ASTNodeArtifact(astnode.getChild(i));
			child.setParent(this);
			child.setRevision(getRevision());
			children.add(child);
		}
		setChildren(children);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#isEmpty()
	 */
	@Override
	public final boolean isEmpty() {
		return !hasChildren();
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

	/**
	 * Returns whether declaration order is significant for this node.
	 * 
	 * @return whether declaration order is significant for this node
	 */
	public final boolean isOrdered() {
		if (astnode instanceof CompilationUnit
				|| astnode instanceof ConstructorDecl
				|| astnode instanceof MethodDecl
				|| astnode instanceof InterfaceDecl
				|| astnode instanceof FieldDecl
				|| astnode instanceof FieldDeclaration
				|| astnode instanceof ImportDecl) {
			return false;
		}

		return true;
	}

	/**
	 * Returns whether a node matches another node.
	 * 
	 * @param other
	 *            node to compare with.
	 * @return true if the node matches another node.
	 */
	public final boolean matches(final ASTNodeArtifact other) {
		assert (astnode != null);
		assert (other != null);
		assert (other.astnode != null);

		if (astnode instanceof ImportDecl && other.astnode instanceof ImportDecl) {
			return astnode.toString().equals(other.astnode.toString());
		}
		
		return astnode.dumpString().equals(other.astnode.dumpString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#merge(de.fosd.jdime.common.operations.
	 * MergeOperation, de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeStrategy<ASTNodeArtifact> strategy = new ASTNodeStrategy();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Using strategy: " + strategy.toString());
		}

		MergeTriple<ASTNodeArtifact> triple = operation.getMergeTriple();
		assert (triple != null);
		ASTNodeArtifact left, right, target;
		left = triple.getLeft();
		right = triple.getRight();
		target = operation.getTarget();

		boolean safeMerge = true;

		try {
			if (!isRoot()
					&& target.astnode.getClass().newInstance()
							.getNumChildNoTransform() > 0) {
				// this language element has a fixed number of children
				// we need to be careful with this one
				boolean leftChanges = left.hasChanges(false);
				boolean rightChanges = right.hasChanges(false);

				if (leftChanges || rightChanges) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("target " + target.getId()
								+ " expects a fixed amount of children.");
						LOG.trace("changes in " + left.getId() + ": "
								+ leftChanges);
						LOG.trace("changes in " + right.getId() + ": "
								+ rightChanges);
						LOG.trace("We will report a conflict instead of performing the merge");
					}
					safeMerge = false;
					// to be safe, we will report a conflict instead of merging
					ASTNodeArtifact targetParent = target.getParent();
					targetParent.removeChild(target);
					ConflictOperation conflictOp = new ConflictOperation<>(
							left, left, right, targetParent);
					conflictOp.apply(context);
				}
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}

		if (safeMerge) {
			strategy.merge(operation, context);
		}

		if (!context.isQuiet() && context.hasOutput()) {
			System.out.println(context.getStdIn());
		}
	}

	public final void removeChild(ASTNodeArtifact child) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("[" + getId() + "] removing child " + child.getId());
			LOG.trace("children before removal: " + getChildren());
		}
		
		Iterator<ASTNodeArtifact> it = children.iterator();
		ASTNodeArtifact elem;
		while (it.hasNext()) {
			elem = it.next();
			if (elem == child) {
				it.remove();
			}
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("children after removal: " + getChildren());
		}
	}

	/**
	 * Pretty-prints the AST to source code.
	 * 
	 * @return Pretty-printed AST (source code)
	 */
	public final String prettyPrint() {
		assert (astnode != null);
		rebuildAST();
		astnode.flushCaches();
		if (LOG.isDebugEnabled()) {
		    System.out.println(dumpTree());
		}
		return astnode.prettyPrint();
	}

	/**
	 * Rebuild the encapsulated ASTNode tree top down. This should be only
	 * called at the root node
	 */
	public final void rebuildAST() {

		if (isConflict()) {
			astnode.isConflict = true;
			astnode.jdimeId = getId();

			if (left != null) {
				left.rebuildAST();
				astnode.left = left.astnode;
			}

			if (right != null) {
				right.rebuildAST();
				astnode.right = right.astnode;
			}
			
			
		}
		
		ASTNode<?>[] newchildren = new ASTNode[getNumChildren()];
		
		for (int i = 0; i < getNumChildren(); i++) {
			ASTNodeArtifact child = getChild(i);
			newchildren[i] = child.astnode;
			newchildren[i].setParent(astnode);
			child.rebuildAST();
			
		}
		astnode.jdimeChanges = hasChanges();
		astnode.jdimeId = getId();
		astnode.setChildren(newchildren);


		if (LOG.isTraceEnabled()) {
			if (getNumChildren() != astnode.getNumChildNoTransform()) {
				LOG.trace("ASTNodeArtifact has " + getNumChildren()
						+ " children: {" + getChildren() + "}");
				StringBuilder sb = new StringBuilder();
				sb.append("astnode has " + astnode.getNumChildNoTransform()
						+ " children: {");
				
				ASTNode<?>[] astnodechildren 
					= new ASTNode[astnode.getNumChild()];
				for (int i = 0; i < astnode.getNumChild(); i++) {
					astnodechildren[i] = astnode.getChild(i);
				}
				sb.append(Arrays.toString(astnodechildren));
				sb.append("}");
				LOG.trace(sb);
			}
		}

		assert (isConflict() || getNumChildren() == astnode
				.getNumChildNoTransform());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.common.Artifact#toString()
	 */
	@Override
	public final String toString() {
		assert (astnode != null);
		return astnode.dumpString();
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

	/**
	 * @param artifact
	 *            artifact to create program from
	 * @return ASTNodeArtifact
	 */
	public static ASTNodeArtifact createProgram(final ASTNodeArtifact artifact) {
		assert (artifact.astnode != null);
		assert (artifact.astnode instanceof Program);

		Program program = (Program) artifact.astnode.copy();
		// Iterator<CompilationUnit> it = program.compilationUnitIterator();
		// while (it.hasNext()) {
		// CompilationUnit cu = it.next();
		// for (int i = 0; i < cu.getNumChild(); i++) {
		// cu.getImportDeclList().removeChildren();
		// cu.getTypeDeclList().removeChildren();
		// }
		// }
		ASTNodeArtifact p = new ASTNodeArtifact(program);
		p.deleteChildren();

		return p;
	}

	@Override
	public final ASTNodeArtifact createConflictDummy(
			final ASTNodeArtifact type,
			final ASTNodeArtifact left,
			final ASTNodeArtifact right)
			throws FileNotFoundException {
			ASTNodeArtifact conflict;
		
			conflict = new ASTNodeArtifact(type.astnode.fullCopy());
			conflict.setConflict(left, right);
	
		return conflict;
	}

}
