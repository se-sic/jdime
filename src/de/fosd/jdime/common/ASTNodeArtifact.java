/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
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
 *     Kathrin Hanauer <hanauer@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.common;

import AST.*;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.common.operations.Operation;
import de.fosd.jdime.matcher.Color;
import de.fosd.jdime.matcher.Matching;
import de.fosd.jdime.stats.ASTStats;
import de.fosd.jdime.stats.StatsElement;
import de.fosd.jdime.strategy.ASTNodeStrategy;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * @author Olaf Lessenich
 *
 */
public class ASTNodeArtifact extends Artifact<ASTNodeArtifact> {

	private static final Logger LOG = Logger.getLogger(ClassUtils
			.getShortClassName(ASTNodeArtifact.class));

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
	 * @param artifact
	 *            artifact to create program from
	 * @return ASTNodeArtifact
	 */
	public static ASTNodeArtifact createProgram(final ASTNodeArtifact artifact) {
		assert (artifact.astnode != null);
		assert (artifact.astnode instanceof Program);

		Program old = (Program) artifact.astnode;
		Program program;
		try {
			program = old.clone();
		} catch (CloneNotSupportedException e) {
			program = new Program();
		}

		ASTNodeArtifact p = new ASTNodeArtifact(program);
		p.deleteChildren();

		return p;
	}

	/**
	 * Encapsulated ASTNode.
	 */
	private ASTNode<?> astnode = null;

	/**
	 * Constructor class.
	 */
	private ASTNodeArtifact() {
		this.astnode = new ASTNode<>();
		this.initializeChildren();
	}

	/**
	 * @param astnode
	 *            astnode
	 */
	private ASTNodeArtifact(final ASTNode<?> astnode) {
		assert (astnode != null);
		this.astnode = astnode;

		this.initializeChildren();
	}

	private void initializeChildren() {
		ArtifactList<ASTNodeArtifact> children = new ArtifactList<>();
		for (int i = 0; i < astnode.getNumChildNoTransform(); i++) {
			if (astnode != null) {
				ASTNodeArtifact child = new ASTNodeArtifact(astnode.getChild(i));
				child.setParent(this);
				child.setRevision(getRevision());
				children.add(child);
				child.initializeChildren();
			}
		}
		setChildren(children);
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

		ASTNode<?> astnode;
		if (artifact.isEmpty()) {
			astnode = new ASTNode<>();
		} else {
			Program p = initProgram();
			p.addSourceFile(artifact.getPath());
			astnode = p;
		}

		this.astnode = astnode;
		this.initializeChildren();
		renumberTree();
	}

	/**
	 * Returns the encapsulated JastAddJ ASTNode
	 *
	 * @return encapsulated ASTNode object from JastAddJ
	 */
	public final ASTNode<?> getASTNode() {
		return astnode;
	}

	@Override
	public Object clone() {
		assert (exists());

		ASTNodeArtifact clone = null;

		try {
			clone = new ASTNodeArtifact((ASTNode<?>) astnode.clone());
			clone.setRevision(getRevision());
			clone.setNumber(getNumber());
			clone.cloneMatches(this);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

		assert (clone.exists());

		return clone;
	}

	@Override
	public final ASTNodeArtifact addChild(final ASTNodeArtifact child) throws IOException {
		LOG.trace(getId() + ".addChild(" + child.getId() + ")");

		assert (this.exists());
		assert (child.exists());

		child.setParent(this);
		child.initializeChildren();
		children.add(child);

		return child;
	}

	@Override
	public final int compareTo(final ASTNodeArtifact o) {
		if (hasUniqueLabels()) {
			return astnode.dumpString().compareTo(o.astnode.dumpString());
		} else {
			throw new RuntimeException("This artifact is not comparable.");
		}
	}

	@Override
	public final ASTNodeArtifact createEmptyArtifact() throws FileNotFoundException {
		ASTNodeArtifact emptyArtifact= new ASTNodeArtifact();
		emptyArtifact.setRevision(getRevision());
		return emptyArtifact;
	}

	public void deleteChildren() {
		while (hasChildren()) {
			ASTNodeArtifact child = getChild(0);
			child.astnode = null;
			children.remove(0);
		}
	}

	private static String getGraphvizId(ASTNodeArtifact artifact) {
		return "\"" + artifact.getId() + "\"";
	}

	/**
	 * Returns the AST in dot-format.
	 *
	 * @param includeNumbers
	 *            include node number in label if true
	 * @return AST in dot-format.
	 */
	public final String dumpGraphvizTree(final boolean includeNumbers, int virtualcount) {
		assert (astnode != null);
		StringBuilder sb = new StringBuilder();

		if (isConflict()) {
			// insert virtual node
			String conflictId = "\"c" + virtualcount + "\"";
			sb.append(conflictId);
			sb.append("[label=\"Conflict\", fillcolor = red, style = filled]").append(System.lineSeparator());

			// left alternative
			sb.append(left.dumpGraphvizTree(includeNumbers, virtualcount));
			sb.append(conflictId).append("->").append(getGraphvizId(left)).
					append("[label=\"").append(left.getRevision()).append("\"]").append(";").append(System.lineSeparator());

			// right alternative
			sb.append(right.dumpGraphvizTree(includeNumbers, virtualcount));
			sb.append(conflictId).append("->").append(getGraphvizId(right)).
					append("[label=\"").append(right.getRevision()).append("\"]").append(";").append(System.lineSeparator());
		} else {
			sb.append(getGraphvizId(this)).append("[label=\"");

			// node label
			if (includeNumbers) {
				sb.append("(").append(getNumber()).append(") ");
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
				String childId = getGraphvizId(child);
				if (child.isConflict()) {
					virtualcount++;
					childId = "\"c" + virtualcount + "\"";
				}

				sb.append(child.dumpGraphvizTree(includeNumbers, virtualcount));

				// edge
				sb.append(getGraphvizId(this)).append("->").append(childId).append(";").append(System.lineSeparator());
			}
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
		StringBuilder sb = new StringBuilder();

		// node itself
		Matching<ASTNodeArtifact> m = null;

		// color
		if (!isConflict() && hasMatches()) {

			Set<Revision> matchingRevisions = matches.keySet();

			// print color code
			String color = "";

			for (Revision rev : matchingRevisions) {
				m = getMatching(rev);
				color = m.getHighlightColor().toShell();
			}

			sb.append(color);
		}

		if (isConflict()) {
			sb.append(Color.RED.toShell());
			sb.append(indent).append("(").append(getId()).append(") ");
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
			sb.append(indent).append("(").append(getId()).append(") ");
			sb.append(this);

			if (hasMatches()) {
				assert (m != null);
				sb.append(" <=> (").append(m.getMatchingArtifact(this).getId())
						.append(")");
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
	// @Override
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
		return getRevision() + "-" + getNumber();
	}

	@Override
	public final String getStatsKey(final MergeContext context) {
		return "nodes";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ASTNodeArtifact that = (ASTNodeArtifact) o;

		return getId().equals(that.getId());
	}

	@Override
	public final int hashCode() {
		return getId().hashCode();
	}

	@Override
	public final boolean hasUniqueLabels() {
		return ImportDecl.class.isAssignableFrom(astnode.getClass())
				|| Literal.class.isAssignableFrom(astnode.getClass());
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
	@Override
	public final boolean isOrdered() {
		return !ConstructorDecl.class.isAssignableFrom(astnode.getClass())
				&& !MethodDecl.class.isAssignableFrom(astnode.getClass())
				&& !InterfaceDecl.class.isAssignableFrom(astnode.getClass())
				&& !FieldDecl.class.isAssignableFrom(astnode.getClass())
				&& !FieldDeclaration.class.isAssignableFrom(astnode.getClass())
				&& !ImportDecl.class.isAssignableFrom(astnode.getClass());
	}

	/**
	 * Returns whether a node matches another node.
	 *
	 * @param other
	 *            node to compare with.
	 * @return true if the node matches another node.
	 */
	@Override
	public final boolean matches(final ASTNodeArtifact other) {
		assert (astnode != null);
		assert (other != null);
		assert (other.astnode != null);

		if (LOG.isDebugEnabled()) {
			LOG.debug("match(" + getId() + ", " + other.getId() + ")");
		}

		if ((ImportDecl.class.isAssignableFrom(astnode.getClass()) || Literal.class
				.isAssignableFrom(astnode.getClass()))
				&& other.astnode.getClass().equals(astnode.getClass())) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Try Matching (prettyPrint): {"
						+ astnode.prettyPrint() + "} and {"
						+ other.astnode.prettyPrint() + "}");
			}
			return astnode.prettyPrint().equals(other.astnode.prettyPrint());
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Try Matching (dumpString): {" + astnode.dumpString()
					+ "} and {" + other.astnode.dumpString() + "}");
		}
		return astnode.dumpString().equals(other.astnode.dumpString());
	}

	@Override
	public final void merge(MergeOperation<ASTNodeArtifact> operation, MergeContext context) throws IOException, InterruptedException {
		Objects.requireNonNull(operation, "operation must not be null!");
		Objects.requireNonNull(context, "context must not be null!");

		MergeStrategy<ASTNodeArtifact> astNodeStrategy = new ASTNodeStrategy();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Using strategy: " + astNodeStrategy);
		}

		MergeScenario<ASTNodeArtifact> triple = operation.getMergeScenario();
		ASTNodeArtifact left = triple.getLeft();
		ASTNodeArtifact right = triple.getRight();
		ASTNodeArtifact target = operation.getTarget();

		boolean safeMerge = true;

		int numChildNoTransform;
		try {
			numChildNoTransform = target.astnode.getClass().newInstance().getNumChildNoTransform();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException();
		}

		if (!isRoot() && numChildNoTransform > 0) {
		
			// this language element has a fixed number of children, we need to be careful with this one
			boolean leftChanges = left.isChange();
			boolean rightChanges = right.isChange();

			for (int i = 0; !leftChanges && i < left.getNumChildren(); i++) {
				leftChanges = left.getChild(i).isChange();
			}

			for (int i = 0; !rightChanges && i < right.getNumChildren(); i++) {
				rightChanges = right.getChild(i).isChange();
			}

			if (leftChanges && rightChanges) {
				
				if (LOG.isTraceEnabled()) {
					LOG.trace("Target " + target.getId() + " expects a fixed amount of children.");
					LOG.trace("Both " + left.getId() + " and " + right.getId() + " contain changes.");
					LOG.trace("We will report a conflict instead of performing the merge.");
				}
				safeMerge = false;
				
				// to be safe, we will report a conflict instead of merging
				ASTNodeArtifact targetParent = target.getParent();
				targetParent.removeChild(target);
				
				Operation<ASTNodeArtifact> conflictOp = new ConflictOperation<>(left, right, targetParent);
				conflictOp.apply(context);
			}
		}
		
		if (safeMerge) {
			astNodeStrategy.merge(operation, context);
		}

		if (!context.isQuiet() && context.hasOutput()) {
			System.out.print(context.getStdIn());
		}
	}

	/**
	 * Removes a child.
	 *
	 * @param child
	 *            child that should be removed
	 */
	private void removeChild(final ASTNodeArtifact child) {
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
	private void rebuildAST() {

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

	@Override
	public final ASTNodeArtifact createConflictArtifact(final ASTNodeArtifact left, final ASTNodeArtifact right) {
		ASTNodeArtifact conflict = left != null
				? new ASTNodeArtifact(left.astnode.fullCopy())
				: new ASTNodeArtifact(right.astnode.fullCopy());

		conflict.setConflict(left, right);

		return conflict;
	}

	/**
	 * Returns statistical data of the tree. stats[0]: number of nodes stats[1]:
	 * tree depth stats[2]: maximum number of children
	 *
	 * @return statistics
	 */
	public final int[] getStats() {
		// 0: number of nodes, 1: tree depth, 2: max children
		int[] mystats = new int[3];
		mystats[0] = 1;
		mystats[1] = 0;
		mystats[2] = getNumChildren();

		for (int i = 0; i < getNumChildren(); i++) {
			int[] childstats = getChild(i).getStats();
			mystats[0] += childstats[0];
			if (childstats[1] + 1 > mystats[1]) {
				mystats[1] = childstats[1] + 1;
			}
			if (childstats[2] > mystats[2]) {
				mystats[2] = childstats[2];
			}
		}

		return mystats;
	}

	public final ASTStats getStats(Revision revision, LangElem level,
			boolean isFragment) {
		StatsElement nodeStats = new StatsElement();
		StatsElement toplevelnodeStats = new StatsElement();
		StatsElement classlevelnodeStats = new StatsElement();
		StatsElement methodlevelnodeStats = new StatsElement();
		StatsElement classStats = new StatsElement();
		StatsElement methodStats = new StatsElement();

		// clearly, this is a node
		nodeStats.incrementElements();

		if (isConflict()) {
			nodeStats.incrementChanges();
			nodeStats.incrementConflicting();
		} else if ((revision == null && hasMatches()) || hasMatching(revision)) {
			nodeStats.incrementMatches();
		} else {
			nodeStats.incrementChanges();
			// added or deleted?
			if (hasMatches()) {
				// was deleted
				nodeStats.incrementDeleted();
			} else {
				// was added
				nodeStats.incrementAdded();
			}
		}

		StatsElement myStats = null;
		switch (level) {
		case TOPLEVELNODE:
			myStats = toplevelnodeStats;
			break;
		case CLASSLEVELNODE:
			myStats = classlevelnodeStats;
			break;
		case METHODLEVELNODE:
			myStats = methodlevelnodeStats;
			break;
		default:
			throw new NotYetImplementedException();
		}

		assert (myStats != null);

		nodeStats.copy(myStats);
		assert myStats.getElements() != 0;

		// find out level for child nodes and adjust class and method counter
		if (astnode instanceof ClassDecl) {
			level = LangElem.CLASSLEVELNODE;
			myStats.copy(classStats);
		} else if (astnode instanceof MethodDecl
				|| astnode instanceof ConstructorDecl) {
			level = LangElem.METHODLEVELNODE;
			myStats.copy(methodStats);
		}

		HashMap<String, StatsElement> diffstats = new HashMap<>();
		diffstats.put(LangElem.NODE.toString(), nodeStats);
		diffstats.put(LangElem.TOPLEVELNODE.toString(), toplevelnodeStats);
		diffstats.put(LangElem.CLASSLEVELNODE.toString(), classlevelnodeStats);
		diffstats
				.put(LangElem.METHODLEVELNODE.toString(), methodlevelnodeStats);
		diffstats.put(LangElem.CLASS.toString(), classStats);
		diffstats.put(LangElem.METHOD.toString(), methodStats);
		ASTStats stats = new ASTStats(1, 1, getNumChildren(), diffstats,
				myStats.getChanges() != 0);
		boolean hasSubtreeChanges = stats.hasChanges();

		if (!hasSubtreeChanges) {
			isFragment = false;
		} else if (!isFragment) {
			isFragment = true;
			stats.incrementFragments();
		}

		/*
		This is a rather mean hack.

		Basically the loop does sanity checks.
		While benchmarking, I switch asserts off and the code will not be executed to save time.
		*/
		boolean assertsEnabled = false;
		assert assertsEnabled = true;
		if (assertsEnabled) {
			for (String key : diffstats.keySet()) {
				StatsElement e = diffstats.get(key);
				assert (e.getElements() == e.getMatches() + e.getAdded()
						+ e.getDeleted() + e.getConflicting());
				assert (e.getChanges() == e.getAdded() + e.getDeleted()
						+ e.getConflicting());
			}

		}

		for (int i = 0; i < getNumChildren(); i++) {
			stats.add(getChild(i).getStats(revision, level, isFragment));

			if (!hasSubtreeChanges && stats.hasChanges()) {
				hasSubtreeChanges = true;
				if (astnode instanceof ClassDecl) {
					stats.getDiffStats(LangElem.CLASS.toString())
							.incrementChanges();
				} else if (astnode instanceof MethodDecl
						|| astnode instanceof ConstructorDecl) {
					stats.getDiffStats(LangElem.METHOD.toString())
							.incrementChanges();
				}
			}

			if (assertsEnabled) {
				for (String key : diffstats.keySet()) {
					StatsElement e = diffstats.get(key);
					assert (e.getElements() == e.getMatches() + e.getAdded()
							+ e.getDeleted() + e.getConflicting());
				}
			}
		}

		return stats;
	}

	public HashMap<String, Integer> getLanguageElementStatistics() {
		HashMap<String, Integer> elements = new HashMap<>();

		String key = this.toString().split(" ")[0];
		key = key.startsWith("AST.") ? key.replaceFirst("AST.", "") : key;
		elements.put(key, new Integer(1));

		for (int i = 0; i < getNumChildren(); i++) {
			HashMap<String, Integer> childElements = getChild(i).getLanguageElementStatistics();
			for (String childKey : childElements.keySet()) {
				Integer value = elements.get(childKey);
				value = value == null ? childElements.get(childKey) : value + childElements.get(childKey);
				elements.put(childKey, value);
			}
		}
		
		return elements;
	}
}
