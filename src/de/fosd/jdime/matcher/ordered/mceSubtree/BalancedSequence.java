package de.fosd.jdime.matcher.ordered.mceSubtree;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.NewMatching;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Trees can be described as balanced sequences. A balanced sequence is a sequence of even length over the alphabet
 * {0, 1}. The balanced sequence of a leaf node is the empty sequence. The balanced sequence of a non leaf node is the
 * concatenation of the balanced sequences of its children, every one preceded by a 0 and followed by a 1. The balanced
 * sequence of a tree is the balanced sequence of its root node.
 *
 * @param <T>
 *         the type of the <code>Artifact</code> whose balanced sequence is to be constructed
 *
 * @author Georg Seibt
 * @see <a href="http://www.cs.upc.edu/~antoni/subtree.pdf">This Paper</a>
 */
public class BalancedSequence<T extends Artifact<T>> {

    private T root;
	private List<T> seq;

    /**
     * Constructs a new <code>BalancedSequence</code> representing the given <code>tree</code> structure.
     *
     * @param tree
     * 		the tree of <code>Artifact</code>s
     */
    public BalancedSequence(T tree) {
        this.root = tree;
        this.seq = new ArrayList<>(Collections.<T>nCopies(tree.getSubtreeSize() * 2, null));
        initSeq(tree, 0, 0, Integer.MAX_VALUE);
    }

    /**
     * Constructs a new <code>BalancedSequence</code> representing the given <code>tree</code> structure.
     * All nodes with depth <code>maxDepth</code> will be considered leaf nodes.
     *
     * @param tree
     *         the tree of <code>Artifact</code>s
     * @param maxDepth
     *         the maximum depth of nodes to consider
     */
    public BalancedSequence(T tree, int maxDepth) {
        this.root = tree;
		this.seq = new ArrayList<>(Collections.<T>nCopies(getSize(tree, maxDepth) * 2, null));
		initSeq(tree, 0, 0, maxDepth);
	}

	/**
	 * Constructs a new <code>BalancedSequence</code> wrapping the given <code>boolean[]</code>.
	 *
	 * @param seq
	 * 		the sequence to wrap
	 */
	private BalancedSequence(List<T> seq) {
		this.seq = seq;
	}

    /**
     * Returns one less (the root node) than the number of nodes in the tree that have at most the given
     * <code>depth</code>.
     *
     * @param tree
     *         the tree whose nodes are to be counted
     * @param depth
     *         the maximum depth of nodes to count
     *
     * @return the number of nodes
     */
    private int getSize(T tree, int depth) {

        if (depth == 0) {
            return 0;
        }

        int num = tree.getNumChildren();

        for (T t : tree.getChildren()) {
            num += getSize(t, depth - 1);
        }

        return num;
    }

    /**
     * Initializes the <code>seq</code> array to the balanced sequence of the <code>tree</code>.
     *
     * @param tree
     *         the tree whose balanced sequence is to be inserted in the <code>seq</code> array
     * @param index
     *         the index for the 0 before the first child
     * @param currentDepth
     *         the current depth in the tree
     * @param maxDepth
     *         the maximum Depth of nodes to add
     *
     * @return the index after the last index written to; this return value is only relevant for the recursive calls
     * of this method as the following 1 will be placed there
     */
    private int initSeq(T tree, int index, int currentDepth, int maxDepth) {

        if (currentDepth < maxDepth) {
            for (T t : tree.getChildren()) {
                seq.set(index++, t);
                index = initSeq(t, index, currentDepth + 1, maxDepth);
                index++;
            }
        }

        return index;
	}

	/**
	 * Partitions the balanced sequence into its head and tail. The head and tail of a balanced sequence <code>s</code>
	 * are unique balanced sequences such that <code>s = 0 head(s) 1 tail(s)</code>.
	 *
	 * @return a <code>Pair</code> of (<code>head(s), tail(s)</code>)
	 */
	public Pair<BalancedSequence<T>, BalancedSequence<T>> partition() {

		if (seq.size() == 0 || seq.size() == 2) {
			return Pair.of(emptySeq(), emptySeq());
		}

		int numZeros = 0;
		int index = 0;

		do {
			if (seq.get(index++) == null) {
				numZeros--;
			} else {
				numZeros++;
			}
		} while (numZeros > 0);

		BalancedSequence<T> head;
		BalancedSequence<T> tail;

		int headLength = index - 2;
		int tailLength = seq.size() - index;

		if (headLength != 0) {
			head = new BalancedSequence<>(seq.subList(1, 1 + headLength));
		} else {
			head = emptySeq();
		}

		if (tailLength != 0) {
			tail = new BalancedSequence<>(seq.subList(index, index + tailLength));
		} else {
			tail = emptySeq();
		}

		return Pair.of(head, tail);
	}

    /**
     * Returns a new empty sequence.
     *
     * @return an empty sequence
     */
    private BalancedSequence<T> emptySeq() {
        return new BalancedSequence<>(new ArrayList<T>());
    }

    /**
	 * Returns the decomposition of this balanced sequence. The decomposition of the empty balanced sequence is a set
	 * containing only the empty balanced sequence. For all other sequences s the decomposition is the union of a
	 * set containing s and the decompositions of head(s), tail(s) and the concatenation of head(s) and tail(s).
	 *
	 * @return the decomposition of this balanced sequence
	 */
	public Set<BalancedSequence<T>> decompose() {

		if (isEmpty()) {
			return Collections.singleton(emptySeq());
		}

		Set<BalancedSequence<T>> decomposition = new HashSet<>(Collections.singleton(this));

		Pair<BalancedSequence<T>, BalancedSequence<T>> partition = partition();
		BalancedSequence<T> head = partition.getLeft();
		BalancedSequence<T> tail = partition.getRight();

		decomposition.addAll(head.decompose());
		decomposition.addAll(tail.decompose());
		decomposition.addAll(concatenate(head, tail).decompose());

		return decomposition;
	}

    /**
     * Concatenates the two given <code>BalancedSequence</code>s.
     *
     * @param left
     *         the left part of the resulting <code>BalancedSequence</code>
     * @param right
     *         the right part of the resulting <code>BalancedSequence</code>
     * @param <T>
     *         the type of the <code>Artifact</code>s
     *
     * @return the concatenation result
     */
    private static <T extends Artifact<T>> BalancedSequence<T> concatenate(BalancedSequence<T> left, BalancedSequence<T> right) {
		int length = left.seq.size() + right.seq.size();

        List<T> result = new ArrayList<>(length);
        result.addAll(left.seq);
        result.addAll(right.seq);

		return new BalancedSequence<>(result);
	}

    /**
     * Returns the length (being the number of edges of the tree it represents) of the longest common balanced sequence
     * between the balanced sequences <code>s</code> and <code>t</code>.
     *
     * @param s
     *         the first <code>BalancedSequence</code>
     * @param t
     *         the second <code>BalancedSequence</code>
     * @param <T>
     *         the type of the <code>Artifact</code>s
     *
     * @return the length of the longest common balanced sequence
     */
    public static <T extends Artifact<T>> Set<NewMatching<T>> lcs(BalancedSequence<T> s, BalancedSequence<T> t) {

        Map<Integer, Integer> codes = new HashMap<>();
        Integer[][] results;
        int code = 0;

        /*
         * The decompositions of s and t contain all balanced sequences that will be produced during the recursion
         * through lcsRec. We assign each balanced sequence an index into the results array.
         */
        Set<BalancedSequence<T>> dec = new HashSet<>(s.decompose());
        dec.addAll(t.decompose());

        for (BalancedSequence<T> seq : dec) {
            codes.put(seq.hashCode(), code++);
        }

        /*
         * We build a triangular array because the lcs problem is symmetric (lcs(s, t) = lcs(t, s)).
         * The functions lookup and store are then used to correctly address sub-problems (the recursive cases in
         * lcsRec) using the codes of their balanced sequences.
         */
        results = new Integer[codes.size()][];

        for (int i = 0; i < results.length; i++) {
            results[i] = new Integer[i + 1];
        }

        lcsRec(s, t, codes, results);

        List<BalancedSequence<T>> leftSequences = getSequences(preOrder(s.root));
        List<BalancedSequence<T>> rightSequences = getSequences(preOrder(t.root));
        Set<NewMatching<T>> matchings = getMatchings(results, leftSequences, rightSequences);

        /*
         * Now we filter out the BalancedSequences in rightSequences which were produced by a node that is
         * already in the left tree.
         */
        for (ListIterator<BalancedSequence<T>> it = rightSequences.listIterator(); it.hasNext(); ) {
            BalancedSequence<T> rightSeq = it.next();

            for (BalancedSequence<T> leftSeq : leftSequences) {

                if (rightSeq.root.matches(leftSeq.root)) {
                    it.remove();
                    break;
                }
            }
        }

        matchings.addAll(getMatchings(results, rightSequences, leftSequences));

        return matchings;
    }

    /**
     * Returns for every element of <code>leftSequences</code> a <code>NewMatching</code> with the element of
     * <code>rightSequences</code> for which the <code>results</code> array contains the highest score.
     *
     * @param results
     * 		the results array produced by {@link #lcsRec(BalancedSequence, BalancedSequence, Map, Integer[][])}
     * @param leftSequences
     * 		the <code>BalancedSequence</code>s of the nodes of the left tree
     * @param rightSequences
     * 		the <code>BalancedSequence</code>s of the nodes of the right tree
     * @param <T>
     * 		the type of the <code>Artifact</code>
     * @return a <code>Set</code> of <code>NewMatching</code>s of the described format
     */
    private static <T extends Artifact<T>> Set<NewMatching<T>> getMatchings(Integer[][] results,
            List<BalancedSequence<T>> leftSequences, List<BalancedSequence<T>> rightSequences) {

        Set<NewMatching<T>> matchings = new HashSet<>();
        NewMatching<T> matching = null;

        for (BalancedSequence<T> left : leftSequences) {
            int hcLeft = left.hashCode();

            for (BalancedSequence<T> right : rightSequences) {
                int hcRight = right.hashCode();
                Integer res = lookup(hcLeft, hcRight, results);
                Integer score = left.root.matches(right.root) ? res : res + 1;

                if (matching == null || matching.getScore() < score) {
                    matching = new NewMatching<>(left.root, right.root, score);
                }
            }

            matchings.add(matching);
            matching = null;
        }

        return matchings;
    }

    /**
     * Returns the tree with root <code>root</code> in pre-order.
     *
     * @param root
     * 		the root of the tree
     * @param <T>
     * 		the type of the <code>Artifact</code>
     * @return the tree in pre-order
     */
    private static <T extends Artifact<T>> List<T> preOrder(T root) {
        List<T> nodes = new ArrayList<>();
        Queue<T> waitQ = new LinkedList<>(Collections.singleton(root));
        T node;

        while (!waitQ.isEmpty()) {
            node = waitQ.poll();
            nodes.add(node);
            waitQ.addAll(node.getChildren());
        }

        return nodes;
    }

    /**
     * Transforms the <code>List of T</code> into a list containing the corresponding <code>BalancedSequence</code>s.
     *
     * @param nodes
     * 		the nodes to transform
     * @param <T>
     * 		the type of the <code>Artifact</code>
     * @return the <code>BalancedSequence</code>s of the nodes
     */
    private static <T extends Artifact<T>> List<BalancedSequence<T>> getSequences(List<T> nodes) {
        List<BalancedSequence<T>> sequences = new ArrayList<>(nodes.size());

        for (T node : nodes) {
            sequences.add(new BalancedSequence<>(node));
        }

        return sequences;
    }

    /**
     * Recursive helper function for {@link BalancedSequence#lcs(BalancedSequence, BalancedSequence)}. Computes
     * the longest common balanced sequence between the balanced sequences <code>s</code> and <code>t</code> using
     * the <code>results</code> array to store results of sub-problems. <code>codes</code> must
     * contain mappings for the hashes of every balanced sequence in the decompositions of <code>s</code> and
     * <code>t</code> to an index in the array <code>results</code>.
     *
     * @param s
     *         the first <code>BalancedSequence</code>
     * @param t
     *         the seconds <code>BalancedSequence</code>
     * @param codes
     *         the codes of the <code>BalancedSequence</code>s in the decompositions
     * @param results
     *         the array of solutions to sub-problems
     * @param <T>
     *         the type of the <code>Artifact</code>s
     *
     * @return the length of the longest common balanced sequence
     */
    private static <T extends Artifact<T>> Integer lcsRec(BalancedSequence<T> s, BalancedSequence<T> t,
            Map<Integer, Integer> codes, Integer[][] results) {

        if (s.isEmpty() || t.isEmpty()) {
            return 0;
        }

        Integer codeS = codes.get(s.hashCode());
        Integer codeT = codes.get(t.hashCode());
        Integer result = lookup(codeS, codeT, results);

        if (result != null) {
            return result;
        }

        Pair<BalancedSequence<T>, BalancedSequence<T>> sPart = s.partition();
        Pair<BalancedSequence<T>, BalancedSequence<T>> tPart = t.partition();
        BalancedSequence<T> sHead = sPart.getLeft();
        BalancedSequence<T> tHead = tPart.getLeft();
        BalancedSequence<T> sTail = sPart.getRight();
        BalancedSequence<T> tTail = tPart.getRight();

        Integer a = lcsRec(concatenate(sHead, sTail), t, codes, results);
        Integer b = lcsRec(s, concatenate(tHead, tTail), codes, results);

        if (s.seq.get(0).matches(t.seq.get(0))) {
            Integer c = lcsRec(sHead, tHead, codes, results) + lcsRec(sTail, tTail, codes, results) + 1;
            result = max(a, max(b, c));
        } else {
            result = max(a, b);
        }

        store(codeS, codeT, results, result);

        return result;
    }

    /**
     * Looks up the result of the lcs problem for the balanced sequences with the given codes in the
     * <code>results</code> array.
     *
     * @param codeA
     *         the code of the first balanced sequence
     * @param codeB
     *         the code of the second balanced sequence
     * @param results
     *         the results array
     *
     * @return the solution to the lcs problem or <code>null</code> if <code>results</code> contains none
     */
    private static Integer lookup(Integer codeA, Integer codeB, Integer[][] results) {
        return codeA.compareTo(codeB) > 0 ? results[codeA][codeB] : results[codeB][codeA];
    }

    /**
     * Stores the solution to an lcs problem between the balanced sequences with the given codes in the
     * <code>results</code> array.
     *
     * @param codeA
     *         the code of the first balanced sequence
     * @param codeB
     *         the code of the second balanced sequence
     * @param results
     *         the results array
     * @param result
     *         the result to be stored
     */
    private static void store(Integer codeA, Integer codeB, Integer[][] results, Integer result) {

        if (codeA.compareTo(codeB) > 0) {
            results[codeA][codeB] = result;
        } else {
            results[codeB][codeA] = result;
        }
    }

    /**
     * Returns the maximum of two <code>Integer</code>s.
     *
     * @param a
     *         the first <code>Integer</code>
     * @param b
     *         the second <code>Integer</code>
     *
     * @return the bigger of both <code>Integer</code>s
     */
    private static Integer max(Integer a, Integer b) {
        return a.compareTo(b) > 0 ? a : b;
    }

	/**
	 * Returns whether this <code>BalancedSequence</code> is empty.
	 *
	 * @return true iff the <code>BalancedSequence</code> is empty
	 */
	public boolean isEmpty() {
		return seq.isEmpty();
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BalancedSequence that = (BalancedSequence) o;

        if (!seq.equals(that.seq)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return seq.hashCode();
    }

    @Override
	public String toString() {
		StringBuilder builder = new StringBuilder(seq.size());

        for (T bit : seq) {
            builder.append((bit == null) ? '1' : '0');
        }

		return builder.toString();
	}
}
