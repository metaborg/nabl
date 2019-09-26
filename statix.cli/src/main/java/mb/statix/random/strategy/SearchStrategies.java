package mb.statix.random.strategy;

import java.util.Map;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableMap;

import mb.statix.constraints.CConj;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchNodes;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.solver.IConstraint;

public final class SearchStrategies {

    public static final <I, O> SearchStrategy<I, O> limit(int n, SearchStrategy<I, O> s) {
        return new Limit<>(n, s);
    }

    public static final <I, O> SearchStrategy<I, O> _for(int n, SearchStrategy<I, O> s) {
        return new For<>(n, s);
    }

    public static final <I, O> SearchStrategy<I, O> repeat(SearchStrategy<I, O> s) {
        return new Repeat<>(s);
    }

    public static final <I1, I2, O> SearchStrategy<I1, O> seq(SearchStrategy<I1, I2> s1, SearchStrategy<I2, O> s2) {
        return new Seq<>(s1, s2);
    }

    public static final <I, O1, O2> SearchStrategy<I, Either2<O1, O2>> alt(SearchStrategy<I, O1> s1,
            SearchStrategy<I, O2> s2) {
        return new Alt<>(s1, s2);

    }

    public static final <I1, I2, O> SearchStrategy<Either2<I1, I2>, O> match(SearchStrategy<I1, O> s1,
            SearchStrategy<I2, O> s2) {
        // this doesn't interleave!
        return new Match<>(s1, s2);
    }

    public static final SearchStrategy<SearchState, SearchState> infer() {
        return new Infer();
    }

    public static final SearchStrategy<SearchState, SearchState> fix(SearchStrategy<SearchState, SearchState> search,
            SearchStrategy<SearchState, SearchState> infer, Predicate1<CUser> done) {
        return new Fix(search, infer, done);
    }

    public static final <C extends IConstraint> SearchStrategy<SearchState, FocusedSearchState<C>> select(Class<C> cls,
            Predicate1<C> include) {
        return new Select<>(cls, include);
    }

    public static final SearchStrategy<SearchState, SearchState> transform(Function1<IConstraint, IConstraint> f) {
        return new Transform(f);
    }

    public static final SearchStrategy<FocusedSearchState<CUser>, SearchState> expand() {
        return expand(ImmutableMap.of());
    }

    public static final SearchStrategy<FocusedSearchState<CUser>, SearchState> expand(Map<String, Integer> weights) {
        return new Expand(weights);
    }

    public static final SearchStrategy<FocusedSearchState<CResolveQuery>, SearchState> resolve() {
        return new Resolve();
    }

    public static final SearchStrategy<FocusedSearchState<CResolveQuery>, FocusedSearchState<CResolveQuery>>
            canResolve() {
        return new CanResolve();
    }

    public static final <I, O> SearchStrategy<I, O> debug(SearchStrategy<I, O> s, Action1<SearchNode<O>> debug) {
        return new SearchStrategy<I, O>() {

            @Override protected SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
                return s.apply(ctx, input, parent).map(n -> {
                    debug.apply(n);
                    return n;
                });
            }

            @Override public String toString() {
                return s.toString();
            }

        };
    }

    public static final <I> SearchStrategy<I, I> identity() {
        return new SearchStrategy<I, I>() {

            @Override protected SearchNodes<I> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
                return SearchNodes.of(new SearchNode<>(ctx.nextNodeId(), input, parent, "id"));
            }

            @Override public String toString() {
                return "id";
            }

        };
    }

    // util

    public static SearchStrategy<SearchState, SearchState> transformPred(String pattern,
            Function1<CUser, IConstraint> f) {
        final mb.statix.random.predicate.Match match = new mb.statix.random.predicate.Match(pattern);
        return transform(Constraints.bottomup(Constraints.<IConstraint>cases().user(c -> {
            if(match.test(c)) {
                return f.apply(c);
            } else {
                return c;
            }
        }).otherwise(c -> {
            return c;
        })));
    }

    public static SearchStrategy<SearchState, SearchState> addAuxPred(String pattern, Function1<CUser, IConstraint> f) {
        return transformPred(pattern, c -> {
            return new CConj(c, f.apply(c), c);
        });
    }

    public static SearchStrategy<SearchState, SearchState> dropPred(String pattern) {
        return transformPred(pattern, c -> {
            return new CTrue(c);
        });
    }

    public static SearchStrategy<SearchState, SearchState> dropAst() {
        return transform(Constraints.bottomup(Constraints.<IConstraint>cases().termId(c -> new CTrue(c))
                .termProperty(c -> new CTrue(c)).otherwise(c -> c)));
    }


}