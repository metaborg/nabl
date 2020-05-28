package mb.statix.solver.concurrent;

import java.util.concurrent.CompletableFuture;

import io.usethesource.capsule.Set;

public interface IScopeGraphFacade<S, L, D> {

    void openRootEdges(Iterable<L> labels);

    CompletableFuture<S> freshScope(D datum, Iterable<L> labels);

    void addEdge(S source, L label, S target);

    void closeEdge(S source, L label);

    CompletableFuture<Set.Immutable<Object>> query(S scope);

}