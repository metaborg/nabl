package mb.p_raffrayi.impl.diff;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set.Immutable;

public interface IDifferScopeOps<S, D> {

    Immutable<S> getScopes(D datum);

    IFuture<Boolean> matchDatums(D currentDatum, D previousDatum,
        Function2<S, S, IFuture<Boolean>> scopeMatch);

}
