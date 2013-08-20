/**
 * 
 */
package de.fosd.jdime.matcher;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

import de.fosd.jdime.common.ASTNodeArtifact;

/**
 * @author Olaf Lessenich
 *
 */
public final class UnorderedASTMatcher {
	
	/**
	 * Private constructor.
	 */
	private UnorderedASTMatcher() {
		
	}
	
	/**
	 * Number of times this method was called.
	 */
	static int calls = 0;
	
	/**
	 * Logger.
	 */
	private static final Logger LOG 
			= Logger.getLogger(UnorderedASTMatcher.class);

	/**
	 * Returns the largest common subtree of two unordered trees.
	 * 
	 * @param left
	 *            left tree
	 * @param right
	 *            right tree
	 * @return largest common subtree of left and right tree
	 */
	public static Matching match(final ASTNodeArtifact left, 
			final ASTNodeArtifact right) {
		calls++;
		// return brokenUnorderedTreeMatching(t1, t2);
		return bipartiteMatching(left, right);
		//return hungarianMatching(t1, t2);
	}
	
	/**
	 * Computes the largest common subtree of two unordered trees by computing
	 * the maximum matching on weighted, bipartite graphs.
	 * 
	 * @param left
	 *            left tree
	 * @param right
	 *            right tree
	 * @return largest common subtree
	 */
	private static Matching bipartiteMatching(final ASTNodeArtifact left, 
			final ASTNodeArtifact right) {

		String id = "unordered";

		if (!left.matches(right)) {
			return new Matching(left, right, 0);
		}

		// number of first-level subtrees of t1
		int m = left.getNumChildren();

		// number of first-level subtrees of t2
		int n = right.getNumChildren();

		if (m == 0 || n == 0) {
			return new Matching(left, right, 1);
		}

		Matching[][] matching = new Matching[m][n];

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				matching[i][j] = new Matching();
			}
		}

		ASTNodeArtifact childT1;
		ASTNodeArtifact childT2;

		for (int i = 0; i < m; i++) {
			childT1 = left.getChild(i);
			for (int j = 0; j < n; j++) {
				childT2 = right.getChild(j);
				Matching w = ASTMatcher.match(childT1, childT2);
				matching[i][j] = w;
			}
		}

		int width = m > n ? m : n;
		int cols = width * width;

		/* Caution, indices are one-based! */

		// create problem
		glp_prob lp = GLPK.glp_create_prob();

		// add number of columns
		GLPK.glp_add_cols(lp, cols);

		// define kind and bounds of variables
		for (int i = 1; i <= cols; i++) {
			// set kind of column i: CV=continuous, IV=integer
			// FIXME: possible performance issue, CV should also be ok
			GLPK.glp_set_col_kind(lp, i, GLPKConstants.GLP_IV);

			// set bounds for column i: 0 <= x <= 1.0
			// LO = lower, UP = upper, DB = double; superfluous are params
			// ignored
			GLPK.glp_set_col_bnds(lp, i, GLPKConstants.GLP_LO, 
					0.0 /* lower */, 
					1.0 /* upper */);
		}

		/* constraints */
		int rows = 2 * width;

		GLPK.glp_add_rows(lp, rows);

		SWIGTYPE_p_int ind;
		SWIGTYPE_p_double val;

		// row constraints
		for (int i = 1; i <= width; i++) {
			// define indices & values
			ind = GLPK.new_intArray(width + 1);
			val = GLPK.new_doubleArray(width + 1);
			for (int j = 1; j <= width; j++) {
				// glpk index is zero-based
				GLPK.intArray_setitem(ind, j, 
						getGlpkIndex(i - 1, j - 1, width) + 1);
				GLPK.doubleArray_setitem(val, j, 1.0);
			}
			GLPK.glp_set_mat_row(lp, i /* row */, 
								width /* max array index */, 
								ind, val);
		}

		// column constraints
		for (int j = 1; j <= width; j++) {
			// define indices & values
			ind = GLPK.new_intArray(width + 1);
			val = GLPK.new_doubleArray(width + 1);
			for (int i = 1; i <= width; i++) {
				// glpk index is zero-based
				GLPK.intArray_setitem(ind, i, 
						getGlpkIndex(i - 1, j - 1, width) + 1);
				GLPK.doubleArray_setitem(val, i, 1.0);
			}
			GLPK.glp_set_mat_row(lp, width + j /* row */, width /*
																 * max array
																 * index
																 */, ind, val);
		}

		// all constraints are "= 1"
		for (int i = 1; i <= 2 * width; i++) {
			GLPK.glp_set_row_bnds(lp, i, GLPKConstants.GLP_FX, 1.0, 1.0);
		}

		/* objective function */

		// objective function... minimize or maximize
		GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MAX);

		// set coefficients
		for (int c = 1; c <= cols; c++) {
			int[] indices = getMyIndices(c - 1, width);
			int i = indices[0];
			int j = indices[1];
			// take care of dummy rows/cols
			// FIXME is m and n correct?
			int score = i < m && j < n ? matching[i][j].getScore() : 0;
			GLPK.glp_set_obj_coef(lp, c, score);
		}

		/* SOLVE */

		// set parameters
		glp_smcp parm = new glp_smcp();
		GLPK.glp_init_smcp(parm); // defaults
		parm.setMsg_lev(GLPKConstants.GLP_MSG_OFF);
		parm.setMeth(GLPKConstants.GLP_PRIMAL); // primal or dual?

		// solve
		int ret = GLPK.glp_simplex(lp, parm);

		if (ret > 0) {
			GLPK.glp_delete_prob(lp);
			// FIXME error handling?
			return null;
		}

		// problem could be solved
		// prevent precision problems
		int objective = (int) Math.round(GLPK.glp_get_obj_val(lp));

		List<Matching> children = new LinkedList<Matching>();

		for (int c = 1; c <= cols; c++) {
			if (Math.abs(1.0 - GLPK.glp_get_col_prim(lp, c)) < 1e-6) {
				int[] indices = getMyIndices(c - 1, width);
				int i = indices[0];
				int j = indices[1];
				if (i < m && j < n) { // FIXME see above
					Matching curMatching = matching[i][j];
					if (curMatching.getScore() > 0) {
						children.add(curMatching);
						curMatching.setAlgorithm(id);
					}
				}
			}
		}
		GLPK.glp_delete_prob(lp);

		Matching rootmatching = new Matching(left, right, objective + 1);
		rootmatching.setChildren(children);

		return rootmatching;
	}
	
	/**
	 * Computes indices in the constraint matrix.
	 * 
	 * @param i
	 *            row in node matrix
	 * @param j
	 *            column in node matrix
	 * @param width
	 *            columns per row in node matrix
	 * @return index in constraint matrix
	 */
	private static int getGlpkIndex(final int i, final int j, final int width) {
		return i * width + j;
	}

	/**
	 * Computes indices in the node matrix.
	 * 
	 * @param x
	 *            index in constraint matrix
	 * @param width
	 *            columns per row in node matrix
	 * @return index in node matrix
	 */
	private static int[] getMyIndices(final int x, final int width) {
		return new int[] { (int) x / width, x % width };
	}

}
