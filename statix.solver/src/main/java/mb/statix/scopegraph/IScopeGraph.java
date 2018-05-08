package mb.statix.scopegraph;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IRelation3;

public interface IScopeGraph<S, L, R, O> {

    Set.Immutable<L> getLabels();
    
    L getEndOfPath();
    
    Set.Immutable<R> getRelations();
    
    Set<S> getAllScopes();

    IRelation3<S, L, S> getEdges();

    IRelation3<S, R, O> getData();

    interface Immutable<S, L, R, O> extends IScopeGraph<S, L, R, O> {

        @Override Set.Immutable<S> getAllScopes();

        @Override IRelation3.Immutable<S, L, S> getEdges();

        @Override IRelation3.Immutable<S, R, O> getData();

    }

    interface Transient<S, L, R, O>
            extends IScopeGraph<S, L, R, O> {

        boolean addEdge(S sourceScope, L label, S targetScope);

        boolean addDatum(S sourceScope, R relation, O decl);

        boolean addAll(IScopeGraph<S, L, R, O> other);

        // -----------------------

        IScopeGraph.Immutable<S, L, R, O> freeze();

    }

}