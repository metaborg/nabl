package mb.statix.concurrent._attic;

import mb.statix.concurrent.actors.futures.IFuture;

public interface ITypeChecker<S, L, D, R> {

    IFuture<R> run(S root) throws InterruptedException;

}