package de.fosd.jdime.matcher.ordered;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.MatcherInterface;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.matcher.matching.Matchings;

public class EqualityMatcher<T extends Artifact<T>> extends OrderedMatcher<T> {

    /**
     * Constructs a new <code>OrderedMatcher</code> using the given <code>matcher</code> for recursive calls.
     *
     * @param matcher the parent <code>MatcherInterface</code>
     */
    public EqualityMatcher(MatcherInterface<T> matcher) {
        super(matcher);
    }

    @Override
    public Matchings<T> match(MergeContext context, T left, T right) {
        String id = getClass().getSimpleName();

        Matchings<T> matchings = new Matchings<>();
        int score = 0;
        boolean identicalSubtree = true;

        for (int i = 0; i < Math.min(left.getNumChildren(), right.getNumChildren()); i++) {
            T leftChild = left.getChild(i);
            T rightChild = right.getChild(i);

            Matchings<T> childMatchings = match(context, leftChild, rightChild);
            matchings.addAll(childMatchings);

            if (childMatchings.get(leftChild, rightChild).isPresent()) {
                Matching<T> childMatching = childMatchings.get(leftChild, rightChild).get();
                int childScore = childMatching.getScore();
                if (childScore == leftChild.getTreeSize()) {
                    score += childScore;
                } else {
                    identicalSubtree = false;
                }
            }

        }

        if (left.matches(right) && left.getNumChildren() == right.getNumChildren() && identicalSubtree) {
            LOG.finest(() -> {
                String format = "%s - Trees are equal: (%s, %s)";
                return String.format(format, id, left.getId(), right.getId());
            });

            score++;
        } else {
            LOG.finest(() -> {
                String format = "%s - Trees are NOT equal: (%s, %s)";
                return String.format(format, id, left.getId(), right.getId());
            });

        }

        matchings.addAll(Matchings.of(left, right, score));

        for (Matching<T> matching : matchings) {
            matching.setAlgorithm(id);
        }
        return matchings;
    }
}
