package mb.statix.random.scopegraph;

import mb.statix.scopegraph.reference.ResolutionException;

public interface LabelOrder<L> {

    boolean lt(L l1, L l2) throws ResolutionException, InterruptedException;

    static <L> LabelOrder<L> NONE() {
        return (l1, l2) -> false;
    }

}