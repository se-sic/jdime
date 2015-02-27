package de.fosd.jdime.matcher.ordered.mceSubtree;

import java.util.Collections;
import java.util.Set;

import de.fosd.jdime.common.Artifact;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Trees can be described as balanced sequences. A balanced sequence is a sequence of even length over the alphabet
 * {0, 1} here represented by a boolean array. The balanced sequence of a leaf node is the empty sequence.
 * The balanced sequence of a non leaf node is the concatenation of the balanced sequences of its children, every
 * one preceded by a 0 and followed by a 1. The balanced sequence of a tree is the balanced sequence of its root node.
 */
public class BalancedSequence {

	private boolean[] seq;

	/**
	 * Constructs a new <code>BalancedSequence</code> representing the given <code>tree</code> structure.
	 *
	 * @param tree the tree of <code>Artifact</code>s
	 * @param <T> the type of the <code>Artifact</code>
	 */
	public <T extends Artifact<T>> BalancedSequence(Artifact<T> tree) {
		this.seq = new boolean[tree.getSubtreeSize() * 2];
		initSeq(tree, 0);
	}

	/**
	 * Constructs a new <code>BalancedSequence</code> wrapping the given <code>boolean[]</code>.
	 *
	 * @param seq the sequence to wrap
	 */
	private BalancedSequence(boolean[] seq) {
		this.seq = seq;
	}

	/**
	 * Initializes the <code>seq</code> array to the balanced sequence of the <code>tree</code>.
	 *
	 * @param tree
	 * 		the tree whose balanced sequence is to be inserted in the <code>seq</code> array
	 * @param index
	 * 		the index for the 0 before the first child
	 * @param <T>
	 * 		the type of the artifact
	 *
	 * @return the index after the last index written to; this return value is only relevant for the recursive calls
	 * of this method as the following 1 will be placed there
	 */
	private <T extends Artifact<T>> int initSeq(Artifact<T> tree, int index) {

		for (T t : tree.getChildren()) {
			index++;
			index = initSeq(t, index);
			seq[index++] = true;
		}

		return index;
	}

	public Pair<BalancedSequence, BalancedSequence> partition() {

		assert false : "Not implemented.";
		return Pair.of(null, null);
	}

	public Set<BalancedSequence> decompose() {

		assert false : "Not implemented.";
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(seq.length);

		for (boolean bit : seq) {
			builder.append(bit ? '1' : '0');
		}

		return builder.toString();
	}
}
