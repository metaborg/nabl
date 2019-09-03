package mb.statix.random.node;

import java.util.Random;

import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;

public class SearchNodes {

    private final Random rnd;

    public SearchNodes(Random rnd) {
        this.rnd = rnd;
    }

    @SafeVarargs public final <I, O> SearchNode<I, O> alt(SearchNode<I, O>... ns) {
        return new Alt<>(rnd, ns);
    }

    public final SearchNode<SearchState, SearchState> _const() {
        return new Const(rnd);
    }

    @SafeVarargs public final SearchNode<SearchState, SearchState> drop(Class<? extends IConstraint>... classes) {
        return new DropPredicates(rnd, classes);
    }

    public final SearchNode<Tuple2<SearchState, CUser>, SearchState> expandPredicate() {
        return new ExpandPredicate(rnd);
    }

    public final SearchNode<SearchState, SearchState> infer() {
        return new Infer(rnd);
    }

    public final <I, O> SearchNode<I, O> limit(int limit, SearchNode<I, O> n) {
        return new Limit<>(rnd, limit, n);
    }

    public final SearchNode<Tuple2<SearchState, CResolveQuery>, SearchState> resolveQuery() {
        return new ResolveQuery(rnd);
    }

    public final SearchNode<SearchState, Tuple2<SearchState, CUser>> selectPredicate() {
        return new SelectRandomPredicate(rnd);
    }

    public final SearchNode<SearchState, Tuple2<SearchState, CResolveQuery>> selectQuery() {
        return new SelectRandomQuery(rnd);
    }

    public final <I, X, O> SearchNode<I, O> seq(SearchNode<I, X> n1, SearchNode<X, O> n2) {
        return new Seq<>(rnd, n1, n2);
    }

}