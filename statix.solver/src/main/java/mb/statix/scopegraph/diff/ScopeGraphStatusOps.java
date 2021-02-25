package mb.statix.scopegraph.diff;

import java.util.Collection;

import mb.statix.scopegraph.reference.EdgeOrData;

public interface ScopeGraphStatusOps<S, L> {

    boolean closed(S scope, EdgeOrData<L> label);
    
    /**
     * Returns all (open and closed) labels for a scope.
     */
    Collection<L> allLabels(S scope);

}
