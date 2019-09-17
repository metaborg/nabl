package mb.statix.random.strategy;

import java.util.Map;

import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableMap;

import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.IConstraint;

public final class SearchStrategies {

    public final <I, O> SearchStrategy<I, O> limit(int n, SearchStrategy<I, O> s) {
        return new Limit<>(n, s);
    }

    public final <I1, I2, O> SearchStrategy<I1, O> seq(SearchStrategy<I1, I2> s1, SearchStrategy<I2, O> s2) {
        return new Seq<>(s1, s2);
    }

    public final <I, O1, O2> SearchStrategy<I, Either2<O1, O2>> alt(SearchStrategy<I, O1> s1,
            SearchStrategy<I, O2> s2) {
        return new Alt<>(s1, s2);

    }

    public final <I1, I2, O> SearchStrategy<Either2<I1, I2>, O> match(SearchStrategy<I1, O> s1,
            SearchStrategy<I2, O> s2) {
        // this doesn't interleave!
        return new Match<>(s1, s2);
    }

    public final SearchStrategy<SearchState, SearchState> infer() {
        return new Infer();
    }

    public final <C extends IConstraint> SearchStrategy<SearchState, FocusedSearchState<C>> select(Class<C> cls,
            Predicate1<C> include) {
        return new Select<>(cls, include);
    }

    @SafeVarargs public final SearchStrategy<SearchState, SearchState> drop(Class<? extends IConstraint>... classes) {
        return new Drop(classes);
    }

    public final SearchStrategy<FocusedSearchState<CUser>, SearchState> expand() {
        return expand(ImmutableMap.of());
    }

    public final SearchStrategy<FocusedSearchState<CUser>, SearchState> expand(Map<String, Integer> weights) {
        return new Expand(weights);
    }

    public final SearchStrategy<FocusedSearchState<CResolveQuery>, SearchState> resolve() {
        return new Resolve();
    }

    public final SearchStrategy<FocusedSearchState<CResolveQuery>, FocusedSearchState<CResolveQuery>> canResolve() {
        return new CanResolve();
    }

}