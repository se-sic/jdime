/**
 * 
 */
package de.fosd.jdime.matcher;

import java.util.LinkedList;
import java.util.List;

import de.fosd.jdime.common.ASTNodeArtifact;

/**
 * @author Olaf Lessenich
 *
 */
public class OrderedASTMatcher {
	
	
	static int calls = 0;

	public static Matching match(ASTNodeArtifact left, ASTNodeArtifact right) {
		calls++;
		return simpleTreeMatching(left, right);
	}
	
	/**
	 * Yang's Simple tree matching algorithm
	 * 
	 * @param left
	 *            left tree
	 * @param right
	 *            right tree
	 * @return maximum matching between left and right tree
	 */
	private static Matching simpleTreeMatching(ASTNodeArtifact left, ASTNodeArtifact right) {

		String id = "stm";

		if (!left.matches(right)) {
			// roots contain distinct symbols
			return new Matching(left, right, 0);
		}

		// number of first-level subtrees of t1
		int m = left.getNumChildren();

		// number of first-level subtrees of t2
		int n = right.getNumChildren();

		int[][] matrixM = new int[m + 1][n + 1];
		Entry[][] matrixT = new Entry[m + 1][n + 1];

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

				Matching w = ASTMatcher.match(left.getChild(i - 1), right.getChild(j - 1));
				if (matrixM[i][j - 1] > matrixM[i - 1][j]) {
					if (matrixM[i][j - 1] > matrixM[i - 1][j - 1] + w.getScore()) {
						matrixM[i][j] = matrixM[i][j - 1];
						matrixT[i][j] = new Entry(Direction.LEFT, w);
					} else {
						matrixM[i][j] = matrixM[i - 1][j - 1] + w.getScore();
						matrixT[i][j] = new Entry(Direction.DIAG, w);
					}
				} else {
					if (matrixM[i - 1][j] > matrixM[i - 1][j - 1] + w.getScore()) {
						matrixM[i][j] = matrixM[i - 1][j];
						matrixT[i][j] = new Entry(Direction.TOP, w);
					} else {
						matrixM[i][j] = matrixM[i - 1][j - 1] + w.getScore();
						matrixT[i][j] = new Entry(Direction.DIAG, w);
					}
				}
			}
		}

		int i = m;
		int j = n;
		List<Matching> children = new LinkedList<Matching>();

		while (i >= 1 && j >= 1) {
			switch (matrixT[i][j].direction) {
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
					children.add(matrixT[i][j].matching);
					matrixT[i][j].matching.setAlgorithm(id);
				}
				i--;
				j--;
			}
		}

		Matching matching = new Matching(left, right, matrixM[m][n] + 1);
		matching.setChildren(children);
		return matching;
	}

}
