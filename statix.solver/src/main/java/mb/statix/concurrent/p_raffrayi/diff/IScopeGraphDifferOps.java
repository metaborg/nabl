package mb.statix.concurrent.p_raffrayi.diff;

import org.metaborg.util.functions.Function2;

import io.usethesource.capsule.Set.Immutable;
import mb.statix.concurrent.actors.futures.IFuture;

public interface IScopeGraphDifferOps<S, D> {
    
    Immutable<S> getScopes(D datum);
    
    IFuture<Boolean> matchDatums(D currentDatum, D previousDatum,
        Function2<S, S, IFuture<Boolean>> scopeMatch);

}
