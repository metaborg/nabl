package mb.scopegraph.pepm16.esop15;

import org.metaborg.util.functions.Predicate2;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.INameResolution;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IResolutionParameters;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.bottomup.BUNameResolution;
import mb.scopegraph.pepm16.esop15.lazy.EsopNameResolution;

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