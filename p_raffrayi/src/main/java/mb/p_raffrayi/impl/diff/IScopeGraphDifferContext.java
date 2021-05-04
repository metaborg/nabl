package mb.p_raffrayi.impl.diff;

import java.util.Optional;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set;

public interface IScopeGraphDifferContext<S, L, D> {

    // Scope graph querying

    IFuture<Iterable<S>> getCurrentEdges(S scope, L label);

    IFuture<Iterable<S>> getPreviousEdges(S scope, L label);

    IFuture<Iterable<L>> labels(S currentScope);

    /**
     * Returns the external representation of the datum of the scope, if it exists
     */
    IFuture<Optional<D>> currentDatum(S scope);

    IFuture<Optional<D>> previousDatum(S scope);

    // Data matching

    IFuture<Boolean> matchDatums(D currentDatum, D previousDatum, Function2<S, S, IFuture<Boolean>> scopeMatch);

    boolean isMatchAllowed(S currentScope, S previousScope);

    Set.Immutable<S> getCurrentScopes(D d);

    Set.Immutable<S> getPreviousScopes(D d);

    // External scopes

    boolean ownScope(S scope);

    boolean ownOrSharedScope(S currentScope);

    IFuture<Optional<S>> externalMatch(S previousScope);
}
