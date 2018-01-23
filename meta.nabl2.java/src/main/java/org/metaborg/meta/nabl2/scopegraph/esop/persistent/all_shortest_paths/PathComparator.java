package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.util.Comparator;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;

public class PathComparator<L extends ILabel> implements Comparator<Distance<L>> {

    private final IRelation<L> labelOrder;

    public PathComparator(IRelation<L> labelOrder) {
        this.labelOrder = labelOrder;
    }

    @Override
    public int compare(Distance<L> o1, Distance<L> o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == Distance.ZERO) {
            return -1;
        }
        if (o1 == Distance.INFINITE) {
            return +1;
        }
        if (o2 == Distance.ZERO) {
            return +1;
        }
        if (o2 == Distance.INFINITE) {
            return -1;
        }

        final boolean isValidOne = Distance.isValid(o1);
        final boolean isValidTwo = Distance.isValid(o2);

        if (!isValidOne && !isValidTwo) {
            return 0;
        }
        if (!isValidOne) {
            return +1;
        }
        if (!isValidTwo) {
            return +1;
        }

        int commonLength = Math.min(o1.getLabels().size(), o2.getLabels().size());

        final boolean isAmbiguous = IntStream.range(0, commonLength).map(index -> {
            final L l1 = o1.getLabels().get(index);
            final L l2 = o2.getLabels().get(index);
            return !Objects.equals(l1, l2) && !labelOrder.smaller(l2).contains(l1)
                    && !labelOrder.larger(l2).contains(l1) ? 1 : 0;
        }).sum() == 0 ? false : true;

        if (isAmbiguous) {
            // incomparable or ambiguity respectively
            return 0;
        }

        final OptionalInt commonComparisonResult = IntStream.range(0, commonLength).map(index -> {
            L l1 = o1.getLabels().get(index);
            L l2 = o2.getLabels().get(index);

            if (Objects.equals(l1, l2)) {
                return 0;
            }

            if (labelOrder.smaller(l2).contains(l1)) {
                return -1;
            } else if (labelOrder.larger(l2).contains(l1)) {
                return +1;
            } else {
                throw new IllegalStateException("Incomparable or ambiguous labels must be handled beforehand.");
            }
        }).filter(comparisionResult -> comparisionResult != 0).findFirst();

        if (commonComparisonResult.isPresent()) {
            return commonComparisonResult.getAsInt();
        }

        if (o1.getLabels().size() == o2.getLabels().size()) {
            return 0;
        } else {
            if (o1.getLabels().size() < o2.getLabels().size()) {
                return -1;
            } else {
                return +1;
            }
        }
    }
}