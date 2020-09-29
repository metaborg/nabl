package mb.nabl2.scopegraph.esop;

import org.metaborg.util.functions.Predicate2;

import com.google.common.annotations.Beta;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.INameResolution;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IResolutionParameters;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.esop.bottomup.BUNameResolution;
import mb.nabl2.scopegraph.esop.lazy.EsopNameResolution;

@Beta
public interface IEsopNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends INameResolution<S, L, O> {

    boolean addCached(IResolutionCache<S, L, O> cache);

    IResolutionCache<S, L, O> toCache();

    interface IResolutionCache<S extends IScope, L extends ILabel, O extends IOccurrence> {

        static <S extends IScope, L extends ILabel, O extends IOccurrence> IResolutionCache<S, L, O> empty() {
            return new IResolutionCache<S, L, O>() {};
        }

    }

    static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopNameResolution<S, L, O>
            of(IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isClosed) {
        switch(params.getStrategy()) {
            case ENVIRONMENTS:
                return BUNameResolution.of(params, scopeGraph, isClosed);
            case SEARCH:
                return EsopNameResolution.of(params, scopeGraph, isClosed);
            default:
                throw new IllegalArgumentException("Unknown strategy " + params.getStrategy());
        }
    }

    static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopNameResolution<S, L, O> of(
            IResolutionParameters<L> params, IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isClosed,
            IResolutionCache<S, L, O> cache) {
        switch(params.getStrategy()) {
            case ENVIRONMENTS:
                return BUNameResolution.of(params, scopeGraph, isClosed, cache);
            case SEARCH:
                return EsopNameResolution.of(params, scopeGraph, isClosed, cache);
            default:
                throw new IllegalArgumentException("Unknown strategy " + params.getStrategy());
        }
    }

}