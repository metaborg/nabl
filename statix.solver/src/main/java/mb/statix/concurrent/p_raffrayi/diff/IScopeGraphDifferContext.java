package mb.statix.concurrent.p_raffrayi.diff;

import java.util.Optional;

import org.metaborg.util.functions.Function2;

import io.usethesource.capsule.Set;
import mb.statix.concurrent.actors.futures.IFuture;

public interface IScopeGraphDifferContext<S, L, D> {
    
    // Scope graph querying

    IFuture<Iterable<S>> getCurrentEdges(S scope, L label);

    IFuture<Iterable<S>> getPreviousEdges(S scope, L label);
    
    String owner(S current);

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

}
