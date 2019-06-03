package mb.statix.taico.observers;

import java.util.function.Consumer;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.CriticalEdge;

@FunctionalInterface
public interface EdgeCompleteObserver extends Consumer<CriticalEdge> {
    /**
     * Accept the edge with the given scope and label.
     * 
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     */
    default void accept(ITerm scope, ITerm label) {
        accept(CriticalEdge.of(scope, label));
    }
}
