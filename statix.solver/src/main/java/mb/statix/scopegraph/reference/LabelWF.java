package mb.statix.scopegraph.reference;

import java.util.Optional;

import com.google.common.collect.Iterables;

public interface LabelWF<L> {

    /**
     * This predicate should be prefix-monotone:
     * 
     * - If p = empty, then p.p' = empty must hold.
     */
    Optional<Boolean> wf(Iterable<L> labels);

    static <L> LabelWF<L> ANY() {
        return (p) -> Optional.of(true);
    }

    static <L> LabelWF<L> EPSILON() {
        return (ls) -> Iterables.isEmpty(ls) ? Optional.of(true) : Optional.empty();
    }

}