package mb.statix.solver.concurrent;

import java.util.concurrent.CompletableFuture;

public interface ITypeChecker<S, L, D, R> {

    CompletableFuture<R> run(S root) throws InterruptedException;

}