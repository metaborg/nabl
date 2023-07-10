package mb.p_raffrayi.impl.diff;

import java.util.Collection;
import java.util.Optional;

import org.metaborg.util.future.IFuture;

import org.metaborg.util.collection.BiMap;

public interface IDifferOps<S, L, D> {

    // Matching

    boolean isMatchAllowed(S currentScope, S previousScope);

    Optional<BiMap.Immutable<S>> matchDatums(D currentDatum, D previousDatum);

    // Data

    /**
     * Returns the external representation of the datum of the scope, if it exists
     */
    public Collection<S> getScopes(D d);

    public D embed(S scope);

    // External scopes

    boolean ownScope(S scope);

    boolean ownOrSharedScope(S currentScope);

    IFuture<Optional<S>> externalMatch(S previousScope);

}
