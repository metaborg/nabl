package mb.statix.concurrent.p_raffrayi.diff;

import java.util.List;
import java.util.Optional;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.diff.BiMap;
import mb.statix.scopegraph.diff.ScopeGraphDiff;

public interface IScopeGraphDiffer<S, L, D>  {

    IFuture<ScopeGraphDiff<S, L, D>> diff(List<S> currentRootScopes, List<S> previousRootScopes);

    IFuture<Optional<S>> match(S previousScope);

    public boolean matchScopes(BiMap.Immutable<S> scopes);

    // Signals the differ that no other subunits (for which root scopes must match) will be added.
    void typeCheckerFinished();
}
