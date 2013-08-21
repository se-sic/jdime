/**
 * 
 */
package de.fosd.jdime.matcher;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.fosd.jdime.common.Artifact;

/**
 * @author Olaf Lessenich
 * 
 * @param <T> type of artifact
 */
public class OrderedMatcher<T extends Artifact<T>> 
	implements MatchingInterface<T> {
	
	/**
	 * Matcher.
	 */
	private ASTMatcher<T> matcher;

	/**
	 * Creates a new instance of OrderedMatcher.
	 * @param matcher matcher
	 */
	public OrderedMatcher(final ASTMatcher<T> matcher) {
		this.matcher = matcher;
	}

	/**
	 * Number of calls.
	 */
	private int calls = 0;
	
	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(OrderedMatcher.class);

	/**
	 * Compares two nodes.
	 * 
	 * @param left
	 *            left node
	 * @param right
	 *            right node
	 * @return matching
	 */
	public final Matching<T> match(final T left, final T right) {
		calls++;
		return simpleTreeMatching(left, right);
	}

	/**
	 * Yang's Simple tree matching algorithm.
	 * 
	 * @param left
	 *            left tree
	 * @param right
	 *            right tree
	 * @return maximum matching between left and right tree
	 */
	private Matching<T> simpleTreeMatching(final T left, final T right) {
		String id = "stm";

		if (!left.matches(right)) {
			// roots contain distinct symbols
			return new Matching<T>(left, right, 0);
		}

		// number of first-level subtrees of t1
		int m = left.getNumChildren();

		// number of first-level subtrees of t2
		int n = right.getNumChildren();

		int[][] matrixM = new int[m + 1][n + 1];
		Entry<T>[][] matrixT = new Entry[m + 1][n + 1];

		// initialize first column matrix
		for (int i = 0; i <= m; i++) {
			matrixM[i][0] = 0;
		}

		// initialize first row of matrix
		for (int j = 0; j <= n; j++) {
			matrixM[0][j] = 0;
		}

		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {

				Matching<T> w = matcher.match(left.getChild(i - 1),
						right.getChild(j - 1));
				if (matrixM[i][j - 1] > matrixM[i - 1][j]) {
					if (matrixM[i][j - 1] > matrixM[i - 1][j - 1]
							+ w.getScore()) {
						matrixM[i][j] = matrixM[i][j - 1];
						matrixT[i][j] = new Entry<T>(Direction.LEFT, w);
					} else {
						matrixM[i][j] = matrixM[i - 1][j - 1] + w.getScore();
						matrixT[i][j] = new Entry<T>(Direction.DIAG, w);
					}
				} else {
					if (matrixM[i - 1][j] > matrixM[i - 1][j - 1]
							+ w.getScore()) {
						matrixM[i][j] = matrixM[i - 1][j];
						matrixT[i][j] = new Entry<T>(Direction.TOP, w);
					} else {
						matrixM[i][j] = matrixM[i - 1][j - 1] + w.getScore();
						matrixT[i][j] = new Entry<T>(Direction.DIAG, w);
					}
				}
			}
		}

		int i = m;
		int j = n;
		List<Matching<T>> children = new LinkedList<Matching<T>>();

		while (i >= 1 && j >= 1) {
			switch (matrixT[i][j].getDirection()) {
			case TOP:
				i--;
				break;
			case LEFT:
				j--;
				break;
			case DIAG:
				if (matrixM[i][j] > matrixM[i - 1][j - 1]) {
					// markMatching("stm", matrixM[i][j]-matrixM[i - 1][j - 1],
					// t1.getChild(i - 1), t2.getChild(j - 1));
					children.add(matrixT[i][j].getMatching());
					matrixT[i][j].getMatching().setAlgorithm(id);
				}
				i--;
				j--;
			default:
				
				break;
			}
		}

		Matching<T> matching = new Matching<T>(left, right, matrixM[m][n] + 1);
		matching.setChildren(children);
		return matching;
	}
	
	
	/**
	 * Returns the number of calls.
	 * @return number of calls
	 */
	public final int getCalls() {
		return calls;
	}

}
