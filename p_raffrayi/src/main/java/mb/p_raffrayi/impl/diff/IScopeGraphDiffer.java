package mb.p_raffrayi.impl.diff;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.future.IFuture;

import com.google.common.collect.Multimap;

import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.patching.IPatchCollection;

public interface IScopeGraphDiffer<S, L, D> {

    /**
     * Entry point. Returns a future that is completed when there are no scopes/edges to match anymore.
     */
    IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes);

    /**
     * Entry point. Accepts a context-free scope graph, and returns a future that is completed when there are no
     * scopes/edges to match anymore.
     */
    IFuture<ScopeGraphDiff<S, L, D>> diff(IScopeGraph.Immutable<S, L, D> initiallyMatchedGraph, Collection<S> scopes,
            Collection<S> sharedScopes, IPatchCollection.Immutable<S> patches, Collection<S> openScopes,
            Multimap<S, EdgeOrData<L>> openEdges);

    /**
     * Provides external matches to the differ. This method is used to ensure that shared scopes remain consistent in
     * the parent unit.
     */
    public boolean matchScopes(BiMap.Immutable<S> scopes);

    /**
     * Signals the differ that no other subunits (for which root scopes must match) will be added.
     */
    void typeCheckerFinished();

    // Queries

    /**
     * Returns the current scope the previous scope is matched on, or empty if it is removed.
     */
    IFuture<Optional<S>> match(S previousScope);

    /**
     * Returns the diff for a particular edge. It may either indicate that a scope is removed, or which edges are
     * added/removed from it.
     */
    IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label);
}
