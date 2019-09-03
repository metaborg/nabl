package mb.statix.random.node;

import java.util.Random;

import mb.statix.random.SearchNode;
import mb.statix.solver.IConstraint;

public class SearchNodes {

    private final Random rnd;

    public SearchNodes(Random rnd) {
        this.rnd = rnd;
    }

    @SafeVarargs public final <I, O> Alt<I, O> alt(SearchNode<I, O>... ns) {
        return new Alt<>(rnd, ns);
    }

    public final Const _const() {
        return new Const(rnd);
    }

    @SafeVarargs public final DropPredicates drop(Class<? extends IConstraint>... classes) {
        return new DropPredicates(rnd, classes);
    }

    public final ExpandPredicate expandPredicate() {
        return new ExpandPredicate(rnd);
    }

    public final Infer infer() {
        return new Infer(rnd);
    }

    public final <I, O> Limit<I, O> limit(int limit, SearchNode<I, O> n) {
        return new Limit<>(rnd, limit, n);
    }

    public final ResolveQuery resolveQuery() {
        return new ResolveQuery(rnd);
    }

    public final SelectRandomPredicate selectPredicate() {
        return new SelectRandomPredicate(rnd);
    }

    public final SelectRandomQuery selectQuery() {
        return new SelectRandomQuery(rnd);
    }

    public final <I, X, O> Seq<I, X, O> seq(SearchNode<I, X> n1, SearchNode<X, O> n2) {
        return new Seq<>(rnd, n1, n2);
    }

}