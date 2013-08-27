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
package de.fosd.jdime.matcher;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.ordered.OrderedMatcher;
import de.fosd.jdime.matcher.ordered.SimpleTreeMatcher;
import de.fosd.jdime.matcher.unordered.LPMatcher;
import de.fosd.jdime.matcher.unordered.UniqueLabelMatcher;
import de.fosd.jdime.matcher.unordered.UnorderedMatcher;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 */
public class Matcher<T extends Artifact<T>> implements MatchingInterface<T> {
	
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Matcher.class);

	/**
	 * 
	 */
	private int calls = 0;
	
	/**
	 * 
	 */
	private int orderedCalls = 0;
	
	/**
	 * 
	 */
	private int unorderedCalls = 0;
	
	/**
	 * 
	 */
	private UnorderedMatcher<T> unorderedMatcher;
	
	/**
	 * 
	 */
	private OrderedMatcher<T> orderedMatcher;
	
	/**
	 * 
	 * @param uniqueLabels whether the matcher can assume unique labels
	 */
	public Matcher(final boolean uniqueLabels) {
		unorderedMatcher = uniqueLabels ? new UniqueLabelMatcher<T>(this) 
								: new LPMatcher<T>(this);
		orderedMatcher = uniqueLabels ? new SimpleTreeMatcher<T>(this)
										: new SimpleTreeMatcher<T>(this);
	}
	
	/**
	 * Logger.
	 */
	//private static final Logger LOG = Logger.getLogger(ASTMatcher.class);
	
	/**
	 * @param left artifact
	 * @param right artifact
	 * @return Matching
	 */
	public final Matching<T> match(final T left, final T right) {
		boolean isOrdered = false;

		for (int i = 0; !isOrdered && i < left.getNumChildren(); i++) {
			if (left.getChild(i).isOrdered()) {
				isOrdered = true;
			}
		}

		for (int i = 0; !isOrdered && i < right.getNumChildren(); i++) {
			if (right.getChild(i).isOrdered()) {
				isOrdered = true;
			}
		}
		
		calls++;
		
		if (isOrdered) {
			orderedCalls++;
			if (LOG.isTraceEnabled()) {
				LOG.trace(orderedMatcher.getClass().getSimpleName() + ".match("
						+ left.getId() + ", " + right.getId() + ")");
			}
			
			return orderedMatcher.match(left, right);
		} else {
			unorderedCalls++;
			
			if (LOG.isTraceEnabled()) {
				LOG.trace(unorderedMatcher.getClass().getSimpleName() + ".match("
						+ left.getId() + ", " + right.getId() + ")");
			}
			
			return unorderedMatcher.match(left, right);
		}
	}

	/**
	 * Marks corresponding nodes using an already computed matching. The
	 * respective nodes are flagged with <code>matchingFlag</code> and
	 * references are set to each other.
	 * 
	 * @param matching
	 *            used to mark nodes
	 * @param color
	 *            color used to highlight the matching in debug output
	 */
	public final void storeMatching(final Matching<T> matching, 
			final Color color) {
		T left = matching.getLeft();
		T right = matching.getRight();

		if (matching.getScore() > 0) {
			assert (left.matches(right));
			matching.setColor(color);
			left.addMatching(matching);
			right.addMatching(matching);
			for (Matching<T> childMatching : matching.getChildren()) {
				storeMatching(childMatching, color);
			}
		}

		
	}
	
	/**
	 * Resets the call counter.
	 */
	public final void reset() {
		calls = 0;
		unorderedCalls = 0;
		orderedCalls = 0;
	}
	
	/**
	 * Returns the logged call counts.
	 * @return logged call counts
	 */
	public final String getLog() {
		StringBuffer sb = new StringBuffer();
		sb.append("matcher calls (all/ordered/unordered): ");
		sb.append(calls + "/");
		sb.append(orderedCalls + "/");
		sb.append(unorderedCalls);
		assert (calls == unorderedCalls 
				+ orderedCalls) 
				: "Wrong sum for matcher calls";
		return sb.toString();
	}
}
