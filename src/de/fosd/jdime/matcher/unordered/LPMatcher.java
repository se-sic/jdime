/*******************************************************************************
 * Copyright (C) 2013-2015 Olaf Lessenich.
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
 *******************************************************************************/
package de.fosd.jdime.matcher.unordered;

import java.util.LinkedList;
import java.util.List;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;

/**
 * This unordered matcher calls an LP-Solver to solve the assignment problem.
 * TODO: this needs more explanation, I'll fix that soon
 *
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class LPMatcher<T extends Artifact<T>> extends UnorderedMatcher<T> {

	private static String id = "unordered";

	/**
	 * Threshold for rounding errors.
	 */
	private static final double THRESHOLD = 1e-6;

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
		return new int[] { x / width, x % width };
	}

	/**
	 * @param matcher
	 *            matcher
	 */
	public LPMatcher(final Matcher<T> matcher) {
		super(matcher);
	}

	@Override
	public final Matching<T> match(final T left, final T right) {

		if (!left.matches(right)) {
			return new Matching<>(left, right, 0);
		}

		// number of first-level subtrees of t1
		int m = left.getNumChildren();

		// number of first-level subtrees of t2
		int n = right.getNumChildren();

		if (m == 0 || n == 0) {
			return new Matching<>(left, right, 1);
		}

		@SuppressWarnings("unchecked")
		Matching<T>[][] matching = new Matching[m][n];

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				matching[i][j] = new Matching<>();
			}
		}

		T childT1;
		T childT2;

		for (int i = 0; i < m; i++) {
			childT1 = left.getChild(i);
			for (int j = 0; j < n; j++) {
				childT2 = right.getChild(j);
				Matching<T> w = matcher.match(childT1, childT2);
				matching[i][j] = w;
			}
		}

		return solveLP(left, right, matching);
	}

	/**
	 * Invokes the LP-Solver and solves the assignment problem.
	 *
	 * @param left
	 *            left artifact
	 * @param right
	 *            right artifact
	 * @param matching
	 *            matrix of matchings
	 * @return matching of root nodes
	 */
	private Matching<T> solveLP(final T left, final T right,
			final Matching<T>[][] matching) {
		int m = matching.length;
		int n = matching[0].length;
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
					0.0 /* lower */, 1.0 /* upper */);
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
			GLPK.glp_set_mat_row(lp, i /* row */, width /* max array index */,
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
			// TODO: verify that m and n are correct
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
			// TODO: error handling
			return null;
		}

		// problem could be solved
		// prevent precision problems
		int objective = (int) Math.round(GLPK.glp_get_obj_val(lp));

		List<Matching<T>> children = new LinkedList<>();

		for (int c = 1; c <= cols; c++) {
			if (Math.abs(1.0 - GLPK.glp_get_col_prim(lp, c)) < THRESHOLD) {
				int[] indices = getMyIndices(c - 1, width);
				int i = indices[0];
				int j = indices[1];
				if (i < m && j < n) { // TODO: verify that this is correct
					Matching<T> curMatching = matching[i][j];
					if (curMatching.getScore() > 0) {
						children.add(curMatching);
						curMatching.setAlgorithm(id);
					}
				}
			}
		}
		GLPK.glp_delete_prob(lp);

		Matching<T> rootmatching = new Matching<>(left, right, objective + 1);
		rootmatching.setChildren(children);

		return rootmatching;
	}
}
